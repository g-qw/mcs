package org.cloud.fs.exception;

public class FilePathException extends RuntimeException {
    public FilePathException(String path) {
        super("路径错误，无法找到文件：" + path);
    }
}
