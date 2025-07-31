package org.cloud.fs.service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.cloud.fs.model.MinioFile;
import org.cloud.fs.exception.DirectoryNotFoundException;
import org.cloud.fs.exception.FileAlreadyExistsException;
import org.cloud.fs.exception.FileNotFoundException;
import org.cloud.fs.mappers.DirectoryMapper;
import org.cloud.fs.mappers.FileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
@Transactional(rollbackFor = Exception.class)
public class FileService {
    private final static Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileMapper fileMapper;
    private final DirectoryMapper directoryMapper;
    private final MinioClient minioClient;
    private final PathService pathService;
    private final Executor executor;

    @Autowired
    public FileService(FileMapper fileMapper, DirectoryMapper directoryMapper, MinioClient minioClient, PathService pathService, @Qualifier("sharedExecutor") Executor executor) {
        this.fileMapper = fileMapper;
        this.directoryMapper = directoryMapper;
        this.minioClient = minioClient;
        this.pathService = pathService;
        this.executor = executor;
    }

    /**
     * 创建文件
     * @param file 文件对象, 需要包含 directoryId、userId、objectName 参数
     * @return 文件ID
     * @throws DirectoryNotFoundException 目录不存在
     * @throws FileAlreadyExistsException 文件已存在
     */
    public Mono<String> createFile(MinioFile file) throws DirectoryNotFoundException, FileAlreadyExistsException{
        UUID directoryId = UUID.fromString(file.getDirectoryId());
        UUID userId = UUID.fromString(file.getUserId());
        String objectName  = file.getObjectName();

        Mono<Boolean> dirExists = Mono.fromCallable(
                () -> directoryMapper.isDirectoryExist(directoryId)
        ).subscribeOn(Schedulers.fromExecutor(executor));

        Mono<Boolean> fileExists = Mono.fromCallable(
                () -> fileMapper.isFileExist(directoryId, objectName)
        ).subscribeOn(Schedulers.fromExecutor(executor));

        Mono<String> doInsert = Mono.fromCallable(
                () -> {
                    UUID fileId = UUID.randomUUID();
                    fileMapper.createFile(
                            fileId,
                            directoryId,
                            userId,
                            objectName,
                            file.getMimeType(),
                            file.getSize()
                    );
                    logger.info("用户 {} 创建文件 {} 成功.", file.getUserId(), file.getObjectName());

                    return fileId.toString();
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));

        /* 查目录 & 文件 -> 校验 -> 插入 */
        return dirExists
            .flatMap(exists -> exists
                    ? fileExists
                    : Mono.error(new DirectoryNotFoundException(directoryId.toString())))
            .flatMap(exists -> exists
                    ? Mono.error(new FileAlreadyExistsException(objectName))
                    : doInsert
            );
    }

    /**
     * 删除文件, 如果文件不存在则默认没有任何操作
     * @param fileId 文件ID
     * @throws FileNotFoundException 文件不存在
     */
    public Mono<Boolean> deleteFile(UUID fileId) throws FileNotFoundException{
        // 查询桶
        Mono<String> bucketMono = Mono.fromCallable(
                () -> {
                    String bucket = fileMapper.getUserIdByFileId(fileId);
                    if(bucket == null) throw new FileNotFoundException(fileId.toString());

                    return bucket;
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));

        // 查询文件的绝对路径
        Mono<String> absolutePathMono = pathService.getAbsoluteFilePath(fileId);

        // 组装 minio 删除参数
        Mono<RemoveObjectArgs> removeObjectArgsMono = bucketMono.zipWith(absolutePathMono)
            .map(
                    tuple -> RemoveObjectArgs.builder()
                            .bucket(tuple.getT1())
                            .object(tuple.getT2())
                            .build()
            );

        // 删除数据库中的文件记录
        Mono<Integer> deleteFromDB = Mono.fromCallable(
                () -> {
                    logger.info("删除文件 {} 成功.", fileId);
                    return fileMapper.deleteFile(fileId);
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));

        // 删除 minio 文件
        return removeObjectArgsMono.flatMap(
                removeObjectArgs -> Mono.fromCallable(
                        () -> {
                            minioClient.removeObject(removeObjectArgs); // 删除 minio 中的文件
                            return true;
                        }
                ).flatMap(
                        deleteSuccess -> deleteFromDB.thenReturn(true)
                )
        );
    }

    /**
     *  获取指定目录下的所有文件
     *  @param directoryId 目录ID
     *  @throws DirectoryNotFoundException 目录不存在
     */
    public Mono<List<MinioFile>> getFiles(UUID directoryId){
        return Mono.fromCallable(
                () -> fileMapper.getFilesByDirectoryId(directoryId)
        ).subscribeOn(Schedulers.fromExecutor(executor));
    }

    /**
     * 移动文件，如果文件或目录不存在则默认没有任何操作
     * @param fileId 文件ID
     * @param directoryId 目录ID
     * @throws FileNotFoundException 文件不存在
     * @throws DirectoryNotFoundException 目录不存在
     * @return 操作是否成功，移动文件成功返回 true，否则 false
     */
    public Mono<Boolean> moveFile(UUID fileId, UUID directoryId) throws FileNotFoundException, DirectoryNotFoundException{
        // 如果移动至的目录与文件所在的原目录相同，则不进行任何操作
        String location = fileMapper.getDirectoryIdByFileId(fileId);
        if(location.equals(directoryId.toString())) {
            return Mono.just(true);
        }

        // 移动前的文件路径
        Mono<String> originPathMono = pathService.getAbsoluteFilePath(fileId);

        // 移动至的路径位置
        Mono<String> directoryPathMono = pathService.getAbsoluteDirectoryPath(directoryId);

        // 文件名
        Mono<String> fileNameMono = Mono.fromCallable(
                () -> fileMapper.getNameByFileId(fileId)
        ).subscribeOn(Schedulers.fromExecutor(executor));

        // minio 存储桶
        Mono<String> bucketMono = Mono.fromCallable(
                () -> fileMapper.getUserIdByFileId(fileId)
        ).subscribeOn(Schedulers.fromExecutor(executor));

        // 移动文件
        Mono<Integer> moveFile = Mono.fromCallable(
                () -> {
                    logger.info("移动文件 {} 到 {} 成功.", fileId, directoryId);
                    return fileMapper.moveFile(fileId, directoryId);
                }
        ).subscribeOn(Schedulers.fromExecutor(executor));

        return Mono.zip(
            originPathMono, directoryPathMono, fileNameMono, bucketMono
        ).flatMap(
            tuple -> {
                String originPath = tuple.getT1();
                String targetPath = tuple.getT2() + tuple.getT3();
                String bucket = tuple.getT4();

                return Mono.fromCallable(
                    // 复制文件到新位置，如果出现异常则整个链直接失败，不会执行删除原文加操作
                    () -> {
                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(bucket)
                                        .object(targetPath)
                                        .source(CopySource.builder()
                                                .bucket(bucket)
                                                .object(originPath)
                                                .build())
                                        .build()
                        );
                        return true;
                    }
                ).flatMap(
                    // 删除原文件，如果发生失败则需要回滚复制文件操作
                    copySuccess -> Mono.fromCallable(
                        () -> {
                            minioClient.removeObject(
                                    RemoveObjectArgs.builder()
                                            .bucket(bucket)
                                            .object(originPath)
                                            .build()
                            );
                            return true;
                        }
                    ).onErrorResume(
                        // 处理删除操作的回滚，如果删除原文件失败，则删除复制到目标目录的文件
                        deleteError -> Mono.fromCallable(
                                () -> {
                                    minioClient.removeObject(
                                            RemoveObjectArgs.builder()
                                                    .bucket(bucket)
                                                    .object(targetPath)
                                                    .build()
                                    );

                                    return false;
                                }
                        )
                    )
                ).flatMap(
                    // 如果 minio 中移动文件操作成功，则更新文件系统的数据，执行移动文件的数据库操作
                    deleteSuccess -> {
                        if(Boolean.TRUE.equals(deleteSuccess)) {
                            return moveFile.thenReturn(true);
                        } else {
                            return Mono.just(false);
                        }
                    }
                );
            }
        );
    }
}
