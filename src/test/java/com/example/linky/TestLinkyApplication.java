package com.example.linky;

import org.springframework.boot.SpringApplication;

public class TestLinkyApplication {

	public static void main(String[] args) {
		SpringApplication.from(LinkyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
