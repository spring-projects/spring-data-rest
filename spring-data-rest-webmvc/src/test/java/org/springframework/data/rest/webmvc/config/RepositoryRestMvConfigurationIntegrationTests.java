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
package org.springframework.data.rest.webmvc.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.http.converter.HttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for basic application bootstrapping (general configuration related checks).
 * 
 * @author Oliver Gierke
 */
public class RepositoryRestMvConfigurationIntegrationTests {

	static AbstractApplicationContext context;

	@BeforeClass
	public static void setUp() {
		context = new AnnotationConfigApplicationContext(ExtendingConfiguration.class);
	}

	@AfterClass
	public static void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	/**
	 * @see DATAREST-210
	 */
	@Test
	public void assertEnableHypermediaSupportWorkingCorrectly() {

		assertThat(context.getBean("entityLinksPluginRegistry"), is(notNullValue()));
		assertThat(context.getBean(LinkDiscoverers.class), is(notNullValue()));
	}

	@Test
	public void assertBeansBeingSetUp() throws Exception {

		context.getBean(PageableHandlerMethodArgumentResolver.class);

		// Verify HAL setup
		context.getBean("halJacksonHttpMessageConverter", HttpMessageConverter.class);
		ObjectMapper mapper = context.getBean("halObjectMapper", ObjectMapper.class);
		mapper.writeValueAsString(new RepositoryLinksResource());
	}

	@Configuration
	static class ExtendingConfiguration extends RepositoryRestMvcConfiguration {

		@Bean
		public DefaultRelProvider relProvider() {
			return new DefaultRelProvider();
		}
	}
}
