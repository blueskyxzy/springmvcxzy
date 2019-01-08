package com.xzy.demo.service.impl;

import com.xzy.demo.service.DemoService;
import com.xzy.framework.annotation.MyService;

/**
 * Created by xzy on 19/1/8  .
 */

@MyService
public class DemoServiceImpl implements DemoService{

    public String get(String name) {
        return "My name is " + name;
    }
}
