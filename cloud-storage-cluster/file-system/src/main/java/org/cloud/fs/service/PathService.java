package org.cloud.fs.service;

import org.cloud.fs.exception.DirectoryNotFoundException;
import org.cloud.fs.model.MinioDirectory;
import org.cloud.fs.model.MinioFile;
import org.cloud.fs.exception.DirectoryPathException;
import org.cloud.fs.exception.FileNotFoundException;
import org.cloud.fs.exception.FilePathException;
import org.cloud.fs.mappers.DirectoryMapper;
import org.cloud.fs.mappers.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
public class PathService {
    private final FileMapper fileMapper;
    private final DirectoryMapper directoryMapper;

    // 最大的目录深度
    private final int MAX_DIRECTORY_DEPTH = 32;

    @Autowired
    public PathService(FileMapper fileMapper, DirectoryMapper directoryMapper) {
        this.fileMapper = fileMapper;
        this.directoryMapper = directoryMapper;
    }

    /**
     * 获取文件的绝对路径
     * 路径结构: /dir1/.../file
     * @param fileId 文件ID
     * @return 文件的绝对路径
     */
    public String getAbsoluteFilePath(UUID fileId) throws FileNotFoundException{
        // 获取文件的父目录
        MinioDirectory directory = fileMapper.getParentDirectory(fileId);

        // 如果没有父目录，说明文件不存在
        if(directory == null) {
            throw new FileNotFoundException(fileId.toString());
        }

        // 从文件ID开始向上查找目录，直到根目录
        List<String> directoryList = new LinkedList<>(); // 使用链表，因为需要头插法
        directoryList.addFirst(fileMapper.getNameByFileId(fileId)); // 将文件名插入到列表的开头

        for(int i = 0;i < MAX_DIRECTORY_DEPTH;i++){  // 最大的目录深度为 32, 防止死循环
            // 如果当前目录的父目录ID为NULL，则说明已经到达根目录
            String parentDirectoryId = directory.getParentDirectoryId();
            if(parentDirectoryId == null) break;

            directoryList.addFirst(directory.getName()); // 将目录名插入到列表的开头
            directory = directoryMapper.getDirectory(UUID.fromString(parentDirectoryId)); // 获取父目录对象
        }

        return "/" + String.join("/", directoryList);
    }

    /**
     * 获取目录的绝对路径
     * 路径结构: /dir1/.../
     * @param directoryId 目录ID
     * @return 目录的绝对路径
     */
    public String getAbsoluteDirectoryPath(UUID directoryId) throws DirectoryNotFoundException {
        // 获取目录
        MinioDirectory directory = directoryMapper.getDirectoryById(directoryId);

        if(directory == null) {
            throw new DirectoryNotFoundException(directoryId.toString());
        }

        if(directory.getName().equals("/")) { // 根目录
            return "/";
        }

        // 将非根目录的目录名称插入到列表中
        List<String> directoryList = new LinkedList<>(); // 使用链表，因为需要头插法
        for(int i = 0;i < MAX_DIRECTORY_DEPTH;i++){  // 最大的目录深度为 32, 防止死循环
            // 如果当前目录的父目录ID为NULL，则说明已经到达根目录
            String parentDirectoryId = directory.getParentDirectoryId();
            if(parentDirectoryId == null) break;

            directoryList.addFirst(directory.getName()); // 将目录名插入到列表的开头
            directory = directoryMapper.getDirectory(UUID.fromString(parentDirectoryId)); // 获取父目录对象
        }

        return "/" + String.join("/", directoryList)  + "/";// 目录的路径以 "/" 结尾
    }

    /**
     * 判断路径是否为目录
     * 目录的路径以"/"结尾
     * @param path 路径
     * @return 如果路径为目录则返回true, 否则返回false
     */
    public boolean isDirectory(String path) {
        return path.endsWith("/");
    }

    /**
     * 判断路径是否为文件
     * 文件的路径不以"/"结尾
     * @param path 路径
     * @return 如果路径为文件则返回true, 否则返回false
     */
    public boolean isFile(String path) {
        return !path.endsWith("/");
    }

    /**
     * 根据路径获取文件
     * @param path 绝对路径
     * @param userId 用户ID
     * @return 文件对象
     * @throws FilePathException 文件的路径错误
     */
    public MinioFile getFile(String path, UUID userId) throws FilePathException {
        if(!isFile(path)) {
            throw new FilePathException(path);
        }

        /*
          获取目录名称列表
          例如: /dir1/dir2/dir3/file 的目录名称列表为 [dir1, dir2, dir3], 文件名称 file
         */
        String[] parts = path.split("/");

        // 获取指定用户的根目录
        MinioDirectory directory = directoryMapper.getRootDirectory(userId);
        String directoryId = directory.getDirectoryId();
        for(int i = 1; i < parts.length - 1; i++) {
            directoryId = directoryMapper.getDirectoryID(UUID.fromString(directoryId), parts[i]);
        }

        return fileMapper.getFileByName(UUID.fromString(directoryId), parts[parts.length - 1]);
    }

    /**
     * 根据路径获取目录
     * @param path 绝对路径
     * @param userId 用户ID
     * @return 目录对象
     * @throws DirectoryPathException 目录的路径错误
     */
    public MinioDirectory getDirectory(String path, UUID userId) throws DirectoryPathException{
        if(!isDirectory(path)) {
            throw new DirectoryPathException(path);
        }

        if(path.equals("/")) {
            return directoryMapper.getRootDirectory(userId);// 如果路径为"/"，则返回用户的根目录
        }

        /*
         * 获取目录名称列表
         * 例如: /dir1/dir2/dir3/ 的目录名称列表为 [dir1, dir2, dir3]
         */
        String[] parts = path.split("/");

        MinioDirectory directory = directoryMapper.getRootDirectory(userId);
        String directoryId = directory.getDirectoryId();
        for(int i = 1; i <= parts.length - 1; i++) {
            directoryId = directoryMapper.getDirectoryID(UUID.fromString(directoryId), parts[i]);
        }

        return directoryMapper.getDirectoryById(UUID.fromString(directoryId));
    }
 }
