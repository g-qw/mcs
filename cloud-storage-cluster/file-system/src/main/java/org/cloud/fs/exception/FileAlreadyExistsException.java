package org.cloud.fs.exception;

public class FileAlreadyExistsException extends RuntimeException {
    public FileAlreadyExistsException(String file) {
        super("文件 " + file + " 已存在");
    }
}
