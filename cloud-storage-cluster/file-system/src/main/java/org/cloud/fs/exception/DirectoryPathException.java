package org.cloud.fs.exception;

public class DirectoryPathException extends RuntimeException{
    public DirectoryPathException(String path) {
        super("路径错误，无法找到目录：" + path);
    }
}
