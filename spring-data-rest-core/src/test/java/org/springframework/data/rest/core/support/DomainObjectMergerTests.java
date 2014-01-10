/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.data.rest.core.support;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.persistence.*;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.support.Repositories;
import org.springframework.hateoas.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainObjectMergerTests {

	@Autowired
	PersonRepository personRepository;

	@Autowired
	GenericApplicationContext context;

	@Test
	public void mergeNewValue() {
		Repositories repositories = new Repositories(context.getBeanFactory());
		ConversionService conversionService = new DefaultConversionService();

		Resource<Person> incoming = new Resource<Person>(new Person("Greg"));
		Person existingDomainObject = new Person("Sara");

		DomainObjectMerger merger = new DomainObjectMerger(repositories,
				conversionService);
		merger.merge(incoming.getContent(), existingDomainObject);

		assertThat(existingDomainObject.getFirstname(), equalTo("Greg"));
	}

	/**
	 * @see DATAREST-130
	 */
	@Test
	public void mergeNullValue() {
		Repositories repositories = new Repositories(context.getBeanFactory());
		ConversionService conversionService = new DefaultConversionService();

		Resource<Person> incoming = new Resource<Person>(new Person(null));
		Person existingDomainObject = new Person("Sara");

		DomainObjectMerger merger = new DomainObjectMerger(repositories,
				conversionService);
		merger.merge(incoming.getContent(), existingDomainObject);

		assertThat(existingDomainObject.getFirstname(), equalTo(null));
	}

	@Configuration
	@EnableJpaRepositories
	static class Config {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
			return new JpaTransactionManager(emf);
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setJpaVendorAdapter(jpaVendorAdapter());
			factoryBean.setPersistenceUnitName("jpa.sample");
			factoryBean.setDataSource(dataSource());
			factoryBean.setPackagesToScan(DomainObjectMergerTests.class.getPackage().getName());
			return factoryBean;
		}

		@Bean
		public JpaVendorAdapter jpaVendorAdapter() {
			HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
			jpaVendorAdapter.setDatabase(Database.HSQL);
			jpaVendorAdapter.setShowSql(true);
			jpaVendorAdapter.setGenerateDdl(true);
			return jpaVendorAdapter;
		}
	}

	@Entity
	class Person {

		private Long id;

		private String firstname;

		public Person() {
			this.firstname = null;
		}

		public Person(String firstname) {
			this.firstname = firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getFirstname() {
			return this.firstname;
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return this.firstname;
		}
	}

}
