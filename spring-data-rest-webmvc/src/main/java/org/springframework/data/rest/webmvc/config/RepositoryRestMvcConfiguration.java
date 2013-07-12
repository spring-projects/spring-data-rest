/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.convert.ISO8601DateConverter;
import org.springframework.data.rest.convert.UUIDConverter;
import org.springframework.data.rest.repository.UriDomainClassConverter;
import org.springframework.data.rest.repository.context.AnnotatedHandlerBeanPostProcessor;
import org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener;
import org.springframework.data.rest.repository.mapping.ResourceMappings;
import org.springframework.data.rest.repository.support.DomainObjectMerger;
import org.springframework.data.rest.webmvc.BaseUriMethodArgumentResolver;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.PersistentEntityResourceHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerAdapter;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.data.rest.webmvc.RepositoryRestRequestHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.ResourceMetadataHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.RestController;
import org.springframework.data.rest.webmvc.ServerHttpRequestMethodArgumentResolver;
import org.springframework.data.rest.webmvc.convert.UriListHttpMessageConverter;
import org.springframework.data.rest.webmvc.json.Jackson2DatatypeHelper;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverter;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.rest.webmvc.support.ValidationExceptionHandler;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.RelProvider;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Main application configuration for Spring Data REST. To customize how the exporter works, subclass this and override
 * any of the {@literal configure*} methods.
 * <p/>
 * Any XML files located in the classpath under the {@literal META-INF/spring-data-rest/} path will be automatically
 * found and loaded into this {@link org.springframework.context.ApplicationContext}.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@Configuration
@ComponentScan(basePackageClasses = RestController.class, includeFilters = @Filter(RestController.class),
		useDefaultFilters = false)
@ImportResource("classpath*:META-INF/spring-data-rest/**/*.xml")
public class RepositoryRestMvcConfiguration extends HateoasAwareSpringDataWebConfiguration {

	private static final boolean IS_JAVAX_VALIDATION_AVAILABLE = ClassUtils.isPresent(
			"javax.validation.ConstraintViolationException", RepositoryRestMvcConfiguration.class.getClassLoader());
	private static final boolean IS_JPA_AVAILABLE = ClassUtils.isPresent("javax.persistence.EntityManager",
			RepositoryRestMvcConfiguration.class.getClassLoader());

	@Autowired ListableBeanFactory beanFactory;

	@Bean
	public Repositories repositories() {
		return new Repositories(beanFactory);
	}

	@Bean
	@Qualifier
	public DefaultFormattingConversionService defaultConversionService() {

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		conversionService.addConverter(UUIDConverter.INSTANCE);
		conversionService.addConverter(ISO8601DateConverter.INSTANCE);
		configureConversionService(conversionService);
		return conversionService;
	}

	@Bean
	public DomainClassConverter<?> domainClassConverter() {
		return new DomainClassConverter<DefaultFormattingConversionService>(defaultConversionService());
	}

	@Bean
	public UriDomainClassConverter uriDomainClassConverter() {
		return new UriDomainClassConverter(repositories(), domainClassConverter());
	}

	/**
	 * {@link org.springframework.context.ApplicationListener} implementation for invoking
	 * {@link org.springframework.validation.Validator} instances assigned to specific domain types.
	 */
	@Bean
	public ValidatingRepositoryEventListener validatingRepositoryEventListener() {
		ValidatingRepositoryEventListener listener = new ValidatingRepositoryEventListener();
		configureValidatingRepositoryEventListener(listener);
		return listener;
	}

	@Bean
	@Lazy
	public ValidationExceptionHandler validationExceptionHandler() {
		if (IS_JAVAX_VALIDATION_AVAILABLE) {
			return new ValidationExceptionHandler();
		} else {
			return null;
		}
	}

	@Bean
	@Lazy
	public JpaHelper jpaHelper() {
		if (IS_JPA_AVAILABLE) {
			return new JpaHelper();
		} else {
			return null;
		}
	}

	/**
	 * Main configuration for the REST exporter.
	 */
	@Bean
	public RepositoryRestConfiguration config() {
		RepositoryRestConfiguration config = new RepositoryRestConfiguration();
		configureRepositoryRestConfiguration(config);
		return config;
	}

	/**
	 * {@link org.springframework.beans.factory.config.BeanPostProcessor} to turn beans annotated as
	 * {@link org.springframework.data.rest.repository.annotation.RepositoryEventHandler}s.
	 * 
	 * @return
	 */
	@Bean
	public AnnotatedHandlerBeanPostProcessor annotatedHandlerBeanPostProcessor() {
		return new AnnotatedHandlerBeanPostProcessor();
	}

	/**
	 * For merging incoming objects materialized from JSON with existing domain objects loaded from the repository.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Bean
	public DomainObjectMerger domainObjectMerger() throws Exception {
		return new DomainObjectMerger(repositories(), defaultConversionService());
	}

	/**
	 * Resolves the base {@link java.net.URI} under which this application is configured.
	 * 
	 * @return
	 */
	@Bean
	public BaseUriMethodArgumentResolver baseUriMethodArgumentResolver() {
		return new BaseUriMethodArgumentResolver(config());
	}

	/**
	 * Turns an {@link javax.servlet.http.HttpServletRequest} into a
	 * {@link org.springframework.http.server.ServerHttpRequest}.
	 * 
	 * @return
	 */
	@Bean
	public ServerHttpRequestMethodArgumentResolver serverHttpRequestMethodArgumentResolver() {
		return new ServerHttpRequestMethodArgumentResolver();
	}

	/**
	 * A convenience resolver that pulls together all the information needed to service a request.
	 * 
	 * @return
	 */
	@Bean
	public RepositoryRestRequestHandlerMethodArgumentResolver repoRequestArgumentResolver() {
		return new RepositoryRestRequestHandlerMethodArgumentResolver(defaultConversionService());
	}

	@Bean
	public ResourceMetadataHandlerMethodArgumentResolver resourceMetadataHandlerMethodArgumentResolver() {
		return new ResourceMetadataHandlerMethodArgumentResolver(repositories(), resourceMappings());
	}

	/**
	 * A special {@link org.springframework.hateoas.EntityLinks} implementation that takes repository and current
	 * configuration into account when generating links.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Bean
	public EntityLinks entityLinks() {
		return new RepositoryEntityLinks(repositories(), resourceMappings(), config());
	}

	/**
	 * Reads incoming JSON into an entity.
	 * 
	 * @return
	 */
	@Bean
	public PersistentEntityResourceHandlerMethodArgumentResolver persistentEntityArgumentResolver() {
		List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();
		configureHttpMessageConverters(messageConverters);

		return new PersistentEntityResourceHandlerMethodArgumentResolver(messageConverters);
	}

	/**
	 * Turns a domain class into a {@link org.springframework.data.rest.webmvc.json.JsonSchema}.
	 * 
	 * @return
	 */
	@Bean
	public PersistentEntityToJsonSchemaConverter jsonSchemaConverter() {
		return new PersistentEntityToJsonSchemaConverter(repositories(), resourceMappings());
	}

	/**
	 * The Jackson {@link ObjectMapper} used internally.
	 * 
	 * @return
	 */
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		// Our special PersistentEntityResource Module
		objectMapper.registerModule(persistentEntityJackson2Module());
		Jackson2DatatypeHelper.configureObjectMapper(objectMapper);
		// Configure custom Modules
		configureJacksonObjectMapper(objectMapper);

		return objectMapper;
	}

	/**
	 * The {@link HttpMessageConverter} used by Spring MVC to read and write JSON data.
	 * 
	 * @return
	 */
	@Bean
	public MappingJackson2HttpMessageConverter jacksonHttpMessageConverter() {
		MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
		jacksonConverter.setObjectMapper(objectMapper());
		jacksonConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON,
				MediaType.valueOf("application/schema+json"), MediaType.valueOf("application/x-spring-data-verbose+json"),
				MediaType.valueOf("application/x-spring-data-compact+json")));
		return jacksonConverter;
	}

	/**
	 * The {@link HttpMessageConverter} used to create {@literal text/uri-list} responses.
	 * 
	 * @return
	 */
	@Bean
	public UriListHttpMessageConverter uriListHttpMessageConverter() {
		return new UriListHttpMessageConverter();
	}

	@Bean
	public PersistentEntityResourceAssembler<Object> persistentEntityResourceAssembler() {
		return new PersistentEntityResourceAssembler<Object>(repositories(), entityLinks());
	}

	/**
	 * Special {@link org.springframework.web.servlet.HandlerAdapter} that only recognizes handler methods defined in the
	 * provided controller classes.
	 * 
	 * @return
	 */
	@Bean
	public RequestMappingHandlerAdapter repositoryExporterHandlerAdapter() {

		List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();
		configureHttpMessageConverters(messageConverters);

		RepositoryRestHandlerAdapter handlerAdapter = new RepositoryRestHandlerAdapter();
		handlerAdapter.setMessageConverters(messageConverters);
		handlerAdapter.setCustomArgumentResolvers(defaultMethodArgumentResolvers());

		return handlerAdapter;
	}

	/**
	 * Special {@link org.springframework.web.servlet.HandlerMapping} that only recognizes handler methods defined in the
	 * provided controller classes.
	 * 
	 * @return
	 */
	@Bean
	public RequestMappingHandlerMapping repositoryExporterHandlerMapping() {
		return new RepositoryRestHandlerMapping(resourceMappings());
	}

	@Bean
	public ResourceMappings resourceMappings() {

		Repositories repositories = repositories();
		RepositoryRestConfiguration config = config();

		try {
			RelProvider relProvider = beanFactory.getBean(RelProvider.class);
			return new ResourceMappings(config, repositories, relProvider);
		} catch (NoSuchBeanDefinitionException e) {
			return new ResourceMappings(config, repositories);
		}
	}

	/**
	 * Jackson module responsible for intelligently serializing and deserializing JSON that corresponds to an entity.
	 * 
	 * @return
	 */
	@Bean
	public Module persistentEntityJackson2Module() {
		return new PersistentEntityJackson2Module(resourceMappings());
	}

	/**
	 * Bean for looking up methods annotated with {@link org.springframework.web.bind.annotation.ExceptionHandler}.
	 * 
	 * @return
	 */
	@Bean
	public ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
		ExceptionHandlerExceptionResolver er = new ExceptionHandlerExceptionResolver();
		er.setCustomArgumentResolvers(defaultMethodArgumentResolvers());

		List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();
		configureHttpMessageConverters(messageConverters);

		er.setMessageConverters(messageConverters);
		configureExceptionHandlerExceptionResolver(er);

		return er;
	}

	private List<HttpMessageConverter<?>> defaultMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(jacksonHttpMessageConverter());
		messageConverters.add(uriListHttpMessageConverter());
		return messageConverters;
	}

	private List<HandlerMethodArgumentResolver> defaultMethodArgumentResolvers() {
		return Arrays.asList(baseUriMethodArgumentResolver(), pageableResolver(), sortResolver(),
				serverHttpRequestMethodArgumentResolver(), repoRequestArgumentResolver(), persistentEntityArgumentResolver(),
				resourceMetadataHandlerMethodArgumentResolver());
	}

	/**
	 * Override this method to add additional configuration.
	 * 
	 * @param config Main configuration bean.
	 */
	protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {}

	/**
	 * Override this method to add your own converters.
	 * 
	 * @param conversionService Default ConversionService bean.
	 */
	protected void configureConversionService(ConfigurableConversionService conversionService) {}

	/**
	 * Override this method to add validators manually.
	 * 
	 * @param validatingListener The {@link org.springframework.context.ApplicationListener} responsible for invoking
	 *          {@link org.springframework.validation.Validator} instances.
	 */
	protected void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {}

	/**
	 * Configure the {@link ExceptionHandlerExceptionResolver}.
	 * 
	 * @param exceptionResolver The default exception resolver on which you can add custom argument resolvers.
	 */
	protected void configureExceptionHandlerExceptionResolver(ExceptionHandlerExceptionResolver exceptionResolver) {}

	/**
	 * Configure the available {@link HttpMessageConverter}s by adding your own.
	 * 
	 * @param messageConverters The converters to be used by the system.
	 */
	protected void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {}

	/**
	 * Configure the Jackson {@link ObjectMapper} directly.
	 * 
	 * @param objectMapper The {@literal ObjectMapper} to be used by the system.
	 */
	protected void configureJacksonObjectMapper(ObjectMapper objectMapper) {}
}
