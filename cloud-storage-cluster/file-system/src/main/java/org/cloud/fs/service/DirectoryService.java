package org.cloud.fs.service;

import org.cloud.fs.exception.RootDirectoryDeletionException;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.model.MinioFile;
import org.cloud.fs.exception.DirectoryAlreadyExistsException;
import org.cloud.fs.exception.DirectoryNotEmptyException;
import org.cloud.fs.exception.DirectoryNotFoundException;
import org.cloud.fs.mappers.DirectoryMapper;
import org.cloud.fs.mappers.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DirectoryService {
    private final DirectoryMapper directoryMapper;
    private final FileMapper fileMapper;

    @Autowired
    public DirectoryService(DirectoryMapper directoryMapper, FileMapper fileMapper) {
        this.directoryMapper = directoryMapper;
        this.fileMapper = fileMapper;
    }

    /**
     * 创建用户的根目录
     * 根目录的父目录ID为NULL且名称为"/"
     * @param userId 用户ID
     * @return 根目录ID，如果已存在则返回已有的根目录ID
     */
    public String createRootDirectory(UUID userId) {
        // 查询是否存在根目录
        MinioDirectory rootDirectory = directoryMapper.getRootDirectory(userId);

        if(rootDirectory != null) {
            return rootDirectory.getDirectoryId(); // 返回已有的根目录ID
        }

        // 创建根目录
        directoryMapper.createDirectory(null, userId, "/");

        return directoryMapper.getRootDirectory(userId).getDirectoryId();  // 返回根目录ID
    }


    /**
     * 检查目录是否存在
     * @param directoryId 目录ID
     * @return 如果目录存在则返回true, 否则返回false
     */
    public boolean isDirectoryExist(UUID directoryId) {
        return directoryMapper.isDirectoryExist(directoryId);
    }

    /**
     * 创建目录
     *
     * @param directory 目录对象, 使用 @Validated 校验参数
     * @return 目录对象
     * @throws DirectoryNotFoundException      父目录不存在
     * @throws DirectoryAlreadyExistsException 目录已存在
     */
    public MinioDirectory createDirectory(MinioDirectory directory) throws DirectoryNotFoundException, DirectoryAlreadyExistsException{
        // 检查父目录是否存在
        UUID parentDirectoryId = UUID.fromString(directory.getParentDirectoryId());
        if(!isDirectoryExist(parentDirectoryId)) {
            throw new DirectoryNotFoundException(directory.getParentDirectoryId());
        }

        // 检查父目录下是否已经存在该目录
        String directoryID =  directoryMapper.getDirectoryID(parentDirectoryId, directory.getName());
        if(directoryID != null) {
            throw new DirectoryAlreadyExistsException(directory.getName());
        }

        directoryMapper.createDirectory(parentDirectoryId, UUID.fromString(directory.getUserId()), directory.getName());

        return directoryMapper.getDirectoryByName(parentDirectoryId, directory.getName());
    }

    /**
     * 删除目录
     * @param directoryId 目录ID
     * @throws DirectoryNotFoundException 目录不存在
     * @throws DirectoryNotEmptyException 目录不为空
     */
    public void deleteDirectory(UUID directoryId) throws DirectoryNotFoundException, DirectoryNotEmptyException{
        // 检查目录是否存在
        if(!isDirectoryExist(directoryId)) {
            throw new DirectoryNotFoundException(directoryId.toString());
        }

        // 检查目录下是否存在子目录
        List<MinioDirectory> directories = directoryMapper.getDirectoriesByParentDirectoryId(directoryId);
        if(!directories.isEmpty()) {
            throw new DirectoryNotEmptyException(directoryId.toString());
        }

        // 检查目录下是否存在文件
        List<MinioFile> files = fileMapper.getFilesByDirectoryId(directoryId);
        if(!files.isEmpty()) {
            throw new DirectoryNotEmptyException(directoryId.toString());
        }

        // 检查目录是否为根目录
        MinioDirectory directory =  directoryMapper.getDirectory(directoryId);
        if(directory.getParentDirectoryId() == null) {
            throw new RootDirectoryDeletionException(directory.getUserId(), directory.getDirectoryId());
        }

        // 删除目录
        directoryMapper.deleteDirectory(directoryId);
    }

    /**
     * 更新目录
     * @param directory 目录对象
     * @throws DirectoryNotFoundException 目录不存在
     */
    public void updateDirectory(MinioDirectory directory) throws DirectoryNotFoundException{
        // 检查目录是否存在
        UUID directoryId = UUID.fromString(directory.getDirectoryId());
        if(!isDirectoryExist(directoryId)) {
            throw new DirectoryNotFoundException(directory.getName());
        }

        // 更新目录
        directoryMapper.updateDirectory(directoryId, UUID.fromString(directory.getUserId()), directory.getName());
    }

    /**
     * 更新目录名称
     * @param directoryId 目录ID
     * @param name 目录名称
     */
    public void updateDirectoryName(UUID directoryId, String name) {
        directoryMapper.updateDirectoryName(directoryId, name);
    }

    /**
     * 移动目录
     * @param directoryId 目录ID
     * @param parentDirectoryId 目录ID
     * @throws DirectoryNotFoundException 目录不存在
     */
     public void moveDirectory(UUID directoryId, UUID parentDirectoryId) throws DirectoryNotFoundException{
         // 检查目录是否存在
         if(!isDirectoryExist(directoryId)) {
             throw new DirectoryNotFoundException(directoryId.toString());
         }

         // 检查目标目录是否存在
         if(!directoryMapper.isDirectoryExist(parentDirectoryId)) {
             throw new DirectoryNotFoundException(parentDirectoryId.toString());
         }

         // 移动目录
         directoryMapper.moveDirectory(directoryId, parentDirectoryId);
     }

    /**
     * 根据目录ID查询目录
     * @param directoryId 目录ID
     * @return 目录对象
     * @throws DirectoryNotFoundException 目录不存在
     */
     public MinioDirectory getDirectoryById(UUID directoryId) throws DirectoryNotFoundException{
         if(!isDirectoryExist(directoryId)) {
             throw new DirectoryNotFoundException(directoryId.toString());
         }

         return directoryMapper.getDirectoryById(directoryId);
     }

    /**
     * 根据目录ID查询目录名称
     * @param directoryId 目录ID
     * @return 文件名称
     */
     public String getDirectoryName(UUID directoryId) {
         return directoryMapper.getDirectoryName(directoryId);
     }

    /**
     * 根据父目录ID查询子目录
     * @param parentDirectoryId 父目录ID
     * @return 子目录列表
     * @throws DirectoryNotFoundException 目录不存在
     */
     public List<MinioDirectory> getDirectoriesByParentDirectoryId(UUID parentDirectoryId){
         return directoryMapper.getDirectoriesByParentDirectoryId(parentDirectoryId);
     }

    /**
     * 根据父目录ID查询子目录ID
     * @param parentDirectoryId 父目录ID
     * @return 子目录ID列表
     * @throws DirectoryNotFoundException 目录不存在
     */
     public List<String> getDirectoryIdsByParentDirectoryId(UUID parentDirectoryId) throws DirectoryNotFoundException{
         if(!isDirectoryExist(parentDirectoryId)) {
             throw new DirectoryNotFoundException(parentDirectoryId.toString());
         }

         return directoryMapper.getDirectoryIdsByParentDirectoryId(parentDirectoryId);
     }

    /**
     * 根据目录ID查询用户ID
     * @param directoryId 目录ID
     * @return 用户ID
     */
     public String getUserIdByDirectoryId(UUID directoryId) {
         return directoryMapper.getUserIdByDirectoryId(directoryId);
     }
}
