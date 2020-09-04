package com.atguigu.gmall.item;

import rx.Completable;

import java.util.concurrent.CompletableFuture;

/**
 * @Auther: cfy
 * @Date: 2020/09/03/16:02
 * @Description: TODO
 */
public class CompletableFutureDemo {
    public static void main(String[] args) {
        // test01();

        // 串行化方法
        CompletableFuture.supplyAsync(() -> {
            System.out.println("处理子任务......supplyAsync方法");
            //int i = 1 / 0;
            return "hello CompletableFuture";
        }).thenApplyAsync(t -> {
            System.out.println("串行，获取上一个结果" + t);
            return "hello thenapply";
        }).thenAcceptAsync(t -> {
            // 没有自己的返回结果
            System.out.println("hello accept");
        }).thenRunAsync(()->{
            System.out.println("then run");
        }).whenCompleteAsync((t, u) -> {
            System.out.println("上一个任务处理完毕，开始处理新任务");
            System.out.println("t = " + t);// 上一个任务返回的结果
            System.out.println("u = " + u);// 上一个任务的异常信息
        });
    }

    private static void test01() {
        CompletableFuture.supplyAsync(() -> {
            System.out.println("处理子任务......supplyAsync方法");
            int i = 1 / 0;
            return "hello CompletableFuture";
        }).whenCompleteAsync((t, u) -> {
            System.out.println("上一个任务处理完毕，开始处理新任务");
            System.out.println("t = " + t);// 上一个任务返回的结果
            System.out.println("u = " + u);// 上一个任务的异常信息
        }).exceptionally(t -> {
            System.out.println("上一个任务出现异常t：" + t);
            return "hello exceptionally";
        });

        CompletableFuture.runAsync(() -> {
            System.out.println("toutouzhixing");
        }).whenComplete((t, u) -> {
            System.out.println("..........");
        });
    }
}
