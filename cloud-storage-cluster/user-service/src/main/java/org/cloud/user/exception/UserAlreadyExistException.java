package org.cloud.user.exception;

public class UserAlreadyExistException extends RuntimeException{
    public UserAlreadyExistException(String email) {
        super("邮箱 " + email + " 已注册");
    }
}
