package com.drafire.framework.servlet;

import com.drafire.context.DrafireContext;
import com.drafire.framework.annotation.DrafireController;
import com.drafire.framework.annotation.DrafireRequestMapping;
import com.drafire.framework.annotation.DrafireRequestParam;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DrafireServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";

    //方法集合
    //用list是为了简化数据结构，算是数据结构的内容了。
    private List<Handler> handlerList = new ArrayList<Handler>();

    //<方法,参数> 键值对
    private Map<Handler,HandlerAdapter> handlerAdapterMap=new HashMap<Handler, HandlerAdapter>();

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
                handlerList.add(new Handler(pattern, instance, method));

                System.out.println("Mapping" + url + " " + method.toString());
            }
        }

    }

    private void initHandlerAdapters(DrafireContext context) {
        //所谓的适配，就是解释方法中对应的参数
        //1、判断方法是否为空
        if (handlerList.isEmpty()) {
            return;
        }
        //2、定义一个Map<String,Integer> ，其中String 存储的是参数名字，Integer 存储的是参数索引
        Map<String, Integer> paramsMap = new HashMap<String, Integer>();
        //3、解释参数
        for (Handler handler : handlerList) {
            //3.0、先获取到所有的参数类型
            Class<?>[] parameterTypes = handler.method.getParameterTypes();
            //3.1、如果是request和response，则直接添加到map里面
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == HttpServletRequest.class || parameterTypes[i] == HttpServletResponse.class) {
                    paramsMap.put(parameterTypes[i].getName(), i);
                }
            }

            //3.2、如果是其他，则获取对应的annotation的value作为名字。问题来了，如果是没有annotation呢?(听说spring中都会加上这个)
            //这里之所以是一个二维数组，是以为参数名字有相同的情况，但是索引能够解决这个问题
            Annotation[][] annotations = handler.method.getParameterAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                for (Annotation annotation : annotations[i]) {
                    //如果是参数注解，则解释
                    if (annotation instanceof DrafireRequestParam) {
                        String paramName = ((DrafireRequestParam) annotation).value();
                        if (!"".equals(paramName)) {
                            paramsMap.put(paramName, i);
                        }
                        handlerAdapterMap.put(handler,new HandlerAdapter(paramsMap));
                    }
                }
            }

        }
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

    private class HandlerAdapter{
        private Map<String, Integer> paramMap;

        public HandlerAdapter(Map<String, Integer> paramMap) {
            this.paramMap = paramMap;
        }
    }

}
