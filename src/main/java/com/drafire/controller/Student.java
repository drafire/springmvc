package com.drafire.controller;

import com.drafire.framework.annotation.DrafireRequestMapping;

public class Student {

    @DrafireRequestMapping(value = "/say")   //就算加上RequestMapping 也没用，因为该类没有声明为Controller，也不能被外界访问
    public void say(){
    }
}
