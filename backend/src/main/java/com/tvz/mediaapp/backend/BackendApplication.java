package com.tvz.mediaapp.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        try {
            Dotenv dotenv = System.getProperty("user.dir").endsWith("backend") ?
                    Dotenv.configure().directory("../").load() :
                    Dotenv.configure().load();

            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue()));
        } catch (Exception ignored) {}

        SpringApplication.run(BackendApplication.class, args);
    }
}