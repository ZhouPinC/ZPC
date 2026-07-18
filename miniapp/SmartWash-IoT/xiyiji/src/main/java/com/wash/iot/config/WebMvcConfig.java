package com.wash.iot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        String location = "file:" + uploadRoot.toString() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}

