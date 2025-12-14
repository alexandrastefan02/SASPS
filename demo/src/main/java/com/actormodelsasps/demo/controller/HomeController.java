package com.actormodelsasps.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/team-chat-client.html";
    }
    
    @GetMapping("/error")
    public String handleError() {
        return "redirect:/team-chat-client.html";
    }
}
