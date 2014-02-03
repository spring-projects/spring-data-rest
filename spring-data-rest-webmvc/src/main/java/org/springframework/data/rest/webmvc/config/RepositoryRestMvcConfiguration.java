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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriDomainClassConverter;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AnnotatedHandlerBeanPostProcessor;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.invoke.DefaultRepositoryInvokerFactory;
import org.springframework.data.rest.core.invoke.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceDescription;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.support.DomainObjectMerger;
import org.springframework.data.rest.core.util.UUIDConverter;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.PersistentEntityResourceHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerAdapter;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.data.rest.webmvc.RepositoryRestRequestHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.ResourceMetadataHandlerMethodArgumentResolver;
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
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.hal.Jackson2HalModule.HalHandlerInstantiator;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
@ComponentScan(basePackageClasses = RepositoryRestController.class,
		includeFilters = @Filter(RepositoryRestController.class), useDefaultFilters = false)
@ImportResource("classpath*:META-INF/spring-data-rest/**/*.xml")
public class RepositoryRestMvcConfiguration extends HateoasAwareSpringDataWebConfiguration {

	/**
	 * Helper class instead of using {@link EnableHypermediaSupport} directly to make sure the annotation gets inspected
	 * correctly even if users extend {@link RepositoryRestMvcConfiguration}.
	 * 
	 * @see https://jira.springsource.org/browse/SPR-11251
	 * @author Oliver Gierke
	 */
	@Configuration
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class HypermediaConfigurationDelegate {

	}

	private static final boolean IS_JAVAX_VALIDATION_AVAILABLE = ClassUtils.isPresent(
			"javax.validation.ConstraintViolationException", RepositoryRestMvcConfiguration.class.getClassLoader());
	private static final boolean IS_JPA_AVAILABLE = ClassUtils.isPresent("javax.persistence.EntityManager",
			RepositoryRestMvcConfiguration.class.getClassLoader());

	@Autowired ListableBeanFactory beanFactory;
	@Autowired(required = false) List<ResourceProcessor<?>> resourceProcessors = Collections.emptyList();
	@Autowired(required = false) RelProvider relProvider;
	@Autowired(required = false) CurieProvider curieProvider;

	@Bean
	public Repositories repositories() {
		return new Repositories(beanFactory);
	}

	@Bean
	@Qualifier
	public DefaultFormattingConversionService defaultConversionService() {

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		conversionService.addConverter(UUIDConverter.INSTANCE);
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
	public ValidatingRepositoryEventListener validatingRepositoryEventListener(ObjectFactory<Repositories> repositories) {
		ValidatingRepositoryEventListener listener = new ValidatingRepositoryEventListener(repositories);
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
		return new RepositoryRestRequestHandlerMethodArgumentResolver(repositories(), repositoryInvokerFactory(),
				resourceMetadataHandlerMethodArgumentResolver());
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
		return new RepositoryEntityLinks(repositories(), resourceMappings(), config(), pageableResolver());
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

		return new PersistentEntityResourceHandlerMethodArgumentResolver(messageConverters, repoRequestArgumentResolver());
	}

	/**
	 * Turns a domain class into a {@link org.springframework.data.rest.webmvc.json.JsonSchema}.
	 * 
	 * @return
	 */
	@Bean
	public PersistentEntityToJsonSchemaConverter jsonSchemaConverter() {
		return new PersistentEntityToJsonSchemaConverter(repositories(), resourceMappings(),
				resourceDescriptionMessageSourceAccessor());
	}

	/**
	 * The {@link MessageSourceAccessor} to provide messages for {@link ResourceDescription}s being rendered.
	 * 
	 * @return
	 */
	@Bean
	public MessageSourceAccessor resourceDescriptionMessageSourceAccessor() {

		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename("classpath:rest-messages");
		messageSource.setUseCodeAsDefaultMessage(true);

		return new MessageSourceAccessor(messageSource);
	}

	/**
	 * The Jackson {@link ObjectMapper} used internally.
	 * 
	 * @return
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return basicObjectMapper();
	}

	/**
	 * The {@link HttpMessageConverter} used by Spring MVC to read and write JSON data.
	 * 
	 * @return
	 */
	@Bean
	public MappingJackson2HttpMessageConverter jacksonHttpMessageConverter() {

		List<MediaType> mediaTypes = new ArrayList<MediaType>();
		mediaTypes.addAll(Arrays.asList(MediaType.valueOf("application/schema+json"),
				MediaType.valueOf("application/x-spring-data-verbose+json"),
				MediaType.valueOf("application/x-spring-data-compact+json")));

		// Configure this mapper to be used if HAL is not the default media type
		if (!config().getDefaultMediaType().equals(MediaTypes.HAL_JSON)) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
		jacksonConverter.setObjectMapper(objectMapper());
		jacksonConverter.setSupportedMediaTypes(mediaTypes);

		return jacksonConverter;
	}

	//
	// HAL setup
	//

	@Bean
	public MappingJackson2HttpMessageConverter halJacksonHttpMessageConverter() {

		ArrayList<MediaType> mediaTypes = new ArrayList<MediaType>();
		mediaTypes.add(MediaTypes.HAL_JSON);

		// Enable returning HAL if application/json is asked if it's configured to be the default type
		if (config().getDefaultMediaType().equals(MediaTypes.HAL_JSON)) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(halObjectMapper());
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	@Bean
	public ObjectMapper halObjectMapper() {

		HalHandlerInstantiator instantiator = new HalHandlerInstantiator(getDefaultedRelProvider(), curieProvider);

		ObjectMapper mapper = basicObjectMapper();
		mapper.registerModule(new Jackson2HalModule());
		mapper.setHandlerInstantiator(instantiator);

		return mapper;
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
	 * @param resourceProcessors {@link ResourceProcessor}s available in the {@link ApplicationContext}.
	 * @return
	 */
	@Bean
	public RequestMappingHandlerAdapter repositoryExporterHandlerAdapter() {

		List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();
		configureHttpMessageConverters(messageConverters);

		RepositoryRestHandlerAdapter handlerAdapter = new RepositoryRestHandlerAdapter(defaultMethodArgumentResolvers(),
				resourceProcessors);
		handlerAdapter.setMessageConverters(messageConverters);

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

		RepositoryRestHandlerMapping mapping = new RepositoryRestHandlerMapping(resourceMappings(), config());
		mapping.setJpaHelper(jpaHelper());

		return mapping;
	}

	@Bean
	public ResourceMappings resourceMappings() {

		Repositories repositories = repositories();
		RepositoryRestConfiguration config = config();

		return new ResourceMappings(config, repositories, getDefaultedRelProvider());
	}

	/**
	 * Jackson module responsible for intelligently serializing and deserializing JSON that corresponds to an entity.
	 * 
	 * @return
	 */
	@Bean
	public Module persistentEntityJackson2Module() {
		return new PersistentEntityJackson2Module(resourceMappings(), defaultConversionService());
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

	@Bean
	public RepositoryInvokerFactory repositoryInvokerFactory() {
		return new DefaultRepositoryInvokerFactory(repositories(), defaultConversionService());
	}

	@Bean
	public List<HttpMessageConverter<?>> defaultMessageConverters() {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

		if (config().getDefaultMediaType().equals(MediaTypes.HAL_JSON)) {
			messageConverters.add(halJacksonHttpMessageConverter());
			messageConverters.add(jacksonHttpMessageConverter());
		} else {
			messageConverters.add(jacksonHttpMessageConverter());
			messageConverters.add(halJacksonHttpMessageConverter());
		}
		messageConverters.add(uriListHttpMessageConverter());

		return messageConverters;
	}

	private List<HandlerMethodArgumentResolver> defaultMethodArgumentResolvers() {
		return Arrays.asList(pageableResolver(), sortResolver(), serverHttpRequestMethodArgumentResolver(),
				repoRequestArgumentResolver(), persistentEntityArgumentResolver(),
				resourceMetadataHandlerMethodArgumentResolver());
	}

	private ObjectMapper basicObjectMapper() {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		// Our special PersistentEntityResource Module
		objectMapper.registerModule(persistentEntityJackson2Module());
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Jackson2DatatypeHelper.configureObjectMapper(objectMapper);
		// Configure custom Modules
		configureJacksonObjectMapper(objectMapper);

		return objectMapper;
	}

	private RelProvider getDefaultedRelProvider() {
		return this.relProvider != null ? relProvider : new EvoInflectorRelProvider();
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
