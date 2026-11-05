package com.emailforensics.backend;

import com.emailforensics.backend.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		DotenvLoader.load();
		SpringApplication.run(BackendApplication.class, args);
	}

}
