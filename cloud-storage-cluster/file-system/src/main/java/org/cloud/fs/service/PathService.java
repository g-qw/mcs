package org.cloud.fs.service;

import org.cloud.fs.exception.DirectoryNotFoundException;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.exception.FileNotFoundException;
import org.cloud.fs.mappers.DirectoryMapper;
import org.cloud.fs.mappers.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class PathService {
    private final FileMapper fileMapper;
    private final DirectoryMapper directoryMapper;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final Executor executor;

    // 最大的目录深度
    private static final int MAX_DIRECTORY_DEPTH = 32;
    private static final String DIRECTORY_PATH_CACHE_PREFIX = "directory_path_cache:";

    @Autowired
    public PathService(FileMapper fileMapper, DirectoryMapper directoryMapper, ReactiveRedisTemplate<String, String> redisTemplate, @Qualifier("sharedExecutor") Executor executor) {
        this.fileMapper = fileMapper;
        this.directoryMapper = directoryMapper;
        this.redisTemplate = redisTemplate;
        this.executor = executor;
    }

    /**
     * 获取目录的绝对路径
     * 路径结构: /dir1/.../
     * @param directoryId 目录ID
     * @return 目录的绝对路径
     */
    public Mono<String> getAbsoluteDirectoryPath(UUID directoryId) throws DirectoryNotFoundException {
        // 获取读取目录对象
        Mono<MinioDirectory> directoryMono = Mono.fromCallable(
                () -> directoryMapper.getDirectory(directoryId)
        ).subscribeOn(Schedulers.fromExecutor(executor));

        return directoryMono.flatMap(
            directory -> {
                // 目录不存在，抛出异常
                if(directory == null) {
                    return Mono.error(new DirectoryNotFoundException(directoryId.toString()));
                }

                // 根目录返回 "/"
                if("/".equals(directory.getName())) {
                    return Mono.just("/");
                }

                // 尝试读取缓存
                String pathKey = DIRECTORY_PATH_CACHE_PREFIX + directoryId;
                return readCachedPath(pathKey)
                    .switchIfEmpty(
                        // 缓存未命中 -> 计算路径 -> 写缓存 -> 返回
                        Mono.fromCallable(
                                () -> buildPath(directory)
                        ).subscribeOn(Schedulers.fromExecutor(executor))
                        .flatMap(
                                path -> writeCachedPath(pathKey, path).thenReturn(path)
                        )
                    );
            }
        );
    }

    /**
     * 读取缓存
     * @param pathKey 指定目录的key
     * @return 缓存的路径
     */
    private Mono<String> readCachedPath(String pathKey) {
        return redisTemplate.opsForValue().get(pathKey)
                .filter(Objects::nonNull);
    }

    /**
     * 写缓存
     * @param pathKey 指定目录的key
     * @param path 缓存的路径
     * @return 无
     */
    private Mono<Boolean> writeCachedPath(String pathKey, String path) {
        return redisTemplate.opsForValue().set(pathKey, path, Duration.ofMinutes(30));
    }

    /**
     * 通过递归查询数据库拼接绝对路径，递归终点为根目录
     * @param directory 起始目录对象
     * @return 目录的绝对路径
     */
    private String buildPath(MinioDirectory directory) {
        LinkedList<String> directoryList = new LinkedList<>();

        // 最多向上追溯 MAX_DIRECTORY_DEPTH 层，避免死循环
        for(int i = 0; i < MAX_DIRECTORY_DEPTH; i++) {
            // 如果当前目录的父目录ID为NULL，则说明已经到达根目录
            String parentDirectoryId = directory.getParentDirectoryId();
            if(parentDirectoryId == null) break;

            directoryList.addFirst(directory.getName()); // 将目录名插入到列表的开头
            directory = directoryMapper.getDirectory(UUID.fromString(parentDirectoryId)); // 获取父目录对象
        }

        return "/" + String.join("/", directoryList) + "/"; // 目录路径以 "/" 结尾
    }

    /**
     * 获取文件的绝对路径
     * 路径结构: /dir1/.../file
     * @param fileId 文件ID
     * @return 文件的绝对路径
     */
    public Mono<String> getAbsoluteFilePath(UUID fileId) throws FileNotFoundException{
        return Mono.fromCallable(
                () -> {
                    // 获取文件的父目录
                    MinioDirectory directory = fileMapper.getParentDirectory(fileId);

                    // 如果没有父目录，说明文件不存在
                    if(directory == null) {
                        throw new FileNotFoundException(fileId.toString());
                    }

                    String path = buildPath(directory);
                    String fileName = fileMapper.getNameByFileId(fileId);

                    return path + fileName;
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }
 }
