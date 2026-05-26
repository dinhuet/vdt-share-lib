package com.pm.be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/central/api/configs")
public class ConfigController {

    @GetMapping("/test")
    public String test() {
        return "nice";
    }
}