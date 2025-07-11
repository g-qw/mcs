package org.cloud.fs.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.model.MinioFile;

import java.util.List;
import java.util.UUID;

@Mapper
public interface FileMapper {
    /**
     * 创建文件
     * @param directoryId 目录ID
     * @param userId 用户ID
     * @param objectName 文件名
     * @param size 文件大小
     * @return 受影响的行数
     */
    int createFile(@Param("fileId") UUID fileId,
                   @Param("directoryId") UUID directoryId,
                   @Param("userId") UUID userId,
                   @Param("objectName") String objectName,
                   @Param("mimeType") String mimeType,
                   @Param("size") Long size);

    /**
     * 更新文件
     * @param directoryId 目录ID
     * @param userId 用户ID
     * @param objectName 文件名
     * @param size 文件大小
     * @return 受影响的行数
     */
    int updateFile(@Param("fileId") UUID fileId,
                   @Param("directoryId") UUID directoryId,
                   @Param("userId") UUID userId,
                   @Param("objectName") String objectName,
                   @Param("size") Long size);

    /**
     * 更新文件名
     * @param fileId 文件ID
     * @param objectName 文件名
     * @return 受影响的行数
     */
    int updateFileName(@Param("fileId") UUID fileId, @Param("objectName") String objectName);

    /**
     * 删除文件
     * @param fileId 文件ID
     * @return 受影响的行数
     */
    int deleteFile(@Param("fileId") UUID fileId);

    /**
     * 移动文件
     * @param fileId 文件ID
     * @param directoryId 目录ID
     * @return 受影响的行数
     */
    int moveFile(@Param("fileId") UUID fileId, @Param("directoryId") UUID directoryId);

    /**
     * 检查文件是否存在
     * @param fileId 文件ID
     * @return 文件是否存在, true 表示存在, false 表示不存在
     */
    Boolean isFileExist(@Param("fileId") UUID fileId);

    /**
     * 根据文件ID查询目录ID
     * @param fileId 文件ID
     * @return 目录ID
     */
    String getDirectoryId(@Param("fileId") UUID fileId);

    /**
     * 获取文件的父目录
     * @param fileId 文件ID
     * @return 父目录对象
     */
    MinioDirectory getParentDirectory(@Param("fileId") UUID fileId);

    /**
     * 根据文件ID查询文件
     * @param fileId 文件ID
     * @return 文件对象
     */
    MinioFile getFileById(@Param("fileId") UUID fileId);

    /**
     * 根据目录ID和文件名称查询文件
     * @param directoryId 目录ID
     * @param objectName 文件名称
     * @return 文件对象
     */
    MinioFile getFileByName(@Param("directoryId") UUID directoryId,
                            @Param("objectName") String objectName);

    /**
     * 查询指定文件夹ID下的所有文件
     * @param directoryId 文件夹ID
     * @return 文件列表
     */
    List<MinioFile> getFilesByDirectoryId(@Param("directoryId") UUID directoryId);

    /**
     * 查询指定文件夹ID下的所有文件ID
     * @param directoryId 文件夹ID
     * @return 文件ID列表
     */
    List<String> getFileIdsByDirectoryId(@Param("directoryId") UUID directoryId);

    /**
     * 根据文件ID查询用户ID
     * @param fileId 文件ID
     * @return 用户ID
     */
    String getUserIdByFileId(@Param("fileId") UUID fileId);

    /**
     * 根据文件ID查询文件名
     * @param fileId 文件ID
     * @return 文件名
     */
    String getNameByFileId(@Param("fileId") UUID fileId);

    /**
     * 计算指定用户的所有文件所占用的存储空间
     * @param userId 用户ID
     */
    Long computeUsedStorage(@Param("userId") UUID userId);
}
