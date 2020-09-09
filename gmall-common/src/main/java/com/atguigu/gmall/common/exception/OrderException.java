package com.atguigu.gmall.common.exception;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/16:45
 * @Description: TODO
 */
public class OrderException extends RuntimeException {
    public OrderException() {
    }

    public OrderException(String message) {
        super(message);
    }
}
