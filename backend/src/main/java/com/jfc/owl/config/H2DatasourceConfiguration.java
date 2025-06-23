package com.jfc.owl.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		entityManagerFactoryRef = "h2EntityManagerFactory"
	, transactionManagerRef = "h2TransactionManager"
	, basePackages = {"com.jfc.owl.repository" })
public class H2DatasourceConfiguration {
	
	@Value("${spring.jpa.hibernate.ddl-auto:none}")
	private String ddlAuto;
	
	@Bean(name = "h2Properties")
	@ConfigurationProperties("spring.datasource.h2")
	DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name = "h2Datasource")
	@ConfigurationProperties(prefix = "spring.datasource.h2.hikari")
	DataSource datasource(@Qualifier("h2Properties") DataSourceProperties properties) {
		return properties.initializeDataSourceBuilder().build();
	}
	
	@Bean(name = "h2EntityManagerFactory")
	LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(
			EntityManagerFactoryBuilder builder,
			@Qualifier("h2Datasource") DataSource dataSource) {
		Map<String, Object> properties = new HashMap<>();
	    properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
	    properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
	    
	    // H2 特定的 DDL 設定
	    // 即使全局設定為 none，H2 也應該允許 update
	    if ("none".equals(ddlAuto)) {
	        properties.put("hibernate.hbm2ddl.auto", "update");
	    } else {
	    	properties.put("hibernate.hbm2ddl.auto", ddlAuto);
	    }
	    
		return builder.dataSource(dataSource)
				.packages("com.jfc.owl.entity")
				.persistenceUnit("owl")
				.properties(properties)
				.build();
	}
	
	@Bean(name = "h2TransactionManager")
	PlatformTransactionManager transactionManager(
			@Qualifier("h2EntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
