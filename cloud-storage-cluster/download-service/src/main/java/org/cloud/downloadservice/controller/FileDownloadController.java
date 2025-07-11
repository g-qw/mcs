package org.cloud.downloadservice.controller;

import io.minio.*;
import org.cloud.downloadservice.dto.InitZipDownloadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
@RequestMapping("/download")
public class FileDownloadController {
    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    private final MinioClient minioClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final static String ZIP_TASK_ID_PREFIX = "zip_task_id:";
    private final static String ZIP_TASK_FILES_PREFIX = "zip_task_files:";

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
    public FileDownloadController(MinioClient minioClient, ReactiveRedisTemplate<String, String> redisTemplate) {
        this.minioClient = minioClient;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 下载文件
     *
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

                Resource resource = new InputStreamResource(getObjectResponse);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(statObjectResponse.contentType()))
                        .contentLength(statObjectResponse.size())
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(statObjectResponse.object(), StandardCharsets.UTF_8))
                        .body(resource);

            } catch (Exception e) {
                logger.error("下载文件 {}{} 失败", bucket, object, e);
                return ResponseEntity.status(500).build();
            }
        }, executorService))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).build()));
    }

    /**
     * 分块下载， 在 HTTP Range头中指定文件的起始字节和结束字节
     * HTTP Range头的格式为：bytes=start-end
     * 如果未指定Range头，那么默认范围是整个文件，即下载整个文件
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
     * JSON 格式:
     * - bucket 存储桶名称
     * - objects 文件名称列表, 即文件在 Minio 中的绝对路径
     * @return 返回zip任务id
     */
    @PostMapping("/init_zip_download")
    public Mono<ResponseEntity<?>> initZipDownload(@Validated @RequestBody InitZipDownloadRequest request) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(
                () -> {
                    // 生成zip任务id
                    String zipTaskId = UUID.randomUUID().toString();
                    String zipTaskIdKey = ZIP_TASK_ID_PREFIX + zipTaskId;
                    String zipTaskFilesKey = ZIP_TASK_FILES_PREFIX + zipTaskId;

                    // Redis 中完成以下映射
                    // zipTaskId -> bucket
                    // zipTaskId -> list[file names]
                    return redisTemplate.opsForValue().set(zipTaskIdKey, request.getBucket(), Duration.ofHours(24))
                            .flatMap(v -> {
                                if (Boolean.TRUE.equals(v)) {
                                    // 将文件列表存储到Redis的List中
                                    return redisTemplate.opsForList().leftPushAll(zipTaskFilesKey, request.getObjects())
                                            .flatMap(result -> {
                                                    // 设置24小时过期时间
                                                    return redisTemplate.expire(zipTaskFilesKey, Duration.ofHours(24)).thenReturn(zipTaskId);
                                                }
                                            )
                                            .onErrorResume(e -> Mono.error(new RuntimeException("Failed to store zip task id in Redis")));
                                } else {
                                    return Mono.error(new RuntimeException("Failed to store zip task id in Redis")); // 如果存储失败，抛出异常
                                }
                            })
                            .onErrorResume(e -> {
                                return Mono.error(new RuntimeException("Error occurred when storing zip task id in Redis", e)); // 返回错误响应
                            })
                            .toFuture(); // 将Mono转换为Future
                },
                executorService
            )
        ).flatMap(zipTaskId -> Mono.just(ResponseEntity.ok(zipTaskId)));
    }

    @PostMapping("/zip_download")
    public Mono<ResponseEntity<?>> downloadFilesAsZip(@RequestParam("zipTaskId") String zipTaskId) {
        return Mono.just(ResponseEntity.ok("OK"));
    }
}
