package com.bsuir.WarehouseManagementSystem.controller;


import com.bsuir.WarehouseManagementSystem.model.Product;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class MainController {

    @GetMapping("/")
    public String main(Map<String,Object> model){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = String.valueOf(auth.getAuthorities());

        if(role.equals("[ADMIN]")){
            return "main";
        }
        else if(role.equals("[CHECKMAN]")){
            return "redirect:/getProducts";
        }

        return "userOrdersList";
    }
}
