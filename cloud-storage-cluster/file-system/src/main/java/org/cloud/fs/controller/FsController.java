package org.cloud.fs.controller;

import jakarta.validation.constraints.NotBlank;
import org.cloud.fs.dto.*;
import org.cloud.fs.exception.*;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.model.MinioFile;
import org.cloud.fs.service.DirectoryService;
import org.cloud.fs.service.FileService;
import org.cloud.fs.service.PathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@Validated
public class FsController {
    private final DirectoryService directoryService;
    private final FileService fileService;
    private final PathService pathService;

    @Autowired
    public FsController(DirectoryService directoryService,
                        FileService fileService,
                        PathService pathService) {
        this.directoryService = directoryService;
        this.fileService = fileService;
        this.pathService = pathService;
    }

    /* ======================================================== 目录相关的接口 ======================================================== */

    /**
     * 创建根目录
     * @apiNote 如果根目录已经存在则返回已存在的根目录
     * @param userId 用户ID，URL 查询参数
     * @return 根目录ID
     */
    @PostMapping("/root_dir")
    public Mono<ApiResponse<String>> createRootDirectory(@RequestParam("userId") @NotBlank(message = "用户ID不能为空") String userId) {
        return directoryService.createRootDirectory(userId)
                .map(ApiResponse::success)
                .onErrorResume(
                        Exception.class,
                        e -> Mono.just(ApiResponse.failure(500, "内部服务器错误"))
                );
    }

    /**
     * 获取根目录对象
     * @param userId 用户ID，URL 查询参数
     * @return 根目录对象
     */
    @GetMapping("/get_root_dir")
    public Mono<ApiResponse<MinioDirectory>> getRootDirectory(@RequestParam("userId") @NotBlank(message = "用户ID不能为空") String userId) {
        return directoryService.getRootDirectory(UUID.fromString(userId))
                .map(ApiResponse::success)
                .onErrorResume(IllegalArgumentException.class, e -> Mono.just(ApiResponse.failure(400, e.getMessage())));
    }

    /**
     * 创建目录
     * @param directory 目录对象，JSON 请求体中需要包含 parentDirectoryId、userId、name 参数
     * @return 创建的目录对象
     */
    @PostMapping("/create_dir")
    public Mono<ApiResponse<MinioDirectory>> createDirectory(@RequestBody MinioDirectory directory) {
        return directoryService.createDirectory(directory)
                .map(ApiResponse::success)
                .onErrorResume(DirectoryAlreadyExistsException.class, e -> Mono.just(ApiResponse.failure(500, e.getMessage())));
    }

    /**
     * 获取目录名称
     * @apiNote 用于修改目录名称时如果无法修改则复原旧名称
     * @param directoryId 目录ID，URL 查询参数
     * @return 目录名称
     */
    @GetMapping("/get_dir_name")
    public Mono<ApiResponse<String>> getDirectoryName(@RequestParam("directoryId") @NotBlank(message = "目录ID不能为空") String directoryId) {
        return directoryService.getDirectoryName(UUID.fromString(directoryId))
                .map(ApiResponse::success)
                .onErrorResume(
                        DirectoryNotFoundException.class,
                        e -> Mono.just(ApiResponse.failure(400, e.getMessage()))
                );
    }

    /**
     * 更新目录名称
     * @param directoryId 目录ID, URL 查询参数
     * @param name        新的目录名称, URL 查询参数
     * @return 更新目录的结果
     */
    @PostMapping("/update_dir_name")
    public Mono<ApiResponse<Integer>> updateDirectory(@RequestParam @NotBlank(message = "目录ID不能为空") String directoryId,
                                                      @RequestParam @NotBlank(message = "目录名称不能为空") String name) {
        return directoryService.updateDirectoryName(UUID.fromString(directoryId), name)
                .map(ApiResponse::success)
                .onErrorResume(
                        DuplicateKeyException.class,
                        e -> Mono.just(ApiResponse.failure(400, "目录已存在"))
                );
    }

    /**
     * 删除目录
     * @apiNote 仅允许删除空目录
     * @param directoryId 目录ID, URL 查询参数
     * @return 删除目录的结果
     */
    @PostMapping("/delete_dir")
    public Mono<ApiResponse<String>> deleteDirectory(@RequestParam("directoryId") @NotBlank(message = "目录ID不能为空") String directoryId) {
        return directoryService.deleteDirectory(UUID.fromString(directoryId))
                .thenReturn(ApiResponse.success("true"))
                .onErrorResume(
                        DirectoryNotEmptyException.class,
                        e ->  Mono.just(ApiResponse.failure(409, "无法删除不为空的目录，请先删除目录下的所有文件和目录"))
                );
    }

    /**
     * 加载目录的数据
     * @apiNote 加载指定目录下的所有文件夹、文件
     * @param directoryId 目录ID, URL 查询参数
     * @return 目录的数据
     */
    @PostMapping("/load_dir")
    public Mono<ApiResponse<MinioDirContent>> loadDirContent(@RequestParam("directoryId") @NotBlank(message = "目录ID不能为空") String directoryId) {
        UUID id = UUID.fromString(directoryId);

        return Mono.zip(
                pathService.getAbsoluteDirectoryPath(id),
                directoryService.getChildDirectories(id),
                fileService.getFiles(id)
        ).map(
                tuple -> {
                    String path = tuple.getT1();
                    List<MinioDirectory> dirs = tuple.getT2();
                    List<MinioFile> files = tuple.getT3();
                    return new MinioDirContent(directoryId, path, dirs, files);
                }
        ).map(ApiResponse::success) // 包装成 ApiResponse 响应
        .onErrorResume(
                DirectoryNotFoundException.class,
                e -> Mono.just(ApiResponse.failure(400, e.getMessage()))
        );
    }

    /**
     * 移动目录
     * @apiNote 仅支持移动空目录，因为考虑到 minio 中移动目录需要进行大量的文件复制，这会造成巨大的性能开销
     * @param directoryId 被移动的目录的目录ID
     * @param parentDirectoryId 移动至的目标目录的目录ID
     * @return 移动目录的结果
     */
    @PostMapping("/move_dir")
    public Mono<ApiResponse<Integer>> moveDirectory(@RequestParam("directoryId") @NotBlank(message = "目录ID不能为空") String directoryId,
                                                    @RequestParam("parentDirectoryId") @NotBlank(message = "目标目录ID不能为空") String parentDirectoryId) {
        return directoryService.moveDirectory(UUID.fromString(directoryId), UUID.fromString(parentDirectoryId))
                .map(ApiResponse::success)
                .onErrorResume(
                        DirectoryNotFoundException.class,
                        e -> Mono.just(ApiResponse.failure(404, e.getMessage()))
                ).onErrorResume(
                        RootDirectoryMoveException.class,
                        e -> Mono.just(ApiResponse.failure(403, e.getMessage()))
                )
                .onErrorResume(
                        DirectoryNotEmptyException.class,
                        e -> Mono.just(ApiResponse.failure(400, e.getMessage()))
                );
    }

    /* ======================================================== 文件相关的接口 ======================================================== */


    /**
     * 创建文件
     * @param file 文件对象, 请求体中需包含 directoryId、userId、objectName 参数， objectName 属性最好包含文件扩展名，否则会被视为 .bin 文件
     * @return 文件ID
     */
    @PostMapping("/add_file")
    public Mono<ApiResponse<String>> addFile(@RequestBody MinioFile file) {
        return fileService.createFile(file)
                .map(ApiResponse::success)
                .onErrorResume(
                        DirectoryNotFoundException.class,
                        e -> Mono.just(ApiResponse.failure(404, e.getMessage()))
                ).onErrorResume(
                        FileAlreadyExistsException.class,
                        e -> Mono.just(ApiResponse.failure(400, e.getMessage()))
                );
    }

    /**
     * 删除文件
     * @param fileId 文件ID，URL 查询参数
     */
    @PostMapping("/delete_file")
    public Mono<ApiResponse<Boolean>> deleteFile(@RequestParam("fileId") @NotBlank(message = "文件ID不能为空") String fileId) {
        return fileService.deleteFile(UUID.fromString(fileId))
                .map(ApiResponse::success)
                .onErrorResume(
                        FileNotFoundException.class,
                        e -> Mono.just(ApiResponse.failure(404, e.getMessage()))
                );
    }

    /**
     * 移动文件
     * @param fileId 被移动的文件的文件ID
     * @param directoryId 移动至的目录的目录ID
     */
    @PostMapping("/move_file")
    public Mono<ApiResponse<Boolean>> moveFile(@RequestParam("fileId") @NotBlank(message = "文件ID不能为空") String fileId,
                                               @RequestParam("directoryId") @NotBlank(message = "目标目录ID不能为空") String directoryId) {
        return fileService.moveFile(UUID.fromString(fileId), UUID.fromString(directoryId))
                .map(ApiResponse::success)
                .onErrorResume(
                        e -> e instanceof FileNotFoundException
                                || e instanceof DirectoryNotFoundException,
                        e -> Mono.just(ApiResponse.failure(404, e.getMessage()))
                );
    }
}
