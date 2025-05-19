package com.jfc.owl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
	    UserDetailsServiceAutoConfiguration.class
	})
@ComponentScan(basePackages = {"com.jfc.owl", "com.jfc.rdb.common", "com.jfc.rdb.tiptop"})
public class OwlApplication {

	public static void main(String[] args) {
		SpringApplication.run(OwlApplication.class, args);
	}

}
