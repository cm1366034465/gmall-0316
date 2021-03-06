package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/09/01/16:05
 * @Description: TODO
 */
@Controller
public class IndexController {
    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model) {
        List<CategoryEntity> categories = this.indexService.querylvl1Categories();
        model.addAttribute("categories", categories);
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubByPid(@PathVariable("pid") Long pid) {
        List<CategoryEntity> categoryEntities = this.indexService.queryCategoriesWithSubByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/testLock")
    @ResponseBody
    public ResponseVo<Object> testLock(){
        indexService.testLock();
        return ResponseVo.ok(null);
    }

    @GetMapping("index/read")
    @ResponseBody
    public ResponseVo<String> read(){
        String msg = indexService.readLock();
        return ResponseVo.ok(msg);
    }

    @GetMapping("index/write")
    @ResponseBody
    public ResponseVo<String> write(){
        String msg = indexService.writeLock();
        return ResponseVo.ok(msg);
    }

    /**
     * 等待
     * @return
     */
    @GetMapping("index/latch")
    @ResponseBody
    public ResponseVo<Object> countDownLatch(){

        String msg = indexService.latch();

        return ResponseVo.ok(msg);
    }

    /**
     * 计数
     * @return
     */
    @GetMapping("index/out")
    @ResponseBody
    public ResponseVo<Object> out(){

        String msg = indexService.countDown();

        return ResponseVo.ok(msg);
    }
}
