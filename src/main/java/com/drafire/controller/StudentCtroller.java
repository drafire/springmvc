package com.drafire.controller;

import com.drafire.framework.annotation.DrafireController;
import com.drafire.framework.annotation.DrafireRequestMapping;

@DrafireController
@DrafireRequestMapping(value = "/student")
public class StudentCtroller {

    @DrafireRequestMapping("/say")
    public void say() {
        System.out.println("hhh");
    }

    public int eat(){
        return 0;
    }
}
