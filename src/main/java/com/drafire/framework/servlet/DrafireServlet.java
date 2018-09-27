package com.drafire.framework.servlet;

import com.drafire.context.DrafireContext;
import com.drafire.framework.annotation.DrafireController;
import com.drafire.framework.annotation.DrafireRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DrafireServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";

    private List<Handler> handlerMapping = new ArrayList<Handler>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    //在启动的时候，这个就会回调调用
    @Override
    public void init(ServletConfig config) throws ServletException {
        //先初始化IOC容器
        DrafireContext context = new DrafireContext(config.getInitParameter(LOCATION));

        //请求解析
        initMultipartResolver(context);
        //多语言、国际化
        initLocaleResolver(context);
        //主题View层的
        initThemeResolver(context);

        //============== 重要 ================
        //解析url和Method的关联关系
        initHandlerMappings(context);
        //适配器（匹配的过程）
        initHandlerAdapters(context);
        //============== 重要 ================


        //异常解析
        initHandlerExceptionResolvers(context);
        //视图转发（根据视图名字匹配到一个具体模板）
        initRequestToViewNameTranslator(context);

        //解析模板中的内容（拿到服务器传过来的数据，生成HTML代码）
        initViewResolvers(context);

        initFlashMapManager(context);

        System.out.println("DrafireSpring MVC is init.");
    }

    private void initMultipartResolver(DrafireContext context) {
    }

    private void initLocaleResolver(DrafireContext context) {
    }

    private void initThemeResolver(DrafireContext context) {
    }

    private void initHandlerMappings(DrafireContext context) {
        Map<String, Object> instanceMap = context.getInstanceMap();
        if (instanceMap.isEmpty()) {
            return;
        }

        //1、获得所有Controller 注解的类
        for (Map.Entry<String, Object> instance : instanceMap.entrySet()) {
            Class<?> clazz = instance.getValue().getClass();
            if (!clazz.isAnnotationPresent(DrafireController.class)) {    //没有Controller注解的，跳过
                continue;
            }

            String url = "";
            //1.1、分析Controller 类，如果有RequestMapping注解，则加上url
            if (clazz.isAnnotationPresent(DrafireRequestMapping.class)) {
                DrafireRequestMapping mapping = clazz.getAnnotation(DrafireRequestMapping.class);
                url += mapping.value().replaceAll("/+", "/");
            }
            //2、分析所有的类下的method，如果有mapping注解的，则解释
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (!method.isAnnotationPresent(DrafireRequestMapping.class)) {
                    continue;
                }
                DrafireRequestMapping methodMapping = method.getAnnotation(DrafireRequestMapping.class);
                String regex = (url + methodMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);

                //3、把解释后的handler 添加到一个list里面
                handlerMapping.add(new Handler(pattern, instance, method));

                System.out.println("Mapping" + url + " " + method.toString());
            }
        }

    }

    private void initHandlerAdapters(DrafireContext context) {
    }

    private void initHandlerExceptionResolvers(DrafireContext context) {
    }

    private void initRequestToViewNameTranslator(DrafireContext context) {
    }

    private void initViewResolvers(DrafireContext context) {

    }

    private void initFlashMapManager(DrafireContext context) {
    }

    /**
     * HandlerMapping 定义
     *
     * @author Tom
     */
    private class Handler {

        protected Object controller;
        protected Method method;
        protected Pattern pattern;

        protected Handler(Pattern pattern, Object controller, Method method) {
            this.pattern = pattern;
            this.controller = controller;
            this.method = method;
        }
    }

}
