package com.example.viewfinder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.viewfinder.mapper")
@SpringBootApplication
public class ViewfinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ViewfinderApplication.class, args);
    }

}
