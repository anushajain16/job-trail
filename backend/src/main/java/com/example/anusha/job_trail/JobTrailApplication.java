package com.example.anusha.job_trail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Auth is stateless JWT, handled entirely by AuthService — there's no
// UserDetailsService/AuthenticationManager in play, so this autoconfig
// would only ever generate an unused in-memory user and log a scary
// "Using generated security password" line on every boot.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class JobTrailApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobTrailApplication.class, args);
	}

}
