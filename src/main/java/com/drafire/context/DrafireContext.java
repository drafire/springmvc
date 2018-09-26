package com.drafire.context;

import com.drafire.framework.annotation.DrafireAutowire;
import com.drafire.framework.annotation.DrafireController;
import com.drafire.framework.annotation.DrafireService;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DrafireContext {
    //声明一个map，也就是IOC容器
    private Map<String, Object> instanceMap = new ConcurrentHashMap<String, Object>();

    //内部使用，不需要有get方法
    private List<String> classCache = new ArrayList<String>();

    //声明一个读取propertie 的类
    private Properties config = new Properties();

    public DrafireContext(String location) {
        InputStream is;

        try {
            //1、定位
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            //2、加载
            config.load(is);
            //3、注册--只读取class，spring源码中还会有读取配置的bean
            String packageName = config.getProperty("scanPackage");
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
        //查找所有class文件
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        if (url == null) {
            return;
        }
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {  //如果是文件夹
                doRegister(packageName + "." + file.getName());

            } else {
                classCache.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    private void doCreateBean() throws Exception {
        if (this.classCache.isEmpty()) {
            return;
        }
        for (String name : classCache) {
            //反射得到类
            Class<?> clazz = Class.forName(name);
            //添加到map中
            if (clazz.isAnnotationPresent(DrafireController.class)) {
                //转换名字为小写
                String id = lowerFirstChar(name);
                instanceMap.put(id, clazz.newInstance());
            } else if (clazz.isAnnotationPresent(DrafireService.class)) {
                //如果有自定义的name，则取自定义name，否则，获取类自身的名字
                DrafireService service = clazz.getAnnotation(DrafireService.class);
                String id = service.value().trim();
                if (!id.equals("")) {
                    instanceMap.put(id, clazz.newInstance());
                    continue;
                }
                //如果有继承关系，则解决这种继承关系
                Class<?>[] intefaces = clazz.getInterfaces();
                for (Class item : intefaces) {
                    //如果这个类实现了接口，就用接口的类型作为id
                    instanceMap.put(item.getName(), item.newInstance());
                }
            } else {
                continue;
            }
        }
    }

    private void populate() throws IllegalAccessException {
        if (classCache.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
            //把所有的属性都取出来，包括私有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(DrafireAutowire.class)) {
                    continue;
                }

                //取出名字
                DrafireAutowire autowire = field.getAnnotation(DrafireAutowire.class);
                String id = field.getType().getName();
                //如果有设置名字
                if (!"".equalsIgnoreCase(autowire.value())) {
                    id = autowire.value();
                }
                field.setAccessible(true);        //使私有域可以访问

                field.set(entry.getValue(), instanceMap.get(id));    //这里就是赋值的意思，也就是注入
            }
        }
    }

    private String lowerFirstChar(String name) {
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    public Map<String, Object> getInstanceMap() {
        return instanceMap;
    }

    public Properties getConfig() {
        return config;
    }
}
