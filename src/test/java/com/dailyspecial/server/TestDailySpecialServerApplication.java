package com.dailyspecial.server;

import org.springframework.boot.SpringApplication;

public class TestDailySpecialServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(DailySpecialServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
