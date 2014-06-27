/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import javax.sql.DataSource;

import java.net.URI;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Test cases to confirm both {@link org.springframework.data.rest.webmvc.config.EnableRestRepositories} and
 * {@link org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration} work as options to configure
 * Spring Data REST.
 *
 * @author Greg Turnquist
 */
public class EnableRestRepositoriesTest {

	@Test
	public void testAnnotationDrivenStartup() throws Exception {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AnnotationBasedTestConfiguration.class);
		RepositoryRestConfiguration config = ctx.getBean(RepositoryRestConfiguration.class);
		assertNotNull(config);
		assertThat(config.getBaseUri().toString(), equalTo("/api"));

		PersonRepository repository = ctx.getBean(PersonRepository.class);
		assertNotNull(repository);
	}

	@Test
	public void testImportDrivenDefaultStartup() throws Exception {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportBasedTestConfiguration.class);
		RepositoryRestConfiguration config = ctx.getBean(RepositoryRestConfiguration.class);
		assertNotNull(config);
		assertThat(config.getBaseUri().toString(), equalTo(""));

		PersonRepository repository = ctx.getBean(PersonRepository.class);
		assertNotNull(repository);
	}

	@Test
	public void testCustomDrivenStartup() throws Exception {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CustomConfiguration.class);
		RepositoryRestConfiguration config = ctx.getBean(RepositoryRestConfiguration.class);
		assertNotNull(config);
		assertThat(config.getBaseUri().toString(), equalTo("/api"));

		PersonRepository repository = ctx.getBean(PersonRepository.class);
		assertNotNull(repository);
	}

	protected abstract static class AbstractConfiguration {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.HSQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setPersistenceUnitName("spring-data-rest-webmvc");
			factory.setDataSource(dataSource());
			factory.afterPropertiesSet();

			return factory;
		}

	}

	@Configuration
	@EnableJpaRepositories
	@EnableRestRepositories(baseUri = "/api")
	protected static class AnnotationBasedTestConfiguration extends AbstractConfiguration {
	}

	@Configuration
	@EnableJpaRepositories
	@Import(RepositoryRestMvcConfiguration.class)
	protected static class ImportBasedTestConfiguration extends AbstractConfiguration {
	}

	@Configuration
	@EnableJpaRepositories
	@Import(CustomRepositoryRestMvcConfiguration.class)
	protected static class CustomConfiguration extends AbstractConfiguration {
	}

	@Configuration
	protected static class CustomRepositoryRestMvcConfiguration extends RepositoryRestMvcConfiguration {
		@Override
		public RepositoryRestConfiguration config() {
			RepositoryRestConfiguration config = super.config();
			config.setBaseUri(URI.create("/api"));
			return config;

		}
	}

}
