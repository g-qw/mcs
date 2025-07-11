package org.cloud.fs.controller;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.cloud.fs.dto.*;
import org.cloud.fs.exception.*;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.model.MinioFile;
import org.cloud.fs.service.DirectoryService;
import org.cloud.fs.service.FileService;
import org.cloud.fs.service.PathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
public class FsController {
    private final static Logger logger = LoggerFactory.getLogger(FsController.class);

    private final DirectoryService directoryService;
    private final FileService fileService;
    private final PathService pathService;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final MinioClient minioClient;

    private final String USER_CACHE_PREFIX = "user:";
    private final String ID_CACHE_PREFIX = "id:";
    private final String PATH_CACHE_PREFIX = "path:";
    private final String DIR_CACHE_PREFIX = "dir:";

    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2, // 核心线程数
            Runtime.getRuntime().availableProcessors() * 4, // 最大线程数
            60L,  // 线程空闲时间
            TimeUnit.SECONDS, // 空闲时间单位
            new LinkedBlockingQueue<>(), // 阻塞队列(可以指定队列大小，避免内存溢出)
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略(当队列满了，新任务会在调用线程中执行)
    );

    @Autowired
    public FsController(DirectoryService directoryService,
                        FileService fileService,
                        PathService pathService,
                        ReactiveRedisTemplate<String, Object> redisTemplate,
                        MinioClient minioClient) {
        this.directoryService = directoryService;
        this.fileService = fileService;
        this.pathService = pathService;
        this.redisTemplate = redisTemplate;
        this.minioClient = minioClient;
    }

    /**
     * 创建根目录，如果根目录已经存在则返回已存在的根目录
     * @param userId 用户ID
     * @return 根目录ID
     */
    @PostMapping("/root_dir")
    public Mono<ApiResponse<?>> createRootDirectory(@RequestParam("userId") String userId) {
        return Mono.fromCallable(
            () -> {
                String rootDirectoryId = directoryService.createRootDirectory(UUID.fromString(userId));

                // 创建以用户ID为名称的 bucket
                try {
                    // 检查 bucket 是否存在
                    boolean isBucketExist = minioClient.bucketExists(
                            BucketExistsArgs.builder()
                                    .bucket(userId)
                                    .build()
                    );

                    // 如果 bucket 不存在则创建 bucket
                    if(!isBucketExist) {
                        minioClient.makeBucket(
                                MakeBucketArgs.builder()
                                        .bucket(userId)
                                        .build()
                        );
                    }

                    logger.info("用户 {} 初始化文件系统的根目录 {} 成功.", userId, rootDirectoryId);

                    return ApiResponse.success(rootDirectoryId);
                } catch (Exception e) {
                    logger.error("用户 {} 初始化文件系统的根目录 {} 失败.", userId, rootDirectoryId);
                    return ApiResponse.failure(500, "服务器内部错误");
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 检查目录是否存在
     * @param directoryId 目录ID
     * @return true 表示存在, false 表示不存在
     */
    @GetMapping("/is_dir_exist")
    public Mono<ApiResponse<Boolean>> isDirectoryExist(@RequestParam("directoryId") String directoryId) {
        return Mono.fromCallable(
            () -> {
                Boolean isDirectoryExist = directoryService.isDirectoryExist(UUID.fromString(directoryId));
                return ApiResponse.success(isDirectoryExist);
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 检查文件是否存在
     * @param fileId 文件ID
     * @return true 表示存在, false 表示不存在
     */
    @GetMapping("/is_file_exist")
    public Mono<ApiResponse<Boolean>> isFileExist(@RequestParam("fileId") String fileId) {
        return Mono.fromCallable(
            () -> {
                Boolean isFileExist = fileService.isFileExist(UUID.fromString(fileId));
                return ApiResponse.success(isFileExist);
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 创建文件
     * @param file 文件对象, objectName 属性最好包含文件扩展名，否则会被视为 .bin 文件
     * @return 文件的绝对路径，上传文件时使用此路径作为对象名称上传文件
     */
    @PostMapping("/add_file")
    public Mono<ApiResponse<?>> addFile(@Validated @RequestBody MinioFile file) {
        return Mono.fromCallable(
            () -> {
                try {
                    // 检查文件是否包含文件扩展名
                    String objectName = file.getObjectName();
                    String regex = ".*\\.[^.]+$";
                    boolean hasExtension = objectName.matches(regex);
                    if(!hasExtension) {
                        file.setObjectName(objectName + ".bin"); // 如果没有文件扩展名，则默认视为 .bin 文件
                    }

                    String fileId = fileService.createFile(file);

                    logger.info("用户 {} 创建文件 {} 成功.", file.getUserId(), file.getObjectName());

                    return ApiResponse.success(fileId);
                } catch(DirectoryNotFoundException | FileAlreadyExistsException e) {
                    return ApiResponse.failure(400 ,e.getMessage());
                }
                catch(Exception e) {
                    logger.error(e.getMessage());
                    return ApiResponse.failure(500, "服务器内部错误");
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 获取文件
     * @param fileId 文件ID
     * @return 文件对象
     */
    @GetMapping("/get_file")
    public Mono<ApiResponse<?>> getFile(@RequestParam("fileId") String fileId) {
        return Mono.fromCallable(
            () -> {
                MinioFile file = fileService.getFileById(UUID.fromString(fileId));
                if(file == null) {
                    return ApiResponse.failure(400, "文件不存在");
                }

                return ApiResponse.success(file);
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /*
     * 获取文件名称
     * @param fileId 文件
     */
    @GetMapping("get_file_name")
    public Mono<ApiResponse<?>> getFileName(@RequestParam("fileId") String fileId) {
        return Mono.fromCallable(
                () -> {
                    String name = fileService.getFileName(UUID.fromString(fileId));
                    if(name == null) {
                        return ApiResponse.failure(400, "文件不存在");
                    }

                    return ApiResponse.success(name);
                }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 删除文件
     * @param fileId 文件ID
     */
    @PostMapping("/delete_file")
    public Mono<ApiResponse<?>> deleteFile(@RequestParam("fileId") String fileId) {
        return Mono.fromCallable(
            () -> {
                String userId;
                String objectName;
                List<DeleteObject> deleteObjects = new ArrayList<>();

                // 从数据库中删除文件
                try {
                    // 获取文件信息
                    MinioFile file = fileService.getFileById(UUID.fromString(fileId));
                    userId = file.getUserId();
                    objectName = pathService.getAbsoluteFilePath(UUID.fromString(file.getFileId()));
                    deleteObjects.add(new DeleteObject(objectName));

                    //删除文件
                    fileService.deleteFile(UUID.fromString(fileId));
                } catch(FileNotFoundException e) {
                    return ApiResponse.failure(400, e.getMessage());
                }

                // 在 Minio 中删除文件
                try {
                    Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                            RemoveObjectsArgs.builder()
                                    .bucket(userId)
                                    .objects(deleteObjects)
                                    .build()
                    );
                    for (Result<DeleteError> result : results) {
                        DeleteError error = result.get();
                        if (error != null) {
                            logger.error("minio 中删除文件发生错误： " + error.objectName() + "; " + error.message());
                        }
                    }
                } catch(Exception e) {
                    return ApiResponse.failure(500, "服务器内部错误");
                }

                logger.info("用户 {} 删除文件 {} 成功.", userId, fileId);

                return ApiResponse.success("删除文件成功");
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 更新文件
     * @param file 文件对象
     */
    @PostMapping("/update_file")
    public Mono<ApiResponse<?>> updateFile(@Validated @RequestBody MinioFile file) {
        return Mono.fromCallable(
            () -> {
                try {
                    fileService.updateFile(file);
                    return ApiResponse.success("更新文件成功");
                } catch(DirectoryNotFoundException e) {
                    return ApiResponse.failure(400, e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 更新文件名称
     * @return 更新文件名称的结果
     */
    @PostMapping("/update_file_name")
    public Mono<ApiResponse<?>> updateFileName(@RequestBody Map<String, String> request) {
        String fileId = request.get("fileId");
        String objectName = request.get("objectName");
        return Mono.fromCallable(
            () -> {
                int count = fileService.updateFileName(UUID.fromString(fileId), objectName);
                if(count == 0) {
                    return ApiResponse.failure(400, "文件不存在");
                }

                // 更新 minio 中的文件名称

                return ApiResponse.success("更新文件名称成功");
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 移动文件
     *
     * @param request 请求参数
     */
    @PostMapping("/move_file")
    public Mono<ApiResponse<?>> moveFile(@RequestBody Map<String, String> request) {
        return Mono.fromCallable(
            () -> {
                try {
                    // 移动文件
                    UUID fileId = UUID.fromString(request.get("fileId"));
                    UUID directoryId = UUID.fromString(request.get("directoryId"));
                    String path = pathService.getAbsoluteFilePath(fileId); // 文件的原路径
                    fileService.moveFile(fileId, directoryId);
                    String targetPath = pathService.getAbsoluteFilePath(fileId); // 移动后文件的路径

                    // 查询用户ID
                    String userId = fileService.getUserIdByFileId(fileId);

                    // 在 minio 中剪切文件到目标目录
                    try {
                        // 复制对象到目标路径
                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(userId) // 目标桶
                                        .object(targetPath) // 目标对象名称
                                        .source(
                                            CopySource.builder()
                                                    .bucket(userId) // 源桶
                                                    .object(path)
                                                    .build()
                                        ).build()
                        );

                        // 删除原路径的对象
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(userId)
                                        .object(path)
                                        .build()
                        );

                    } catch(Exception e) {
                        return ApiResponse.failure(500, "服务器内部错误");
                    }

                    // 获取文件的绝对路径
                    return ApiResponse.success("移动文件成功");
                } catch(DirectoryNotFoundException | FileAlreadyExistsException e) {
                    return ApiResponse.failure(400 ,e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 创建目录
     *
     * @param directory 目录对象
     * @return 目录ID
     */
    @PostMapping("/create_dir")
    public Mono<ApiResponse<?>> createDirectory(@Validated @RequestBody MinioDirectory directory) {
        return Mono.fromCallable(
            () -> {
                try {
                    MinioDirectory newDirectory = directoryService.createDirectory(directory);

                    logger.info("用户 {} 创建目录 {} 成功.", directory.getUserId(), directory.getDirectoryId());

                    return ApiResponse.success(newDirectory);
                } catch(DirectoryNotFoundException | DirectoryAlreadyExistsException e) {
                    return ApiResponse.failure(400 ,e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 获取目录
     * @param directoryId 目录ID
     * @return 目录对象
     */
    @GetMapping("/get_dir")
    public Mono<ApiResponse<?>> getDirectory(@RequestParam("directoryId") String directoryId) {
        return Mono.fromCallable(
            () -> {
                try {
                    return ApiResponse.success(directoryService.getDirectoryById(UUID.fromString(directoryId)));
                } catch(DirectoryNotFoundException e) {
                    return ApiResponse.failure(400, e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    @GetMapping("/get_dir_name")
    public Mono<ApiResponse<?>> getDirectoryName(@RequestParam("directoryId") String directoryId) {
        return Mono.fromCallable(
                () -> {
                    try {
                        return ApiResponse.success(directoryService.getDirectoryName(UUID.fromString(directoryId)));
                    } catch(DirectoryNotFoundException e) {
                        return ApiResponse.failure(400, e.getMessage());
                    }
                }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 删除目录
     * @param directoryId 目录ID
     * @return 删除目录的结果
     */
    @Deprecated // 尚未实现在 minio 中删除目录
    @PostMapping("/delete_dir")
    public Mono<ApiResponse<?>> deleteDirectory(@RequestParam("directoryId") String directoryId) {
        return Mono.fromCallable(
            () -> {
                try {
                    directoryService.deleteDirectory(UUID.fromString(directoryId));
                    return ApiResponse.success("删除目录成功");
                } catch(DirectoryNotFoundException e) {
                    return ApiResponse.failure(400, "尝试删除不存在的目录被拒绝");
                } catch(DirectoryNotEmptyException e) {
                    return ApiResponse.failure(400, "无法删除不为空的目录，请先删除目录下的所有文件和目录");
                } catch(RootDirectoryDeletionException e) {
                    return ApiResponse.failure(400, e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 更新目录
     * @param directoryId 目录ID
     * @param name 新的目录名称
     * @return 更新目录的结果
     */
    @PostMapping("/update_dir_name")
    public Mono<ApiResponse<?>> updateDirectory(@RequestParam String directoryId,
                                                @RequestParam String name) {
        return Mono.fromCallable(
            () -> {
                try {
                    directoryService.updateDirectoryName(UUID.fromString(directoryId), name);
                    return ApiResponse.success("更新目录成功");
                } catch(DirectoryNotFoundException e) {
                    return ApiResponse.failure(400, e.getMessage());
                } catch(DuplicateKeyException e) {
                    return ApiResponse.failure(400, "文件夹" + name + "已存在");
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 移动目录
     * @param request 请求参数,包含目录ID和父目录ID
     * @return 移动目录的结果
     */
    @Deprecated // 尚未实现在 minio 中移动目录
    @PostMapping("/move_dir")
    public Mono<ApiResponse<?>> moveDirectory(@RequestBody Map<String, String> request) {
        return Mono.fromCallable(
            () -> {
                try {
                    UUID directoryId = UUID.fromString(request.get("directoryId"));
                    UUID parentDirectoryId = UUID.fromString(request.get("parentDirectoryId"));
                    directoryService.moveDirectory(directoryId, parentDirectoryId);

                    return ApiResponse.success("移动目录成功");
                } catch(DirectoryNotFoundException | FileAlreadyExistsException e) {
                    return ApiResponse.failure(400 ,e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 将初始化过的目录缓存到Redis中，以便快速查询
     * @param dirContent 目录的数据
     */
    private Mono<Boolean> cacheDirContent(MinioDirContent dirContent) {
        // 创建一个 Map 来存储所有的键值对
        Map<String, Object> map = new HashMap<>();

        String id = dirContent.getDirectoryId();
        String path = dirContent.getPath();

        // 将同步方法包装为响应式
        return Mono.fromCallable(
            () -> directoryService.getUserIdByDirectoryId(UUID.fromString(id)))  // 获取目录的用户ID
            .map(userId -> {
                String key = USER_CACHE_PREFIX + userId;
                map.put(ID_CACHE_PREFIX + id, path);
                map.put(PATH_CACHE_PREFIX + path, id);
                map.put(DIR_CACHE_PREFIX + id, dirContent);
                return key;
            })
            .flatMap(key -> redisTemplate.opsForHash().putAll(key, map))
            .subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 加载目录的数据，文件夹、文件
     * @param directoryId 目录ID
     * @return 目录的数据
     */
    @PostMapping("/load_dir")
    public Mono<ApiResponse<MinioDirContent>> loadDirContent(@RequestParam("directoryId") String directoryId) {
        return Mono.fromCallable(
                () -> {
                    UUID id = UUID.fromString(directoryId);
                    String path = pathService.getAbsoluteDirectoryPath(id); // 获取目录的绝对路径
                    List<MinioDirectory> directories = directoryService.getDirectoriesByParentDirectoryId(id); // 获取目录的子目录
                    List<MinioFile> files = fileService.getFilesByDirectoryId(id);  // 获取目录的文件

                    return new MinioDirContent(path, directoryId, directories, files);
                }
        ).map(
                ApiResponse::success  // 将目录的内容封装到 ApiResponse 中返回
        ).onErrorResume(
                e -> {
                    logger.error(e.getMessage());
                    return Mono.just(ApiResponse.failure(500, "初始化目录" + directoryId + "失败，服务器内部错误"));
                }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 解析路径
     * @param request 请求参数, 包含路径和用户ID
     * @return 目录对象
     */
    @PostMapping("/parse_dir_path")
    public Mono<ApiResponse<?>> parseDirPath(@RequestBody ParsePathRequest request) {
        return Mono.fromCallable(
            () -> {
                try {
                    String path = request.getPath();
                    String userId = request.getUserId();
                    MinioDirectory directory = pathService.getDirectory(path, UUID.fromString(userId));

                    return ApiResponse.success(directory);
                } catch(DirectoryPathException e) {
                    return ApiResponse.failure(400, e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    @PostMapping("/parse_file_path")
    public Mono<ApiResponse<?>> parseFilePath(@RequestBody ParsePathRequest request) {
        return Mono.fromCallable(
                () -> {
                    try {
                        String path = request.getPath();
                        String userId = request.getUserId();
                        MinioFile file = pathService.getFile(path, UUID.fromString(userId));

                        return ApiResponse.success(file);
                    } catch(FilePathException e) {
                        return ApiResponse.failure(400, e.getMessage());
                    }
                }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 获取文件的绝对路径
     * @param fileId 文件ID
     * @return 文件的绝对路径
     */
    @GetMapping("/get_file_path")
    public Mono<ApiResponse<?>> getFilePath(@RequestParam("fileId") String fileId) {
        return Mono.fromCallable(
            () -> {
                try {
                    String path = pathService.getAbsoluteFilePath(UUID.fromString(fileId));
                    return ApiResponse.success(path);
                } catch (FileNotFoundException e) {
                    return ApiResponse.failure(400, e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }

    /**
     * 获取目录的绝对路径
     * @param directoryId 目录ID
     * @return 目录的绝对路径
     */
    @GetMapping("/get_dir_path")
    public Mono<ApiResponse<?>> getDirectoryPath(@RequestParam("directoryId") String directoryId) {
        return Mono.fromCallable(
            () -> {
                try {
                    String path = pathService.getAbsoluteDirectoryPath(UUID.fromString(directoryId));
                    return ApiResponse.success(path);
                } catch (DirectoryNotFoundException e) {
                    return ApiResponse.failure(400, e.getMessage());
                }
            }
        ).subscribeOn(Schedulers.fromExecutor(executorService));
    }
}
