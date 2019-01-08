package com.xzy.demo.controller;

import com.xzy.demo.service.DemoService;
import com.xzy.framework.annotation.MyAutowrited;
import com.xzy.framework.annotation.MyController;
import com.xzy.framework.annotation.MyRequestMapping;
import com.xzy.framework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by xzy on 19/1/8  .
 */

@MyController
@MyRequestMapping("/demo")
public class DemoController {

    @MyAutowrited
    private DemoService demoService;

    @MyRequestMapping("/query")
    public void demo(HttpServletRequest req, HttpServletResponse rep,
                     @MyRequestParam("name") String name) {
        String res = demoService.get(name);
        try {
            rep.getWriter().write(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
