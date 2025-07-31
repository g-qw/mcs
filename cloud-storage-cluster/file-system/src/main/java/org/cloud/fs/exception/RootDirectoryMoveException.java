package org.cloud.fs.exception;

public class RootDirectoryMoveException extends RuntimeException{
    public RootDirectoryMoveException(String directoryId) {
        super("非法操作，根目录 " + directoryId + " 不能被移动至其他目录");
    }
}
