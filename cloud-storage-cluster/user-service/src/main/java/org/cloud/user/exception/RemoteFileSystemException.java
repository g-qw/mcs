package org.cloud.user.exception;

public class RemoteFileSystemException extends RuntimeException{
    public RemoteFileSystemException(int errorCode, String errorMessage) {
        super("远程文件系统服务调用失败，错误码：" + errorCode + ", 错误信息：" + errorMessage);
    }
}
