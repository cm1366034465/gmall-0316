package com.atguigu.gmall.common.exception;

/**
 * @Auther: cfy
 * @Date: 2020/09/04/20:06
 * @Description: TODO
 */
public class UserException extends RuntimeException {
    public UserException() {
    }

    public UserException(String message) {
        super(message);
    }
}
