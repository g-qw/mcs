package org.cloud.fs.exception;

public class DirectoryAlreadyExistsException extends RuntimeException{
    public DirectoryAlreadyExistsException (String directory) {
        super("目录 " + directory + " 已存在");
    }
}
