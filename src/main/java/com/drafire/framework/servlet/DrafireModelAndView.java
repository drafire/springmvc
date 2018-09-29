package com.drafire.framework.servlet;

import java.util.Map;

/**
 * 视图和模型
 */
public class DrafireModelAndView {
    private Map<String,Object> model;
    private String view;

    public DrafireModelAndView(Map<String, Object> model, String view) {
        this.model = model;
        this.view = view;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
