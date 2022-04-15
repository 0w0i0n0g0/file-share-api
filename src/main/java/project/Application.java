package project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@EnableScheduling 
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@CrossOrigin("*")
@GetMapping("/ping")
public String healthCheck() {
    return "pong";
}