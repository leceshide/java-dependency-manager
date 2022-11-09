package com.dependency.manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ViewController {

    @RequestMapping("/")
    public String index(ModelMap map) throws Exception {
        return "dashboard/dependency";
    }

    /**
     * http://127.0.0.1:8080/dependency_list
     */
    @RequestMapping("/dependency/list")
    public String dependencyList(ModelMap map) {
        return "dashboard/dependency_list";
    }
}
