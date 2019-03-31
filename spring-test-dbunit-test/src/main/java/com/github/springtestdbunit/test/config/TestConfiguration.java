/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.springtestdbunit.test.config;

import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Generic configuration which can be used across all tests.
 *
 * @author Paul Podgorsek
 */
@Configuration
@EnableTransactionManagement
@PropertySource("classpath:properties/database.properties")
public class TestConfiguration {

	@Value("${database.datasource.poolname}")
	private String dataSourcePoolName;

	@Value("${database.datasource.driver}")
	private String dataSourceDriver;

	@Value("${database.datasource.url}")
	private String dataSourceUrl;

	@Value("${database.datasource.username}")
	private String dataSourceUsername;

	@Value("${database.datasource.password}")
	private String dataSourcePassword;

	@Value("${database.dbunit.dataTypeFactory}")
	private String dbUnitDataTypeFactory;

	@Value("${database.hibernate.dialect}")
	private String hibernateDialect;

	@Value("${database.hibernate.format_sql}")
	private boolean hibernateFormatSql;

	@Value("${database.hibernate.show_sql}")
	private boolean hibernateShowSql;

	@Value("${database.hibernate.hbm2ddl.auto}")
	private String hibernateHbm2DdlAuto;

	@Resource
	private List<String> hibernatePackagesToScan;

	@Bean(destroyMethod = "close")
	public DataSource dataSource() {
		return new HikariDataSource(hikariConfig());
	}

	@Bean
	public IDataTypeFactory dataTypeFactory()
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (IDataTypeFactory) Class.forName(dbUnitDataTypeFactory).newInstance();
	}

	@Bean
	public EntityManagerFactory entityManagerFactory() {

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(true);
		vendorAdapter.setShowSql(true);

		Properties jpaProperties = new Properties();
		jpaProperties.put(AvailableSettings.DIALECT, hibernateDialect);
		jpaProperties.put(AvailableSettings.FORMAT_SQL, hibernateFormatSql);
		jpaProperties.put(AvailableSettings.SHOW_SQL, hibernateShowSql);
		jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, hibernateHbm2DdlAuto);
		jpaProperties.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");

		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setJpaVendorAdapter(vendorAdapter);
		factory.setPackagesToScan(hibernatePackagesToScan.toArray(new String[hibernatePackagesToScan.size()]));
		factory.setDataSource(dataSource());
		factory.setJpaProperties(jpaProperties);

		factory.afterPropertiesSet();

		return factory.getObject();
	}

	@Bean
	public HikariConfig hikariConfig() {

		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setPoolName(dataSourcePoolName);
		hikariConfig.setDriverClassName(dataSourceDriver);
		hikariConfig.setJdbcUrl(dataSourceUrl);
		hikariConfig.setUsername(dataSourceUsername);
		hikariConfig.setPassword(dataSourcePassword);
		hikariConfig.setMaximumPoolSize(50);

		return hikariConfig;
	}

	@Bean
	public PlatformTransactionManager transactionManager() {

		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory());

		return txManager;
	}

}
