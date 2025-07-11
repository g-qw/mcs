package org.cloud.fs.exception;

public class RootDirectoryDeletionException extends RuntimeException{
    public RootDirectoryDeletionException(String userId, String directoryId) {
        super("权限不足, 删除用户 " + userId + " 的根目录 " + directoryId + " 被拒绝");
    }
}
