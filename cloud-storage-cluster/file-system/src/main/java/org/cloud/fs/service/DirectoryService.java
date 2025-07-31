package org.cloud.fs.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.cloud.fs.exception.RootDirectoryMoveException;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.exception.DirectoryAlreadyExistsException;
import org.cloud.fs.exception.DirectoryNotEmptyException;
import org.cloud.fs.exception.DirectoryNotFoundException;
import org.cloud.fs.mappers.DirectoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Transactional(rollbackFor = Exception.class)
public class DirectoryService {
    private final static Logger logger = LoggerFactory.getLogger(DirectoryService.class);
    private static final String ROOT_DIRECTORY_CACHE_PREFIX = "root_directory_cache:";
    private static final String DIRECTORY_PATH_CACHE_PREFIX = "directory_path_cache:";

    private final DirectoryMapper directoryMapper;
    private final MinioClient minioClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final Executor executor;

    @Autowired
    public DirectoryService(DirectoryMapper directoryMapper,
                            MinioClient minioClient,
                            ReactiveRedisTemplate<String, String> redisTemplate,
                            @Qualifier("sharedExecutor") Executor executor) {
        this.directoryMapper = directoryMapper;
        this.minioClient = minioClient;
        this.redisTemplate = redisTemplate;
        this.executor = executor;
    }

    /**
     * 创建用户的根目录，根目录的父目录ID为NULL
     * @param userId 用户ID
     * @return 根目录ID，如果已存在则返回已有的根目录ID，否则创建根目录并返回
     */
    public Mono<String> createRootDirectory(String userId) {
        // 将多个阻塞操作包装成 Mono 冷流，只在订阅时执行
        return Mono.fromCallable(
                () -> {
                    UUID uuidUserId = UUID.fromString(userId);

                    // 检查根目录是否已经创建
                    String rootId = directoryMapper.getRootDirectoryId(uuidUserId);
                    if(rootId != null) {
                        return rootId; // 已有直接返回
                    }

                    // 不存在根目录，创建根目录
                    UUID directoryId = UUID.randomUUID();
                    directoryMapper.createDirectory(directoryId,null, uuidUserId, "/");

                    // 创建以用户ID为名称的 bucket
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

                    logger.info("用户 {} 初始化文件系统的根目录 {} 成功.", userId, directoryId);

                    return directoryId.toString();
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }

    /**
     * 获取用户的根目录
     * @param userId 用户ID
     * @return 目录对象
     */
    public Mono<MinioDirectory> getRootDirectory(UUID userId) {
        // 查询 Redis 中是否有缓存的 directoryId
        String rootDirKey = ROOT_DIRECTORY_CACHE_PREFIX + userId;
        return redisTemplate.opsForValue().get(rootDirKey) // 先读取缓存
            .filter(dirId -> dirId != null && !dirId.isEmpty())
            .switchIfEmpty( // 缓存未命中，查询数据库
                Mono.defer(
                        () -> Mono.fromCallable(
                                () -> {
                                    String dirId = directoryMapper.getRootDirectoryId(userId);
                                    if (dirId == null || dirId.isEmpty()) {
                                        logger.error("用户 {} 的根目录异常，未查询到有效的根目录ID", userId);
                                        throw new IllegalArgumentException("未查询到根目录 ID");
                                    }
                                    return dirId;
                                }
                        ).flatMap(
                                dirId -> redisTemplate.opsForValue().set(rootDirKey, dirId, Duration.ofMinutes(60))
                                        .thenReturn(dirId)
                        )
                ).subscribeOn(Schedulers.fromExecutor(executor))
            ).flatMap(dirId -> // 根据目录 id 再查询一次数据库拿完整对象
                 Mono.fromCallable(
                    () ->  directoryMapper.getDirectoryById(UUID.fromString(dirId))
                 ).subscribeOn(Schedulers.fromExecutor(executor))
            );
    }

    /**
     * 创建目录
     *
     * @param directory 目录对象
     * @return 目录对象
     * @throws DirectoryNotFoundException      父目录不存在
     * @throws DirectoryAlreadyExistsException 目录已存在
     */
    public Mono<MinioDirectory> createDirectory(MinioDirectory directory) throws DirectoryAlreadyExistsException{
        return Mono.fromCallable(
                () -> {
                    UUID parentId = UUID.fromString(directory.getParentDirectoryId());
                    UUID userId   = UUID.fromString(directory.getUserId());
                    String name   = directory.getName();

                    String existingId = directoryMapper.getDirectoryId(parentId, name);
                    if (existingId != null) {
                        throw new DirectoryAlreadyExistsException(name);
                    }

                    UUID directoryId = UUID.randomUUID();
                    directoryMapper.createDirectory(directoryId, parentId, userId, name);
                    return directoryMapper.getDirectory(directoryId);
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }

    /**
     * 根据目录ID查询目录名称
     * @param directoryId 目录ID
     * @return 文件名称
     */
    public Mono<String> getDirectoryName(UUID directoryId) {
        return Mono.fromCallable(
                () -> directoryMapper.getDirectoryName(directoryId)
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }

    /**
     * 更新目录名称
     * @param directoryId 目录ID
     * @param name 目录名称
     */
    public Mono<Integer> updateDirectoryName(UUID directoryId, String name) {
        return Mono.fromCallable(
                () -> directoryMapper.updateDirectoryName(directoryId, name)
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }

    /**
     * 删除目录
     * @param directoryId 目录ID
     * @throws DirectoryNotEmptyException 目录不为空
     */
    public Mono<Integer> deleteDirectory(UUID directoryId) {
        return Mono.fromCallable(
                () -> {
                    Boolean isEmpty = directoryMapper.isDirectoryEmpty(directoryId);
                    if(isEmpty) {
                        return directoryMapper.deleteDirectory(directoryId);
                    } else {
                        throw new DirectoryNotEmptyException(directoryId.toString());
                    }
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }

    /**
     * 根据父目录ID查询子目录
     * @param parentDirectoryId 父目录ID
     * @return 子目录列表
     * @throws DirectoryNotFoundException 目录不存在
     */
    public Mono<List<MinioDirectory>> getChildDirectories(UUID parentDirectoryId){
        return Mono.fromCallable(
                () -> directoryMapper.getDirectoriesByParentDirectoryId(parentDirectoryId)
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }

    /**
     * 移动目录
     * @param directoryId 目录ID
     * @param parentDirectoryId 目录ID
     * @throws DirectoryNotFoundException 目录不存在
     * @throws DirectoryNotEmptyException 目录不为空
     */
     public Mono<Integer> moveDirectory(UUID directoryId, UUID parentDirectoryId) throws DirectoryNotFoundException, DirectoryNotEmptyException{
         String pathKey = DIRECTORY_PATH_CACHE_PREFIX + directoryId;

         // 检查目录是否存在，目录存在返回true，否则false
         Mono<Boolean> dirExist = Mono.fromCallable(
                 () -> directoryMapper.isDirectoryExist(parentDirectoryId)
         ).subscribeOn(Schedulers.fromExecutor(executor));

         // 检查目录是否不是根目录，目录不是根目录返回 true，否则false
         Mono<Boolean> isNotRootDir = Mono.fromCallable(
                 () -> !directoryMapper.getDirectoryName(directoryId).equals("/")
         );

         // 检查目录是否为空，目录为空返回 true，否则false
         Mono<Boolean> dirEmpty = Mono.fromCallable(
                 () -> directoryMapper.isDirectoryEmpty(directoryId)
         );

         return dirExist.filter(Boolean::booleanValue) // 检查目录是否存在，过滤掉目录不存在的情况
        .switchIfEmpty(Mono.error(new DirectoryNotFoundException(parentDirectoryId.toString()))) // 目录不存在
        .then(isNotRootDir).filter(Boolean::booleanValue) // 检查移动的目录是否不是根目录，过滤掉目录为根目录的情况
        .switchIfEmpty(Mono.error(new RootDirectoryMoveException(directoryId.toString()))) // 被移动的目录为根目录
        .then(dirEmpty).filter(Boolean::booleanValue) // 检查目录是否为空，过滤掉目录不为空的情况
        .switchIfEmpty(Mono.error(new DirectoryNotEmptyException(directoryId.toString()))) // 目录不为空
        .then(Mono.fromCallable(() -> redisTemplate.delete(pathKey))) // 清除目录缓存
        .then(Mono.fromCallable(() -> directoryMapper.moveDirectory(directoryId, parentDirectoryId))); // 移动目录
     }
}
