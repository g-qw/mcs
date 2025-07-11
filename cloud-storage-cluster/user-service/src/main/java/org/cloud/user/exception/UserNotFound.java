package org.cloud.user.exception;

public class UserNotFound extends RuntimeException{
    public UserNotFound() {
        super("用户不存在");
    }
}
