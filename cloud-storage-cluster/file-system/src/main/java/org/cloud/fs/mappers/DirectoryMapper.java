package org.cloud.fs.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.cloud.fs.model.MinioDirectory;

import java.util.List;
import java.util.UUID;

@Mapper
public interface DirectoryMapper {
    /**
     * 创建目录
     * @param parentDirectoryId 父目录ID
     * @param userId 用户ID
     * @param name 目录名称
     * @return 受影响的行数
     */
    int createDirectory(@Param("directoryId") UUID directoryId,
                        @Param("parentDirectoryId") UUID parentDirectoryId,
                        @Param("userId") UUID userId,
                        @Param("name") String name);

    /**
     * 更新目录的名称
     * @param directoryId 目录ID
     * @param name 目录的新名称
     * @return 受影响的行数
     */
    int updateDirectoryName(@Param("directoryId") UUID directoryId, @Param("name") String name);

    /**
     * 删除目录
     * @param directoryId 目录ID
     * @return 受影响的行数
     */
    int deleteDirectory(@Param("directoryId") UUID directoryId);

    /**
     * 移动目录
     * @param directoryId 被移动的目录ID
     * @param parentDirectoryId 移动到的目录ID
     * @return 受影响的行数
     */
    int moveDirectory(@Param("directoryId") UUID directoryId, @Param("parentDirectoryId") UUID parentDirectoryId);

    /**
     * 检查目录是否存在
     * @param directoryId 目录ID
     * @return 目录是否存在, true 表示存在, false 表示不存在
     */
    Boolean isDirectoryExist(@Param("directoryId") UUID directoryId);

    /**
     * 检查目录是否为空
     * @param directoryId 目录ID
     * @return 目录是否为空，true 表示为空，false 表示不为空
     */
    Boolean isDirectoryEmpty(@Param("directoryId") UUID directoryId);

    /**
     * 查询指定用户的根目录ID
     * @param userId 用户ID
     * @return 根目录ID
     */
    String getRootDirectoryId(@Param("userId") UUID userId);

    /**
     * 根据目录ID查询目录
     * @param directoryId 目录ID
     * @return 目录对象
     */
    MinioDirectory getDirectoryById(@Param("directoryId") UUID directoryId);

    /**
     * 获取文件名称
     * @return 文件名称
     */
    String getDirectoryName(@Param("directoryId") UUID directoryId);

    /**
     * 查询指定目录的父目录对象，常用于构建目录的绝对路径
     * @param directoryId 文件夹ID
     * @return 父目录对象
     */
    MinioDirectory getDirectory(@Param("directoryId") UUID directoryId);

    /**
     * 根据父目录ID和目录名称查询目录ID
     * @param parentDirectoryId 父目录ID
     * @param name 目录名称
     * @return 目录ID
     */
    String getDirectoryId(@Param("parentDirectoryId") UUID parentDirectoryId, @Param("name") String name);

    /**
     * 查询指定目录的所有子目录
     * @param parentDirectoryId 父目录ID
     * @return 子目录列表
     */
    List<MinioDirectory> getDirectoriesByParentDirectoryId(@Param("parentDirectoryId") UUID parentDirectoryId);
}
