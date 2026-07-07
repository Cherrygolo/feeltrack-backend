package ld.feeltrack_backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

     @GetMapping({"/api", "/api/"})
    public Map<String, String> api() {
        return Map.of(
                "application", "FeelTrack API",
                "status", "UP",
                "version", "1.0"
        );
    }

}
