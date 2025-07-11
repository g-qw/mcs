package org.cloud.fs.exception;

public class DirectoryNotFoundException extends RuntimeException{
    public DirectoryNotFoundException(String directoryId) {
        super("目录 " + directoryId + " 不存在");
    }
}
