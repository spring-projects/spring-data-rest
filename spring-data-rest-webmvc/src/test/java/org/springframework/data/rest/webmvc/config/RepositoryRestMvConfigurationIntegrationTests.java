/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.rest.webmvc.util.AssertionUtils.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.alps.AlpsJsonHttpMessageConverter;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.hateoas.server.core.DefaultLinkRelationProvider;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Integration tests for basic application bootstrapping (general configuration related checks).
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Greg Turnquist
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

	@Test // DATAREST-210
	public void assertEnableHypermediaSupportWorkingCorrectly() {

		assertThat(context.getBean("entityLinksPluginRegistry")).isNotNull();
		assertThat(context.getBean(LinkDiscoverers.class)).isNotNull();
	}

	@Test
	public void assertBeansBeingSetUp() throws Exception {

		context.getBean(PageableHandlerMethodArgumentResolver.class);

		// Verify HAL setup
		getObjectMapper().writeValueAsString(new RepositoryLinksResource());
	}

	@Test // DATAREST-271
	public void assetConsidersPaginationCustomization() {

		HateoasPageableHandlerMethodArgumentResolver resolver = context
				.getBean(HateoasPageableHandlerMethodArgumentResolver.class);

		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		resolver.enhance(builder, null, PageRequest.of(0, 9000, Direction.ASC, "firstname"));

		MultiValueMap<String, String> params = builder.build().getQueryParams();

		assertThat(params.containsKey("myPage")).isTrue();
		assertThat(params.containsKey("mySort")).isTrue();

		assertThat(params.get("mySize")).hasSize(1);
		assertThat(params.get("mySize").get(0)).isEqualTo("7000");
	}

	@Test // DATAREST-336 DATAREST-1509
	public void objectMapperRendersDatesInIsoByDefault() throws Exception {

		Sample sample = new Sample();
		sample.date = new Date();

		ObjectMapper mapper = getObjectMapper();

		String result = JsonPath.read(mapper.writeValueAsString(sample), "$.date");

		assertThat(result).matches(suffix("+00") //
				.or(suffix("+0000")) //
				.or(suffix("+00:00")), "ISO-8601-TZ1, ISO-8601-TZ2, or ISO-8601-TZ3");
	}

	@Test(expected = NoSuchBeanDefinitionException.class) // DATAREST-362
	public void doesNotExposePersistentEntityJackson2ModuleAsBean() {
		context.getBean(PersistentEntityJackson2Module.class);
	}

	@Test // DATAREST-362
	public void registeredHttpMessageConvertersAreTypeConstrained() {

		Collection<MappingJackson2HttpMessageConverter> converters = context
				.getBeansOfType(MappingJackson2HttpMessageConverter.class).values();

		converters.forEach(converter -> {
			assertThat(converter).isInstanceOfAny(TypeConstrainedMappingJackson2HttpMessageConverter.class,
					AlpsJsonHttpMessageConverter.class);
		});
	}

	@Test // DATAREST-424
	public void halHttpMethodConverterIsRegisteredBeforeTheGeneralOne() {

		CollectingComponent component = context.getBean(CollectingComponent.class);
		List<HttpMessageConverter<?>> converters = component.converters;

		assertThat(converters.get(0).getSupportedMediaTypes()).contains(MediaTypes.HAL_JSON);
		assertThat(converters.get(1).getSupportedMediaTypes()).contains(RestMediaTypes.SCHEMA_JSON);
	}

	@Test // DATAREST-424
	public void halHttpMethodConverterIsRegisteredAfterTheGeneralOneIfHalIsDisabledAsDefaultMediaType() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NonHalConfiguration.class);
		CollectingComponent component = context.getBean(CollectingComponent.class);
		context.close();

		List<HttpMessageConverter<?>> converters = component.converters;

		assertThat(converters.get(0).getSupportedMediaTypes()).contains(RestMediaTypes.SCHEMA_JSON);
		assertThat(converters.get(1).getSupportedMediaTypes()).contains(MediaTypes.HAL_JSON);
	}

	@Test // DATAREST-431, DATACMNS-626
	public void hasConvertersForPointAndDistance() {

		ConversionService service = context.getBean("defaultConversionService", ConversionService.class);

		assertThat(service.canConvert(String.class, Point.class)).isTrue();
		assertThat(service.canConvert(Point.class, String.class)).isTrue();
		assertThat(service.canConvert(String.class, Distance.class)).isTrue();
		assertThat(service.canConvert(Distance.class, String.class)).isTrue();
	}

	@Test // DATAREST-1198
	public void hasConvertersForNamAndLdapName() {

		ConversionService service = context.getBean("defaultConversionService", ConversionService.class);

		assertThat(service.canConvert(String.class, Name.class)).isTrue();
		assertThat(service.canConvert(String.class, LdapName.class)).isTrue();
	}

	private static ObjectMapper getObjectMapper() {

		AbstractJackson2HttpMessageConverter converter = context.getBean("halJacksonHttpMessageConverter",
				AbstractJackson2HttpMessageConverter.class);
		return converter.getObjectMapper();
	}

	@Configuration
	@Import(RepositoryRestMvcConfiguration.class)
	static class ExtendingConfiguration {

		@Bean
		DefaultLinkRelationProvider relProvider() {
			return new DefaultLinkRelationProvider();
		}

		@Bean
		CollectingComponent collectingComponent() {
			return new CollectingComponent();
		}

		@Bean
		RepositoryRestConfigurer configurer() {

			return RepositoryRestConfigurer.withConfig(config -> {

				config.setDefaultPageSize(45);
				config.setMaxPageSize(7000);
				config.setPageParamName("myPage");
				config.setLimitParamName("mySize");
				config.setSortParamName("mySort");
			});
		}
	}

	@Configuration
	@Import(RepositoryRestMvcConfiguration.class)
	static class NonHalConfiguration {

		@Bean
		CollectingComponent collectingComponent() {
			return new CollectingComponent();
		}

		@Bean
		RepositoryRestConfigurer configurer() {
			return RepositoryRestConfigurer.withConfig(config -> config.useHalAsDefaultJsonMediaType(false));
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
