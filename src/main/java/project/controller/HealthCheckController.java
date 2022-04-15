package project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@CrossOrigin("*")
@RestController
@RequestMapping("/ping")
public class HealthCheckController {

    @GetMapping
    public String healthCheck() {
        return "pong";
    }

}