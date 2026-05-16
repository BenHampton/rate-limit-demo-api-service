package com.rate_limit_demo_api_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HelloWorldController {

    @GetMapping(value = "/hello-world", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> helloWorld(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        Map<String, String> response = Map.of(
                "message", "Hello World",
                "client_ip", ip
        );

        return ResponseEntity.ok(response);
    }
}
