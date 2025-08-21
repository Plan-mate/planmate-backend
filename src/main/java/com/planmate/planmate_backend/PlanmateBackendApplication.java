package com.planmate.planmate_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PlanmateBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(PlanmateBackendApplication.class, args);
	}
}
