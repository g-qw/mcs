package org.cloud.fs.exception;

public class FileNotFoundException extends RuntimeException{
    public FileNotFoundException(String file) {
        super("文件 " + file + " 不存在");
    }
}
