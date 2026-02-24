package org.cloud.fs.service;
import org.babyfish.jimmer.Page;
import org.cloud.fs.dto.DirectoryInput;
import org.cloud.fs.dto.DirectoryNode;
import org.cloud.fs.dto.DirectorySpecification;
import org.cloud.fs.dto.DirectoryView;
import org.cloud.fs.entity.Directory;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DirectoryService {
    /**
     * 创建根目录
     */
    Directory createRootDirectory(UUID userId);

    /**
     * 获取用户的根目录
     */
    Optional<Directory> getRootDirectory(UUID userId);

    /**
     * 查询指定目录详情（不包含子节点）
     */
    Optional<Directory> getDirectoryById(UUID directoryId, UUID userId);

    /**
     * 查询目录内容
     */
    Optional<DirectoryView> listDirectoryContent(UUID directoryId, UUID userId);

    /**
     * 创建新目录
     */
    Directory createDirectory(DirectoryInput input, UUID userId);

    /**
     * 重命名目录
     */
    boolean renameDirectory(UUID directoryId, String newName, UUID userId);

    /**
     * 批量删除目录(软删除)
     */
    int deleteDirectories(List<UUID> directoryIds, UUID userId);

    /**
     * 批量恢复目录
     */
    int recoverDirectories(List<UUID> directoryIds, UUID userId);

    /**
     * 批量移动目录到新的父目录下
     */
    int moveDirectories(List<UUID> directoryIds, UUID newParentId, UUID userId);

    /**
     * 解析路径
     */
    List<Directory> parsePath(String path, UUID userId);

    /**
     * 获取目录树
     */
    DirectoryNode getDirectoryNode(UUID directoryId, UUID userId);

    /**
     * 分页查询目录
     */
    Page<Directory> queryDirectories(PageRequest pageRequest, DirectorySpecification specification, UUID userId);
}