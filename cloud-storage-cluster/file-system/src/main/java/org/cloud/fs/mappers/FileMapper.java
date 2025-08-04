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
     * 查询指定目录下是否存在特定名称的文件
     */
    Boolean isFileExist(@Param("directoryId") UUID directoryId, @Param("objectName") String objectName);

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
     * 获取文件的父目录
     * @param fileId 文件ID
     * @return 父目录对象
     */
    MinioDirectory getParentDirectory(@Param("fileId") UUID fileId);

    /**
     * 查询指定文件夹ID下的所有文件
     * @param directoryId 文件夹ID
     * @return 文件对象列表
     */
    List<MinioFile> getFilesByDirectoryId(@Param("directoryId") UUID directoryId);

    /**
     * 统计文件夹下的文件总数
     */
    long countFilesByDirectoryId(@Param("directoryId") UUID directoryId);

    /**
     * 分页查询指定文件夹ID下的20个文件
     */
    List<MinioFile> getFileByDirectoryIdPaged(@Param("directoryId") UUID directoryId,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

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
     * 根据文件ID查询文件所属目录ID
     * @param fileId 文件ID
     * @return 父目录ID
     */
    String getDirectoryIdByFileId(@Param("fileId") UUID fileId);

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

    /**
     * 查询指定文件ID列表的所有文件的总字节大小
     * @param fileIds 文件ID列表
     * @return 对应的所有文件的总大小，单位字节(byte)
     */
    Long getTotalSizeByFileIds(@Param("fileIds") List<String> fileIds);
}
