/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

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

	/**
	 * @see DATAREST-271
	 */
	@Test
	public void assetConsidersPaginationCustomization() {

		HateoasPageableHandlerMethodArgumentResolver resolver = context
				.getBean(HateoasPageableHandlerMethodArgumentResolver.class);

		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		resolver.enhance(builder, null, new PageRequest(0, 9000, Direction.ASC, "firstname"));

		MultiValueMap<String, String> params = builder.build().getQueryParams();

		assertThat(params.containsKey("myPage"), is(true));
		assertThat(params.containsKey("mySort"), is(true));

		assertThat(params.get("mySize"), hasSize(1));
		assertThat(params.get("mySize").get(0), is("7000"));
	}

	/**
	 * @see DATAREST-336
	 */
	@Test
	public void objectMapperRendersDatesInIsoByDefault() throws Exception {

		Sample sample = new Sample();
		sample.date = new Date();

		ObjectMapper mapper = context.getBean("objectMapper", ObjectMapper.class);

		DateFormatter formatter = new DateFormatter();
		formatter.setIso(ISO.DATE_TIME);

		Object result = JsonPath.read(mapper.writeValueAsString(sample), "$.date");
		assertThat(result, is(instanceOf(String.class)));
		assertThat(result, is((Object) formatter.print(sample.date, Locale.US)));
	}

	/**
	 * @see DATAREST-362
	 */
	@Test(expected = NoSuchBeanDefinitionException.class)
	public void doesNotExposePersistentEntityJackson2ModuleAsBean() {
		context.getBean(PersistentEntityJackson2Module.class);
	}

	/**
	 * @see DATAREST-362
	 */
	@Test
	public void registeredHttpMessageConvertersAreTypeConstrained() {

		Collection<MappingJackson2HttpMessageConverter> converters = context.getBeansOfType(
				MappingJackson2HttpMessageConverter.class).values();

		for (HttpMessageConverter<?> converter : converters) {
			assertThat(converter, is(instanceOf(TypeConstrainedMappingJackson2HttpMessageConverter.class)));
		}
	}

	/**
	 * @see DATAREST-424
	 */
	@Test
	public void halHttpMethodConverterIsRegisteredBeforeTheGeneralOne() {

		CollectingComponent component = context.getBean(CollectingComponent.class);
		List<HttpMessageConverter<?>> converters = component.converters;

		assertThat(converters.get(0).getSupportedMediaTypes(), hasItem(MediaTypes.HAL_JSON));
		assertThat(converters.get(1).getSupportedMediaTypes(), hasItem(RestMediaTypes.SCHEMA_JSON));
	}

	/**
	 * @see DATAREST-424
	 */
	@Test
	public void halHttpMethodConverterIsRegisteredAfterTheGeneralOneIfHalIsDisabledAsDefaultMediaType() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NonHalConfiguration.class);
		CollectingComponent component = context.getBean(CollectingComponent.class);
		context.close();

		List<HttpMessageConverter<?>> converters = component.converters;

		assertThat(converters.get(0).getSupportedMediaTypes(), hasItem(RestMediaTypes.SCHEMA_JSON));
		assertThat(converters.get(1).getSupportedMediaTypes(), hasItem(MediaTypes.HAL_JSON));
	}

	@Configuration
	static class ExtendingConfiguration extends RepositoryRestMvcConfiguration {

		@Bean
		public DefaultRelProvider relProvider() {
			return new DefaultRelProvider();
		}

		@Bean
		public CollectingComponent collectingComponent() {
			return new CollectingComponent();
		}

		@Override
		protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {

			config.setDefaultPageSize(45);
			config.setMaxPageSize(7000);
			config.setPageParamName("myPage");
			config.setLimitParamName("mySize");
			config.setSortParamName("mySort");
		}
	}

	@Configuration
	static class NonHalConfiguration extends RepositoryRestMvcConfiguration {

		@Bean
		public CollectingComponent collectingComponent() {
			return new CollectingComponent();
		}

		@Override
		protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.useHalAsDefaultJsonMediaType(false);
		}
	}

	static class Sample {

		public Date date;
	}

	static class CollectingComponent {

		List<HttpMessageConverter<?>> converters;

		@Autowired
		public void setConverters(List<HttpMessageConverter<?>> converters) {
			this.converters = converters;
		}
	}
}
