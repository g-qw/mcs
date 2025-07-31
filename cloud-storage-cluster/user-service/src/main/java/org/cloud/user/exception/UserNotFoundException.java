package org.cloud.user.exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException() {
        super("用户不存在");
    }
}
