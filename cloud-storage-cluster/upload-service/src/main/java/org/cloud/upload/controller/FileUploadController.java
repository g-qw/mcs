package org.cloud.upload.controller;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.minio.*;
//import io.minio.messages.Part;
import jakarta.validation.Valid;
import org.cloud.upload.dto.ApiResponse;
import org.cloud.upload.dto.FilesUploadResult;
import org.cloud.upload.dto.InitMultipartUploadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
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

    // 创建一个固定大小的线程池，数量为可用处理器数量的 4 倍， 适用于 I/O 密集型任务
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2, // 核心线程数
            Runtime.getRuntime().availableProcessors() * 4, // 最大线程数
            60L,  // 线程空闲时间
            TimeUnit.SECONDS, // 空闲时间单位
            new LinkedBlockingQueue<>(256), // 阻塞队列(可以指定队列大小，避免内存溢出)
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略(当队列满了，新任务会在调用线程中执行)
    );

    @Autowired
    public FileUploadController(MinioClient minioClient, MinioAsyncClient minioAsyncClient, ReactiveRedisTemplate<String, String> redisTemplate) {
        this.minioClient = minioClient;
        this.minioAsyncClient = minioAsyncClient;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 上传单个中型文件
     * 微小文件：0.2 s
     * 中型文件(几百MB)：1min ~ 2min 左右， 7.5 MB/s
     * @param bucketName 存储桶名称，必须是已存在的存储桶
     * @param objectName 对象名称(需要包括文件后缀，否则下载时无法识别类型), 方法内不检查此参数，但是必须在发起请求时指定此参数
     * @param filePartMono 文件部分的 Mono
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
                return filePartMono.flatMap(filePart -> {// 对文件部分进行处理
                    return DataBufferUtils.join(filePart.content()) // 将多个 DataBuffer 合并为一个
                        .flatMap( // flatMap 将一个数据流中的每个元素映射成一个新的数据流
                            buffer -> { // 对合并后的 DataBuffer 进行处理
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

                                    return Mono.just(ApiResponse.success("文件上传成功！"));
                                } catch (Exception e) {
                                    logger.error("文件上传失败：{}{}", bucketName, objectName, e);
                                    return Mono.just(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件上传失败！"));
                                } finally {
                                    DataBufferUtils.release(buffer);  // 释放 DataBuffer
                                }
                            }
                        );
                    }
                ).defaultIfEmpty(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "未上传文件，请选择要上传的文件。"));  // 如果没有在请求中找到文件，返回错误响应

            }).subscribeOn(Schedulers.fromExecutor(executorService));  // 指定线程池
    }

    /**
     * 上传多个小型文件，使用文件的文件名作为绝对路径，但是在存储时解析它的文件名存储到数据库
     * @param bucketName 存储桶名称，必须是已存在的存储桶
     * @param files 文件部分的异步序列
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

                // 上传文件
                return files.flatMap( filePart -> { // 将 Flux 转换为 Mono
                        String objectName = filePart.headers().getContentDisposition().getFilename();  // 获取文件名称
                        return DataBufferUtils.join(filePart.content())
                            .map( buffer -> { // 将流中的元素转换为 InputStream, map 将一个数据流中的每个元素映射成另一个元素
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
                                    return true;
                                } catch(Exception e) {
                                    return false;
                                } finally {
                                    DataBufferUtils.release(buffer);
                                }
                            });
                    }
                ).collectList().map(uploadResults -> { // 将上传结果收集到一个列表中
                    if(uploadResults.stream().allMatch(Boolean::booleanValue)) {
                        return ApiResponse.success("文件上传成功！");
                    } else {  // 如果有文件上传失败，统计上传成功和错误的文件数量
                        int successCount = (int) uploadResults.stream().filter(Boolean::booleanValue).count();
                        int failureCount = (int) uploadResults.stream().filter(result -> !result).count();

                        FilesUploadResult filesUploadResult =  new FilesUploadResult(successCount, failureCount);
                        logger.error("{} 存在 {} 个文件上传失败！", bucketName, failureCount);

                        return ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "存在" + failureCount + "个文件上传失败！", filesUploadResult);  // 返回上传失败的名称列表
                    }
                });
        }).subscribeOn(Schedulers.fromExecutor(executorService));  // 指定线程池
    }

    /**
     * 初始化分片上传, 每次初始化成功会在redis创建一个List，用于存储分片的唯一标识ETag
     * List的键形式为 uploadId:{uploadId}
     * 每一个分块任务如果在 24 小时内没有完成分片合并，则该任务会被 Minio 自动取消，并删除这个任务的分片
     * bucketName 存储桶名称
     * region 区域名称(可选), 在分布式存储时，可以指定对象的区域
     * objectName 对象名称(需要包括文件后缀，否则下载时无法识别类型)
     * contentType 文件类型
     * return 如果初始化成功，返回上传任务ID
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

                    // 创建一个Multimap来存储请求头信息
                    Multimap<String, String> headers = HashMultimap.create();
                    headers.put("Content-Type", contentType);  // 设置文件类型

                        try {
                            return Mono.fromFuture(minioAsyncClient.createMultipartUploadAsync(bucketName, null, objectName, headers, null))
                                .flatMap(response -> {
                                        logger.info("初始化分片上传任务：{}", response.result().uploadId());
                                        String uploadId = response.result().uploadId();
                                        return Mono.just(ApiResponse.success(uploadId)); // 返回上传任务ID
                                    }
                                )
                                .onErrorResume(e -> Mono.just(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "初始化分片上传失败！")));
                        } catch (Exception e) {
                            logger.error("初始化分片上传任务：{} 失败", objectName);
                            return Mono.error(new RuntimeException(e));
                        }
                    }
                ).subscribeOn(Schedulers.fromExecutorService(executorService));  // 指定线程池
    }

    /**
     * 分片上传， 每次上传成功会将分片的ETag存储到Redis的List中，键为uploadId:{uploadId}
     *
     * @param bucketName   存储桶名称, 推荐指定和初始化上传时相同的存储桶
     * @param uploadId     上传任务ID
     * @param partNumber   分片编号
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
                })
                .subscribeOn(Schedulers.fromExecutorService(executorService)); // 指定线程池
    }

    /**
     * 合并分片，完成分块上传
     * @param bucketName 存储桶名称
     * @param region 区域名称(可选), 在分布式存储时，可以指定对象的区域
     * @param objectName 对象名称(需要包括文件后缀，否则下载时无法识别类型)
     * @param uploadId 上传任务ID
     */
    @PostMapping("/multipart/complete")
    public Mono<ApiResponse<?>> completeMultipartUpload(@RequestPart("bucketName") String bucketName,
                                                        @RequestPart(value="region", required = false) String region,
                                                        @RequestPart("objectName") String objectName,
                                                        @RequestPart("uploadId") String uploadId,
                                                        @RequestPart("contentType") String contentType) {
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
                                        .doOnNext(result -> logger.info("完成分块上传任务：{}, 分块合并成功：{}{}", uploadId, bucketName, objectName)) // 确保执行完成 Redis 删除再返回
                                        .thenReturn(ApiResponse.success(objectName + " 分块上传成功！"))
                        );
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException(e));
                    }
                }
            ).subscribeOn(Schedulers.fromExecutorService(executorService));  // 指定线程池
    }
}
