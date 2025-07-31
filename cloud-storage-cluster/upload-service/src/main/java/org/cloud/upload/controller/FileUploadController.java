package org.cloud.upload.controller;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.minio.*;
//import io.minio.messages.Part;
import jakarta.validation.Valid;
import org.cloud.upload.dto.ApiResponse;
import org.cloud.upload.dto.CompleteMultipartUploadRequest;
import org.cloud.upload.dto.InitMultipartUploadRequest;
import org.cloud.upload.dto.UploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/upload")
public class FileUploadController {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private static final String UPLOAD_ID_PREFIX = "uploadId:";

    private final MinioClient minioClient;
    private final MinioAsyncClient minioAsyncClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ExecutorService executorService;

    @Autowired
    public FileUploadController(MinioClient minioClient,
                                MinioAsyncClient minioAsyncClient,
                                ReactiveRedisTemplate<String, String> redisTemplate,
                                ExecutorService executorService) {
        this.minioClient = minioClient;
        this.minioAsyncClient = minioAsyncClient;
        this.redisTemplate = redisTemplate;
        this.executorService = executorService;
    }

    /**
     * 上传单个文件
     * @apiNote 适合中型文件上传的场景，建议大小在 5 MB ~ 50 MB 的文件使用此方式、
     * @param bucketName Minio 存储桶名称，必须是已存在的存储桶
     * @param objectName 上传的文件在 Minio 的绝对路径，包括文件名和文件后缀，如果不包含后缀，则下载时无法识别文件类型
     * @param filePartMono body 的文件对象数据
     */
    @PostMapping("/file")
    public Mono<ApiResponse<?>> uploadFile(@RequestParam("bucketName") String bucketName,
                                           @RequestParam("objectName") String objectName,
                                           @RequestPart("file") Mono<FilePart> filePartMono) {
        // 检查存储桶是否存在
        CompletableFuture<Boolean> bucketExistsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            } catch (Exception e) {
                return false;
            }
        }, executorService);

        // 返回响应
        return Mono.fromFuture(bucketExistsFuture)
            .flatMap(isBucketExists -> {

                // 检查存储桶是否存在
                if(!isBucketExists) {
                    return Mono.just(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "存储桶不存在，请检查存储桶名称是否正确。"));
                }

                // 检查objectName是否包含后缀
                if (!Pattern.matches(".*\\..+", objectName)) {
                    return Mono.just(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "请指定文件后缀，否则下载时无法识别文件类型。"))   ;
                }

                // 上传文件，并返回响应
                return filePartMono.flatMap(filePart -> // 对文件部分进行处理
                    DataBufferUtils.join(filePart.content()) // 将多个 Flux<DataBuffer> 合并为一个
                        .flatMap( // 将响应流 Mono 展平
                            buffer -> { // 对合并后的 DataBuffer 进行处理

                                // 将阻塞的上传任务提交到线程池中执行
                                return Mono.fromCallable(
                                        () -> {
                                            try{
                                                // 使用 MinioClient 客户端上传文件
                                                minioClient.putObject(
                                                        PutObjectArgs.builder() // 构建上传参数
                                                                .bucket(bucketName)  // 设置存储桶名称
                                                                .object(objectName)  // 设置对象名称
                                                                .stream(buffer.asInputStream(), buffer.readableByteCount(), -1) // 设置输入流, 文件大小和偏移量(-1表示从文件开头开始)
                                                                .contentType(Objects.requireNonNull(filePart.headers().getContentType()).toString()) // 设置内容类型
                                                                .build()
                                                );

                                                logger.info("上传文件：{}{}", bucketName, objectName);

                                                return ApiResponse.success("文件上传成功！");
                                            } catch(Exception e) {
                                                logger.error("文件上传失败：{}{}", bucketName, objectName, e);
                                                return ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件上传失败！");
                                            } finally {
                                                DataBufferUtils.release(buffer);  // 释放 DataBuffer
                                            }
                                        }
                                );
                            }
                        ).subscribeOn(Schedulers.fromExecutorService(executorService)) // 订阅任务，将上传任务提交到线程池执行
                // 如果没有上传文件，返回错误响应
                ).defaultIfEmpty(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "未上传文件，请选择要上传的文件。"));
            });
    }

    /**
     * 上传多个文件
     * @apiNote 适用场景为将小文件使用一次 HTTP 请求上传到 Minio，减少 HTTP 请求次数
     * @param bucketName Minio 存储桶名称，必须是已存在的存储桶
     * @param files body 的文件对象数据，文件对象需要从用户选择的文件重新构造文件对象，构造的文件对象的文件名设置为 Minio 中该文件的绝对路径
     * @return 如果所有文件上传成功，返回成功响应，否则返回包含失败文件列表的失败响应
     */
    @PostMapping("/files")
    public Mono<ApiResponse<?>> uploadFiles(@RequestParam ("bucketName") String bucketName,
                                            @RequestPart("files") Flux<Part> files) {
        // 检查存储桶是否存在
        CompletableFuture<Boolean> bucketExistsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            } catch (Exception e) {
                return false;
            }
        }, executorService);

        return Mono.fromFuture(bucketExistsFuture)
            .flatMap(isBucketExists -> {
                // 检查存储桶是否存在
                if (!isBucketExists) {
                    return Mono.just(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "存储桶不存在，请检查存储桶名称是否正确。"));
                }

                // 处理文件数据
                return files.flatMap( filePart -> { // 将 Flux 转换为 Mono
                        String objectName = filePart.headers().getContentDisposition().getFilename();  // 获取文件名称

                        if (objectName == null || objectName.isEmpty()) {
                            logger.error("多文件上传中出现空文件名，无法处理");
                            return Mono.just(new UploadResult("未知文件-" + System.currentTimeMillis(), false));
                        }

                        return DataBufferUtils.join(filePart.content())
                            .flatMap( buffer -> { // 将 Mono<DataBuffer> 展平为 DataBuffer
                                return Mono.fromCallable(
                                        () -> {
                                            try{
                                                // 使用 MinioClient 客户端上传文件
                                                minioClient.putObject(
                                                        PutObjectArgs.builder() // 构建上传参数
                                                                .bucket(bucketName)  // 设置存储桶名称
                                                                .object(objectName)  // 设置对象名称
                                                                .stream(buffer.asInputStream(), buffer.readableByteCount(), -1) // 设置输入流, 文件大小和偏移量(-1表示从文件开头开始)
                                                                .contentType(Objects.requireNonNull(filePart.headers().getContentType()).toString()) // 设置内容类型
                                                                .build()
                                                );

                                                logger.info("多文件上传：{}{}", bucketName, objectName);
                                                return new UploadResult(objectName, true);
                                            } catch(Exception e) {
                                                return new UploadResult(objectName, false);
                                            } finally {
                                                DataBufferUtils.release(buffer);
                                            }
                                        }
                                ).onErrorResume(
                                    e -> {
                                        logger.error("文件上传异常：{}", objectName, e);
                                        return Mono.just(new UploadResult(objectName, false));
                                    }
                                ); // 捕获每个文件上传过程中发生的异常，并返回一个默认值或替代操作，从而避免因单个文件上传失败而导致整个流程中断
                            }).subscribeOn(Schedulers.fromExecutorService(executorService)); // 订阅任务，将上传文件的任务提交到线程池执行
                    }
                ).collectList()
                .map(uploadResults -> { // 将上传结果收集到一个列表中
                    List<String> failedFiles = uploadResults.stream()
                            .filter(r -> !r.isSuccess())
                            .map(UploadResult::getFileName)
                            .toList();

                    if(failedFiles.isEmpty()) {
                        return ApiResponse.success("所有文件上传成功！");
                    } else {
                        logger.error("多文件上传出现失败，失败列表：\n{}", failedFiles);
                        return ApiResponse.failure(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "存在" + failedFiles.size() + " 个文件上传失败，请尝试重新上传未上传成功的文件！",
                                failedFiles
                        );
                    }
                });
        });
    }

    /**
     * 初始化分片上传
     * @apiNote 每次初始化成功会在redis创建一个List，用于存储分片的唯一标识ETag
     * @apiNote List的键形式为 uploadId:{uploadId}
     * @apiNote 每一个分块任务如果在 24 小时内没有完成分片合并，则该任务会被 Minio 自动取消，并删除这个任务的分片
     * @param request 请求体，包含初始分片上传的信息：
     * bucketName 存储桶名称
     * region 区域名称(可选), 在分布式存储时，可以指定对象的区域
     * objectName 上传的文件在 Minio 的绝对路径
     * contentType 文件的MIME类型
     * @return 如果初始化成功，返回上传任务ID
     */
    @PostMapping("/multipart/init")
    public Mono<ApiResponse<?>> initMultipartUpload(@Valid @RequestBody InitMultipartUploadRequest request) {
        String bucketName = request.getBucketName();
        String objectName = request.getObjectName();
        String contentType = request.getContentType();

        // 检查存储桶是否存在
        CompletableFuture<Boolean> bucketExistsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            } catch (Exception e) {
                return false;
            }
        }, executorService);

        // 初始化分片上传请求
        return Mono.fromFuture(bucketExistsFuture)
                .flatMap(isBucketExists -> {
                    // 检查存储桶是否存在
                    if (!isBucketExists) {
                        return Mono.just(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "存储桶不存在，请检查存储桶名称是否正确。"));
                    }

                    // 检查objectName是否包含后缀
                    if (!Pattern.matches(".*\\..+", objectName)) {
                        return Mono.just(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "请指定文件后缀，否则下载时无法识别文件类型。"))   ;
                    }

                    // 构造请求头
                    Multimap<String, String> headers = HashMultimap.create();
                    headers.put("Content-Type", contentType);

                    // 初始化分片上传请求
                    try {
                        return Mono.fromFuture(minioAsyncClient.createMultipartUploadAsync(bucketName, null, objectName, headers, null))
                                .map(response -> {
                                    String uploadId = response.result().uploadId();   // 获取初始化好的分块上传任务 ID
                                    logger.info("初始化分片上传任务：{}", uploadId);
                                    return ApiResponse.success(uploadId);
                                })
                                .onErrorResume(e -> {
                                    logger.error("初始化分片上传任务：{} 失败", objectName, e);
                                    return Mono.just(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "初始化分片上传失败！"));
                                }).subscribeOn(Schedulers.fromExecutorService(executorService));
                    } catch (Exception e) {
                        logger.error("初始化分片上传任务：{} 失败", objectName);
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }

    /**
     * 分片上传
     * @apiNote 每次上传成功会将分片的ETag存储到Redis的List中，键名为uploadId:{uploadId}
     * @param bucketName   存储桶名称, 推荐指定和初始化上传时相同的存储桶
     * @param region       区域名称(可选), 在分布式存储时，可以指定对象的区域
     * @param objectName   上传的文件在 Minio 的绝对路径
     * @param partNumber   分片编号，编号从1开始
     * @param uploadId     上传任务ID，由初始化分片上传请求返回
     * @param filePartFlux 待上传的文件分片的异步序列流
     * @return 如果分片上传成功，返回分片的ETag
     */
    @PostMapping(value = "/multipart/part", consumes = "application/octet-stream")
    public Mono<ApiResponse<?>> uploadPart(
            @RequestParam("bucketName") String bucketName,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam("objectName") String objectName,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("uploadId") String uploadId,
            @RequestBody Flux<DataBuffer> filePartFlux) {
        return DataBufferUtils.join(filePartFlux) // 使用 DataBufferUtils.join 合并 Flux<DataBuffer>
                .flatMap(dataBuffer -> {
                    try {
                        // 使用 MinioAsyncClient 客户端上传文件
                        CompletableFuture<UploadPartResponse> future = minioAsyncClient.uploadPartAsync(
                                bucketName,
                                region,
                                objectName,
                                dataBuffer.asInputStream(),
                                dataBuffer.readableByteCount(),
                                uploadId,
                                partNumber,
                                null,
                                null
                        );

                        // 获取异步任务的结果
                        return Mono.fromCompletionStage(future)
                                .flatMap(response -> {
                                    String etag = response.etag();
                                    String v = partNumber + "-" + etag;

                                    // 释放 DataBuffer，确保在异步任务完成后再释放
                                    DataBufferUtils.release(dataBuffer);

                                    logger.info("分片上传任务：{}{}, 上传分块：{}", uploadId, objectName, v);

                                    return Mono.defer(() -> redisTemplate.opsForList().rightPush(UPLOAD_ID_PREFIX + uploadId, v)
                                            .doOnNext(result -> logger.info("上传分片 {} 成功", v))
                                            .thenReturn(ApiResponse.success("分片" + partNumber + "上传成功！")));
                                })
                                .onErrorResume(e -> Mono.just(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "分片上传失败！" + e.getMessage())));
                    } catch (Exception e) {
                        logger.error("分片上传任务：{}{}, 分块 {} 上传失败", uploadId, objectName, partNumber);
                        return Mono.just(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "分片上传失败！"));
                    }
                });
    }

    /**
     * 合并分片，完成分块上传
     * @param request 完成分块上传请求对象
     */
    @PostMapping("/multipart/complete")
    public Mono<ApiResponse<?>> completeMultipartUpload(@Validated @RequestBody CompleteMultipartUploadRequest request) {
        String bucketName = request.getBucketName();
        String region = request.getRegion();
        String objectName = request.getObjectName();
        String uploadId = request.getUploadId();
        String contentType = request.getContentType();

        return redisTemplate.opsForList().range(UPLOAD_ID_PREFIX +  uploadId, 0, -1)  // 获取Redis中List的Flux异步序列
            .collectList() //将 Flux 转换为 List
            .flatMap(  // 将 List 转换为 Mono
                etags -> {
                    if(etags.isEmpty()) {
                        return Mono.just(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "未找到分片，请检查上传任务ID是否正确。"));
                    }

                    // 构造分片信息的 Part[] 数组
                    io.minio.messages.Part[] partEtags = etags.stream().map(
                        etag -> {
                            String[] parts = etag.split("-");
                            int partNumber = Integer.parseInt(parts[0]);
                            String eTag = parts[1];
                            return new io.minio.messages.Part(partNumber, eTag);
                        }
                    ).sorted(Comparator.comparingInt(io.minio.messages.Part::partNumber))  // 按照分片编号排序，minio要求按照分片编号顺序合并分片
                    .toArray(io.minio.messages.Part[]::new);

                    // 合并分片
                    try {
                        // 创建一个Multimap来存储请求头信息
                        Multimap<String, String> headers = HashMultimap.create();
                        headers.put("Content-Type", contentType);  // 设置文件类型

                        return Mono.fromFuture(minioAsyncClient.completeMultipartUploadAsync(
                                bucketName,
                                region,
                                objectName,
                                uploadId,
                                partEtags,
                                headers,
                                null
                        )).flatMap(response ->
                                redisTemplate.delete("uploadId:" + uploadId)
                                        .doOnNext(result -> logger.info("完成分块上传任务：{},\n 分块合并成功：{}{}", uploadId, bucketName, objectName)) // 确保执行完成 Redis 删除再返回
                                        .thenReturn(ApiResponse.success(objectName + " 分块上传成功！"))
                        );
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException(e));
                    }
                }
            );  // 指定线程池
    }
}
