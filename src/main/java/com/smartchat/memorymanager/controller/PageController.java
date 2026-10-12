package com.smartchat.memorymanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serve the dedicated subfolder pages without depending on directory index resolution. */
@Controller
public class PageController {

    @GetMapping({"/chat", "/chat/"})
    public String chat() {
        return "forward:/chat/index.html";
    }

    @GetMapping({"/backend-explainer", "/backend-explainer/"})
    public String explainer() {
        return "forward:/backend-explainer/index.html";
    }
}
