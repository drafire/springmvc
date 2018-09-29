package com.drafire.controller;

import com.drafire.framework.annotation.DrafireController;
import com.drafire.framework.annotation.DrafireRequestMapping;
import com.drafire.framework.annotation.DrafireRequestParam;
import com.drafire.framework.servlet.DrafireModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@DrafireController
@DrafireRequestMapping(value = "/student")
public class StudentCtroller {

    @DrafireRequestMapping("/say")
    public DrafireModelAndView say(HttpServletRequest request, HttpServletResponse response,
                                   @DrafireRequestParam(value = "name") String name,
                                   @DrafireRequestParam(value = "age") int age) {
        System.out.println("hhh");
        Map<String, Object> model=new HashMap<String, Object>();
        model.put("name",name);
        model.put("age",age);
        return new DrafireModelAndView(model,"DrafireTemplate.pgml");
    }

    @DrafireRequestMapping("/eat")
    public int eat(HttpServletRequest request, HttpServletResponse response){
        return 0;
    }
}
