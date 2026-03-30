package com.smartstock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartstock.mapper")
public class SmartstockAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartstockAiApplication.class, args);
	}

}
