package org.cloud.fs.service;

import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.model.MinioFile;
import org.cloud.fs.exception.DirectoryNotFoundException;
import org.cloud.fs.exception.FileAlreadyExistsException;
import org.cloud.fs.exception.FileNotFoundException;
import org.cloud.fs.mappers.DirectoryMapper;
import org.cloud.fs.mappers.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class FileService {
    private final FileMapper fileMapper;
    private final DirectoryMapper directoryMapper;

    @Autowired
    public FileService(FileMapper fileMapper, DirectoryMapper directoryMapper) {
        this.fileMapper = fileMapper;
        this.directoryMapper = directoryMapper;
    }

    /**
     * 检查文件是否存在
     * @param fileId 文件ID
     * @return 如果文件存在则返回true, 否则返回false
     */
    public boolean isFileExist(UUID fileId) {
        return fileMapper.isFileExist(fileId);
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
     * 创建文件
     * @param file 文件对象
     * @throws FileAlreadyExistsException 文件已存在
     */
    public String createFile(MinioFile file) throws DirectoryNotFoundException, FileAlreadyExistsException{
        // 检查目录是否存在
        UUID directoryId = UUID.fromString(file.getDirectoryId());
        if(!isDirectoryExist(directoryId)) {
            throw new DirectoryNotFoundException(directoryId.toString());
        }

        // 检查文件是否存在
        MinioFile existFile = fileMapper.getFileByName(directoryId, file.getObjectName());
        if(existFile != null) {
            throw new FileAlreadyExistsException(file.getObjectName());
        }

        // 创建文件
        UUID fileId = UUID.randomUUID();
        fileMapper.createFile(
                fileId,
                directoryId,
                UUID.fromString(file.getUserId()),
                file.getObjectName(),
                file.getMimeType(),
                file.getSize()
        );

        return fileId.toString();
    }

    /**
     * 更新文件，如果文件不存在则默认没有任何操作
     * @param file 文件对象
     */
    public void updateFile(MinioFile file) throws DirectoryNotFoundException{
        // 检查文件目录是否存在
        UUID directoryId = UUID.fromString(file.getDirectoryId());
        if(!isDirectoryExist(directoryId)) {
            throw new DirectoryNotFoundException(directoryId.toString());
        }

        fileMapper.updateFile(
                UUID.fromString(file.getFileId()),
                directoryId,
                UUID.fromString(file.getUserId()),
                file.getObjectName(),
                file.getSize()
        );
    }

    /**
     * 更新文件名
     * @param fileId 文件ID
     * @param objectName 文件名
     * @return 受影响的行数
     */
    public int updateFileName(UUID fileId, String objectName) {
        return fileMapper.updateFileName(fileId, objectName);
    }

    /**
     * 删除文件, 如果文件不存在则默认没有任何操作
     * @param fileId 文件ID
     */
    public void deleteFile(UUID fileId) throws FileNotFoundException{
        if(!isFileExist(fileId)) {
            throw new FileNotFoundException(fileId.toString());
        }

        fileMapper.deleteFile(fileId);
    }

    /**
     * 移动文件，如果文件或目录不存在则默认没有任何操作
     * @param fileId 文件ID
     * @param directoryId 目录ID
     * @throws FileNotFoundException 文件不存在
     * @throws DirectoryNotFoundException 目录不存在
     */
    public void moveFile(UUID fileId, UUID directoryId) throws FileNotFoundException, DirectoryNotFoundException{
        // 检查文件是否存在
        if(!isFileExist(fileId)) {
            throw new FileNotFoundException(fileId.toString());
        }

        // 检查目录是否存在
        if(!isDirectoryExist(directoryId)) {
            throw new DirectoryNotFoundException(directoryId.toString());
        }

        // 移动文件
        fileMapper.moveFile(fileId, directoryId);
    }

    /**
     * 根据文件ID查询文件
     * @param fileId 文件ID
     * @return 文件对象
     */
    public MinioFile getFileById(UUID fileId) {
        return fileMapper.getFileById(fileId);
    }

    /**
     * 根据文件ID查询文件名
     * @param fileId 文件ID
     * @return 文件名
     */
    public String getFileName(UUID fileId) {
        return fileMapper.getNameByFileId(fileId);
    }

    /**
     * 获取文件的父目录
     * @param fileId 文件ID
     * @return 父目录对象
     * @throws FileNotFoundException 文件不存在
     */
    public MinioDirectory getParentDirectory(UUID fileId) throws FileNotFoundException{
        if(!isFileExist(fileId)) {
            throw new FileNotFoundException(fileId.toString());
        }

        UUID directoryId = UUID.fromString(fileMapper.getDirectoryId(fileId));

        return directoryMapper.getDirectoryById(directoryId);
    }

    /**
     *  获取指定目录下的所有文件
     *  @param directoryId 目录ID
     *  @throws DirectoryNotFoundException 目录不存在
     */
    public List<MinioFile> getFilesByDirectoryId(UUID directoryId){
        return fileMapper.getFilesByDirectoryId(directoryId);
    }

    /**
     *  获取指定目录下的所有文件ID
     *  @param directoryId 目录ID
     *  @throws DirectoryNotFoundException 目录不存在
     */
    public List<String> getFileIdsByDirectoryId(UUID directoryId) throws DirectoryNotFoundException{
        if(!isDirectoryExist(directoryId)) {
            throw new DirectoryNotFoundException(directoryId.toString());
        }

        return fileMapper.getFileIdsByDirectoryId(directoryId);
    }

    /**
     * 根据文件ID查询用户ID
     * @param fileId 文件ID
     * @return 用户ID
     */
    public String getUserIdByFileId(UUID fileId) {
        return fileMapper.getUserIdByFileId(fileId);
    }
}
