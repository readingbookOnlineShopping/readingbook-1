package com.example.demo.web.controller.view.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/manage")
public class CategoryViewController {

    @GetMapping("/register/category-group")
    public String registerCategoryGroup(Model model){
        model.addAttribute("selectFlag", "registerCategoryGroup");
        return "manage/category/categorygroup-register";
    }

    @GetMapping("/update/category-group")
    public String updateCategoryGroup(Model model){
        model.addAttribute("selectFlag", "updateCategoryGroup");
        return "manage/category/categorygroup-update";
    }

    @GetMapping("/register/category")
    public String registerCategory(Model model){
        model.addAttribute("selectFlag", "registerCategory");
        return "manage/category/category-register";
    }

    @GetMapping("/update/category")
    public String updateCategory(Model model){
        model.addAttribute("selectFlag", "updateCategory");
        return "manage/category/category-update";
    }
}