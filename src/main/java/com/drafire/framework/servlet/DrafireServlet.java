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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DrafireServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";

    //方法集合
    //用list是为了简化数据结构，算是数据结构的内容了。
    private List<Handler> handlerList = new ArrayList<Handler>();

    //<方法,参数> 键值对
    private Map<Handler, HandlerAdapter> handlerAdapterMap = new HashMap<Handler, HandlerAdapter>();

    //视图解释器
    private List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 error,Message:" + Arrays.toString(e.getStackTrace()));
        }
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
                handlerList.add(new Handler(pattern, instance.getValue(), method));

                System.out.println("Mapping：" + regex + " " + method.toString());
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
                        handlerAdapterMap.put(handler, new HandlerAdapter(paramsMap));
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
        //读取文件内容、替换自定义的占位符和内容、生成html输出
        //为避免用户能直接访问模板，一般不放在root目录下
        String template = context.getConfig().getProperty("template");
        String source = this.getClass().getClassLoader().getResource(template).getFile();
        File sourceDir = new File(source);

        for (File file : sourceDir.listFiles()) {
            //把每一个视图的名称和文件添加到ViewResolver中
            //所谓的解释视图，都是读取文件的内容，并替换掉其中的动态数据、占位符等
            //最终生成html输出
            viewResolvers.add(new ViewResolver(file.getName(), file));
        }
    }

    private void initFlashMapManager(DrafireContext context) {
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        //getHandler
        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("not foun url:404");
            return;
        }
        //getHandlerAdapter
        HandlerAdapter handlerAdapter = getHandlerAdapter(handler);
        //handlerAdapter.handle
        DrafireModelAndView mv = handlerAdapter.handle(req, resp, handler);

        //渲染输出
        applyDefaultViewName(resp, mv);
    }

    private Handler getHandler(HttpServletRequest request) {
        if (handlerList.isEmpty()) {
            return null;
        }

        String url = request.getRequestURI();
        //获取根地址
        String contextPath = request.getContextPath();
        //获取路径
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        //循环所有的方法
        for (Handler handler : handlerList) {
            Matcher matcher = handler.pattern.matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }

    private HandlerAdapter getHandlerAdapter(Handler handler) {
        if (handlerAdapterMap == null) {
            return null;
        }
        return handlerAdapterMap.get(handler);
    }

    private void applyDefaultViewName(HttpServletResponse resp, DrafireModelAndView mv) throws IOException {
        if (null == mv) {
            return;
        }
        if (viewResolvers.isEmpty()) {
            return;
        }

        for (ViewResolver resolver : viewResolvers) {
            if (!mv.getView().equals(resolver.viewName)) {
                continue;
            }

            //解释模板
            //输出内容到response
            String result = resolver.parse(mv);
            if (null != result) {
                resp.getWriter().write(result);
                break;
            }
        }
    }

    /**
     * HandlerMapping 定义
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

    private class HandlerAdapter {
        private Map<String, Integer> paramMap;

        public HandlerAdapter(Map<String, Integer> paramMap) {
            this.paramMap = paramMap;
        }

        //执行方法，返回ModelAndView
        public DrafireModelAndView handle(HttpServletRequest request, HttpServletResponse response, Handler handler) throws InvocationTargetException, IllegalAccessException {
            //1、获得所有的参数类型
            Class<?>[] types = handler.method.getParameterTypes();
            Object[] param = new Object[types.length];
            //2、获得请求的url的参数
            Map<String, String[]> paramMap = request.getParameterMap();
            //3、通过反射获得对应的参数值，注意的是，参数只能通过索引赋值
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                if (!this.paramMap.containsKey(entry.getKey())) {
                    continue;
                }
                int index = this.paramMap.get(entry.getKey());
                param[index] = castStringValue(value, types[index]);
            }

            //request和response赋值
            String requestName = HttpServletRequest.class.getName();
            if (this.paramMap.containsKey(requestName)) {
                int reqIndex = this.paramMap.get(requestName);
                param[reqIndex] = request;
            }

            String responseName = HttpServletResponse.class.getName();
            if (this.paramMap.containsKey(responseName)) {
                int respIndex = this.paramMap.get(responseName);
                param[respIndex] = response;
            }

            //是否要返回ModelAndView
            boolean isModelAndView = handler.method.getReturnType() == DrafireModelAndView.class ? true : false;
            //反射执行方法
            Object result = handler.method.invoke(handler.controller, param);
            if (isModelAndView) {
                return (DrafireModelAndView) result;
            }
            return null;
        }

        //将值转换为指定的类型
        private Object castStringValue(String value, Class<?> clazz) {
            if (clazz == String.class) {
                return value;
            } else if (clazz == Integer.class) {
                return Integer.valueOf(value);
            } else if (clazz == int.class) {
                return Integer.valueOf(value).intValue();
            } else {
                return null;
            }
        }
    }

    private class ViewResolver {
        protected String viewName;
        protected File file;

        public ViewResolver(String viewName, File file) {
            this.viewName = viewName;
            this.file = file;
        }

        public String parse(DrafireModelAndView mv) throws IOException {
            StringBuffer stringBuffer = new StringBuffer();
            //声明为只读文件
            RandomAccessFile raf = new RandomAccessFile(this.file, "rw");
            try {
                //模板就是使用正则表达式来替换字符串
                String result;
                while (null != (result = raf.readLine())) {
                    //使用中文会有读取的字节的问题
                    //while (null != (result = new String(raf.readLine().getBytes("ISO-8859-1"), "utf-8"))) {
                    Matcher m = matcher(result);
                    //如果找到匹配的内容，则替换
                    while (m.find()) {
                        for (int i = 1; i <= m.groupCount(); i++) {
                            String paramName = m.group(i);
                            Object paramValue = mv.getModel().get(paramName);
                            if (null == paramValue) {
                                continue;
                            }
                            result = result.replaceAll("@\\{" + paramName + "\\}", paramValue.toString());
                        }
                    }
                    stringBuffer.append(result);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                raf.close();

            }
            return stringBuffer.toString();
        }

        private Matcher matcher(String str) {
            Pattern pattern = Pattern.compile("@\\{(.+?)\\}", Pattern.CASE_INSENSITIVE);
            Matcher m = pattern.matcher(str);
            return m;
        }
    }
}
