package com.atguigu.gmall.scheduled.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/0:30
 * @Description: TODO
 */
@Component
public class MyJobHandler {
    /**
     * 简单任务示例（Bean模式）
     */
    @XxlJob("myJobHandler")
    public ReturnT<String> executor(String param) {
        XxlJobLogger.log("这xxljob任务输出的日志");
        System.out.println("这是我的第一个xxl-job定时任务......" + param);
        return ReturnT.SUCCESS;
    }
}
