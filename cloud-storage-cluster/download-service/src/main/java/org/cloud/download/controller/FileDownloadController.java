package org.cloud.download.controller;

import io.minio.*;
import io.minio.errors.MinioException;
import org.cloud.download.dto.ApiResponse;
import org.cloud.download.dto.GetFilesSizeRequest;
import org.cloud.download.dto.InitZipDownloadRequest;
import org.cloud.download.dto.ZipDownloadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/download")
public class FileDownloadController {
    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    private final MinioClient minioClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient webClient;

    private final static String ZIP_TASK_ID_PREFIX = "zip_task_id:";
    private final static String ZIP_TASK_FILES_PREFIX = "zip_task_files:";
    private final static Long ZIP_DOWNLOAD_LIMIT = 256 * 1024 * 1024L;

    // 创建一个固定大小的线程池，数量为可用处理器数量的 4 倍， 适用于 I/O 密集型任务
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2, // 核心线程数
            Runtime.getRuntime().availableProcessors() * 4, // 最大线程数
            60L,  // 线程空闲时间
            TimeUnit.SECONDS, // 空闲时间单位
            new LinkedBlockingQueue<>(), // 阻塞队列(可以指定队列大小，避免内存溢出)
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略(当队列满了，新任务会在调用线程中执行)
    );

    @Autowired
    public FileDownloadController(MinioClient minioClient,
                                  ReactiveRedisTemplate<String, String> redisTemplate,
                                  WebClient webClient) {
        this.minioClient = minioClient;
        this.redisTemplate = redisTemplate;
        this.webClient = webClient;
    }

    /**
     * 下载文件
     * @param bucket 存储桶名称
     * @param object 文件名称, 即文件在 Minio 中的绝对路径
     * @return 文件的同步I/O输入流
     */
    @GetMapping("/file")
    public Mono<ResponseEntity<?>> downloadFile(@RequestParam("bucket") String bucket,
                                                @RequestParam("object") String object) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(() -> {
            try {
                // 获取文件的元数据
                StatObjectResponse statObjectResponse = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucket)
                                .object(object)
                                .build()
                );

                // 获取文件的同步I/O输入流
                GetObjectResponse getObjectResponse = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(object)
                                .build()
                );

                logger.info("下载单文件 {}{} 成功", bucket, object);

                Resource resource = new InputStreamResource(getObjectResponse); // 文件资源
                String objectName = statObjectResponse.object(); // minio 对象名称
                String fileName = objectName.contains("/") ? objectName.substring(objectName.lastIndexOf("/")) : objectName; // 文件名称

                // 构造请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(statObjectResponse.contentType()));
                headers.setContentLength(statObjectResponse.size());
                headers.setContentDispositionFormData("attachment", fileName);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);

            } catch (Exception e) {
                logger.error("下载文件 {}{} 失败", bucket, object, e);
                return ResponseEntity.status(500).build();
            }
        }, executorService))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).build()));
    }

    /**
     * 分块下载
     * @apiNote 在 HTTP Range头中指定文件的起始字节和结束字节
     * @apiNote HTTP Range头的格式为：bytes=start-end
     * @apiNote 如果未指定Range头，那么默认范围是整个文件，即下载整个文件
     * @param bucket 存储桶名称
     * @param object 文件名称, 即文件在 Minio 中的绝对路径
     * @return 分块数据
     */
    @GetMapping("/multipart_download")
    public Mono<ResponseEntity<?>> multipartDownload(@RequestParam("bucket") String bucket,
                                                     @RequestParam("object") String object,
                                                     @RequestHeader(value = "Range", required = false) String range) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(
                () -> {
                    try {
                        // 获取文件大小
                        StatObjectResponse statObjectResponse = minioClient.statObject(
                                StatObjectArgs.builder()
                                        .bucket(bucket)
                                        .object(object)
                                        .build()
                        );
                        long fileSize = statObjectResponse.size();

                        // 解析Range请求头
                        long start = 0;
                        long end = fileSize - 1;
                        if (range != null && range.startsWith("bytes=")) {
                            String[] parts = range.substring(6).split("-");
                            start = Long.parseLong(parts[0]);
                            if (parts.length > 1 && !parts[1].isEmpty()) {
                                end = Long.parseLong(parts[1]);
                            }
                            end = Math.min(end, fileSize - 1);
                        }

                        // 计算分块的长度
                        long length = end - start + 1;

                        // 使用GetObjectArgs直接获取指定范围的数据
                        byte[] partFile = minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(bucket)
                                        .object(object)
                                        .offset(start)
                                        .length(length)
                                        .build()
                        ).readAllBytes();

                        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                                .body(partFile);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                }, executorService
        )).onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * 初始化zip下载
     * @apiNote 初始化中会向 file-system 服务获取文件列表中所有文件的总字节大小，如果总大小超过 256 MB，则不允许下载以避免对服务器造成过大的负荷
     * @return 返回zip任务id
     */
    @PostMapping("/init_zip_download")
    public Mono<ResponseEntity<?>> initZipDownload(@RequestBody @Validated InitZipDownloadRequest request) {
        List<String> files = request.getFiles();
        List<String> fileIds = request.getFileIds();

        // 检查文件路径列表和文件ID列表的长度是否相同
        if(files.size() != fileIds.size()) {
            return Mono.just(ResponseEntity.badRequest().body("文件列表和文件ID列表的长度不匹配"));
        }

        // 检查打包的文件总大小是否超过 256 MB
        Mono<ApiResponse> response = webClient.post()
                .uri("http://localhost:8104/get_files_size")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GetFilesSizeRequest(fileIds))
                .retrieve()
                .bodyToMono(ApiResponse.class);

        return response.flatMap(
                apiResponse -> {
                    if(apiResponse.getCode() != 200) {
                        return Mono.just(ResponseEntity.status(500).body("请求远程文件系统服务失败"));
                    }

                    Long size = Long.parseLong((String) apiResponse.getData());
                    if(size > ZIP_DOWNLOAD_LIMIT) {
                        return Mono.just(ResponseEntity.badRequest().body("zip打包的文件总大小不允许超过" + ZIP_DOWNLOAD_LIMIT / 1024 / 1024 + " MB"));
                    } else {
                        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(
                                        () -> {
                                            // 生成zip任务id
                                            String zipTaskId = UUID.randomUUID().toString();
                                            String zipTaskIdKey = ZIP_TASK_ID_PREFIX + zipTaskId;
                                            String zipTaskFilesKey = ZIP_TASK_FILES_PREFIX + zipTaskId;

                                            // Redis 中完成以下映射
                                            // zipTaskId -> bucket
                                            // zipTaskFiles -> list[absolute filePath]
                                            return redisTemplate.opsForValue().set(zipTaskIdKey, request.getBucket(), Duration.ofHours(24))
                                                    .flatMap(v -> {
                                                        if (Boolean.TRUE.equals(v)) {
                                                            // 将文件列表存储到Redis的List中
                                                            return redisTemplate.opsForList().leftPushAll(zipTaskFilesKey, files)
                                                                    .flatMap(result -> {
                                                                                logger.info("创建zip下载任务：{}, 文件总大小：{} bytes", zipTaskId, size);

                                                                                // 设置24小时过期时间
                                                                                return redisTemplate.expire(zipTaskFilesKey, Duration.ofHours(24)).thenReturn(zipTaskId);
                                                                            }
                                                                    )
                                                                    .onErrorResume(e -> Mono.error(new RuntimeException("将zip下载任务的文件列表存储到redis失败")));
                                                        } else {
                                                            return Mono.error(new RuntimeException("存储zip下载的任务ID到redis失败")); // 如果存储失败，抛出异常
                                                        }
                                                    })
                                                    .toFuture(); // 将Mono转换为Future
                                        },
                                        executorService
                                )
                        ).flatMap(zipTaskId -> Mono.just(ResponseEntity.ok(zipTaskId)));
                    }
                }
        );
    }

    /**
     * 将多个文件打包成zip下载
     * @return zip字节流
     */
    @PostMapping("/zip_download")
    public Mono<ResponseEntity<byte[]>> downloadFilesAsZip(@RequestBody @Validated ZipDownloadRequest request) {
        String zipTaskId = request.getZipTaskId();
        String target = request.getTarget();
        String zipTaskIdKey = ZIP_TASK_ID_PREFIX + zipTaskId;
        String zipTaskFilesKey = ZIP_TASK_FILES_PREFIX + zipTaskId;

        return Mono.zip(
                redisTemplate.opsForValue().get(zipTaskIdKey), // 获取存储桶
                redisTemplate.opsForList().range(zipTaskFilesKey, 0, -1).collectList() // 获取整个文件列表
        ).flatMap(
                tuple -> {
                    String bucket = tuple.getT1();
                    List<String> files = tuple.getT2();

                    return Mono.fromCallable(() -> {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                            for (String file : files) {
                                // 构建 minio 的获取对象的参数
                                GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                                        .bucket(bucket)
                                        .object(file)
                                        .build();

                                // 文件名称，用作 zip 压缩包中的文件名称
                                String fileName = file.contains("/") ? file.substring(file.lastIndexOf("/") + 1) : file;

                                try (InputStream is = minioClient.getObject(getObjectArgs)) {
                                    ZipEntry zipEntry = new ZipEntry(fileName);
                                    zos.putNextEntry(zipEntry);
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = is.read(buffer)) != -1) {
                                        zos.write(buffer, 0, len);
                                    }
                                    zos.closeEntry();
                                } catch(MinioException minioException) {
                                    throw new RuntimeException("下载文件 " + fileName + "遇到错误");
                                } catch(IOException ioException) {
                                    throw new RuntimeException("将文件 " + fileName + "写入 zip 流时遇到错误");
                                }
                            }
                            zos.finish(); // 完成 ZIP 文件的写入
                        } catch (Exception e) {
                            logger.error("zip 下载任务 {} 失败：{}", zipTaskId, e.getMessage());
                            throw e;
                        }

                        // 清理 redis 中zip下载任务的数据
                        redisTemplate.expire(zipTaskIdKey, Duration.ofSeconds(0)).subscribe();
                        redisTemplate.expire(zipTaskFilesKey, Duration.ofSeconds(0)).subscribe();

                        return baos.toByteArray(); // 返回zip字节数组
                    }).subscribeOn(Schedulers.fromExecutorService(executorService)); // 将zip任务提交到线程池执行
                }
        ).map(
                zipData -> {
                    logger.info("zip 下载任务 {} 完成", zipTaskId);

                    HttpHeaders headers = new HttpHeaders();

                    // 确保下载的文件名称以 .zip 结尾
                    if(target.endsWith(".zip")) {
                        headers.setContentDispositionFormData("attachment", target);
                    } else {
                        headers.setContentDispositionFormData("attachment", target + ".zip");
                    }

                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    return ResponseEntity.ok().headers(headers).body(zipData);
                }
        );
    }
}
