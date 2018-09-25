package com.drafire.context;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DrafireContext {
    //声明一个map，也就是IOC容器
    private Map<String,Object> map=new ConcurrentHashMap<String, Object>();

    private List<String> classCache = new ArrayList<String>();

    //声明一个读取propertie 的类
    private Properties config=new Properties();

    public DrafireContext(String location) {
        InputStream is;

        try {
            //1、定位
            is=this.getClass().getClassLoader().getResourceAsStream(location);
            //2、加载
            config.load(is);
            //3、注册--只读取class，spring中还会有读取配置的bean
            String packageName=config.getProperty("scanPackage");
            doRegister(packageName);
            //4、初始化
            doCreateBean();
            //5、注入
            populate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("IOC容器已经初始化");
    }

    private void doRegister(String packageName) {

    }

    private void doCreateBean() {
    }

    private void populate() {
    }
}
