package com.drafire.framework.servlet;

import com.drafire.context.DrafireContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DrafireServlet extends HttpServlet {

    private static final String LOCATION="contextConfigLocation";

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
        DrafireContext context=new DrafireContext(config.getInitParameter(LOCATION));

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

}
