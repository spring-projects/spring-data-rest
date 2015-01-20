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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoModule;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AnnotatedHandlerBeanPostProcessor;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.invoke.DefaultRepositoryInvokerFactory;
import org.springframework.data.rest.core.invoke.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceDescription;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.projection.ProxyProjectionFactory;
import org.springframework.data.rest.core.support.DomainObjectMerger;
import org.springframework.data.rest.core.support.RepositoryRelProvider;
import org.springframework.data.rest.core.util.UUIDConverter;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.BaseUriAwareController;
import org.springframework.data.rest.webmvc.BaseUriAwareHandlerMapping;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerAdapter;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.ServerHttpRequestMethodArgumentResolver;
import org.springframework.data.rest.webmvc.alps.AlpsJsonHttpMessageConverter;
import org.springframework.data.rest.webmvc.alps.AlpsResourceProcessor;
import org.springframework.data.rest.webmvc.alps.RootResourceInformationToAlpsDescriptorConverter;
import org.springframework.data.rest.webmvc.convert.StringToDistanceConverter;
import org.springframework.data.rest.webmvc.convert.StringToPointConverter;
import org.springframework.data.rest.webmvc.convert.UriListHttpMessageConverter;
import org.springframework.data.rest.webmvc.json.DomainObjectReader;
import org.springframework.data.rest.webmvc.json.Jackson2DatatypeHelper;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverter;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter;
import org.springframework.data.rest.webmvc.support.BackendIdHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.support.DefaultedPageableHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.support.HttpMethodHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.hal.Jackson2HalModule.HalHandlerInstantiator;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
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
 * @author Oliver Gierke
 * @author Jon Brisbin
 */
@Configuration
@EnableHypermediaSupport(type = HypermediaType.HAL)
@ComponentScan(basePackageClasses = RepositoryRestController.class,
		includeFilters = @Filter(BaseUriAwareController.class), useDefaultFilters = false)
@ImportResource("classpath*:META-INF/spring-data-rest/**/*.xml")
@Import(SpringDataJacksonConfiguration.class)
public class RepositoryRestMvcConfiguration extends HateoasAwareSpringDataWebConfiguration {

	private static final boolean IS_JPA_AVAILABLE = ClassUtils.isPresent("javax.persistence.EntityManager",
			RepositoryRestMvcConfiguration.class.getClassLoader());

	@Autowired ListableBeanFactory beanFactory;

	@Autowired(required = false) List<BackendIdConverter> idConverters = Collections.emptyList();

	@Autowired(required = false) RelProvider relProvider;
	@Autowired(required = false) CurieProvider curieProvider;

	@Bean
	public Repositories repositories() {
		return new Repositories(beanFactory);
	}

	@Bean
	public RepositoryRelProvider repositoryRelProvider(ObjectFactory<ResourceMappings> resourceMappings) {
		return new RepositoryRelProvider(resourceMappings);
	}

	@Bean
	public PersistentEntities persistentEntities() {

		List<MappingContext<?, ?>> arrayList = new ArrayList<MappingContext<?, ?>>();

		for (MappingContext<?, ?> context : BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory,
				MappingContext.class).values()) {
			arrayList.add(context);
		}

		return new PersistentEntities(arrayList);
	}

	@Bean
	@Qualifier
	public DefaultFormattingConversionService defaultConversionService() {

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		conversionService.addConverter(UUIDConverter.INSTANCE);
		addFormatters(conversionService);
		configureConversionService(conversionService);

		if (!conversionService.canConvert(String.class, Point.class)) {
			conversionService.addConverter(StringToPointConverter.INSTANCE);
		}

		if (!conversionService.canConvert(String.class, Distance.class)) {
			conversionService.addConverter(StringToDistanceConverter.INSTANCE);
		}

		return conversionService;
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

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();

		for (Class<?> projection : getProjections(repositories())) {
			configuration.addProjection(projection);
		}

		RepositoryRestConfiguration config = new RepositoryRestConfiguration(configuration, metadataConfiguration());
		configureRepositoryRestConfiguration(config);
		return config;
	}

	@Bean
	public MetadataConfiguration metadataConfiguration() {
		return new MetadataConfiguration();
	}

	@Bean
	public BaseUri baseUri() {
		return new BaseUri(config().getBaseUri());
	}

	/**
	 * {@link org.springframework.beans.factory.config.BeanPostProcessor} to turn beans annotated as
	 * {@link org.springframework.data.rest.repository.annotation.RepositoryEventHandler}s.
	 * 
	 * @return
	 */
	@Bean
	public static AnnotatedHandlerBeanPostProcessor annotatedHandlerBeanPostProcessor() {
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
	public RootResourceInformationHandlerMethodArgumentResolver repoRequestArgumentResolver() {
		return new RootResourceInformationHandlerMethodArgumentResolver(repositories(), repositoryInvokerFactory(),
				resourceMetadataHandlerMethodArgumentResolver());
	}

	@Bean
	public ResourceMetadataHandlerMethodArgumentResolver resourceMetadataHandlerMethodArgumentResolver() {
		return new ResourceMetadataHandlerMethodArgumentResolver(repositories(), resourceMappings(), baseUri());
	}

	@Bean
	public BackendIdHandlerMethodArgumentResolver backendIdHandlerMethodArgumentResolver() {
		return new BackendIdHandlerMethodArgumentResolver(backendIdConverterRegistry(),
				resourceMetadataHandlerMethodArgumentResolver(), baseUri());
	}

	/**
	 * A special {@link org.springframework.hateoas.EntityLinks} implementation that takes repository and current
	 * configuration into account when generating links.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Bean
	public RepositoryEntityLinks entityLinks() {
		return new RepositoryEntityLinks(repositories(), resourceMappings(), config(), pageableResolver(),
				backendIdConverterRegistry());
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

		return new PersistentEntityResourceHandlerMethodArgumentResolver(messageConverters, repoRequestArgumentResolver(),
				backendIdHandlerMethodArgumentResolver(), new DomainObjectReader(persistentEntities(), resourceMappings()));
	}

	/**
	 * Turns a domain class into a {@link org.springframework.data.rest.webmvc.json.JsonSchema}.
	 * 
	 * @return
	 */
	@Bean
	public PersistentEntityToJsonSchemaConverter jsonSchemaConverter() {
		return new PersistentEntityToJsonSchemaConverter(persistentEntities(), resourceMappings(),
				resourceDescriptionMessageSourceAccessor(), entityLinks());
	}

	/**
	 * The {@link MessageSourceAccessor} to provide messages for {@link ResourceDescription}s being rendered.
	 * 
	 * @return
	 */
	@Bean
	public MessageSourceAccessor resourceDescriptionMessageSourceAccessor() {

		try {

			PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
			propertiesFactoryBean.setLocation(new ClassPathResource("rest-default-messages.properties"));
			propertiesFactoryBean.afterPropertiesSet();

			ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
			messageSource.setBasename("classpath:rest-messages");
			messageSource.setCommonMessages(propertiesFactoryBean.getObject());

			return new MessageSourceAccessor(messageSource);

		} catch (Exception o_O) {
			throw new BeanCreationException("resourceDescriptionMessageSourceAccessor", "", o_O);
		}
	}

	/**
	 * The Jackson {@link ObjectMapper} used internally.
	 * 
	 * @return
	 */
	@Bean
	public ObjectMapper objectMapper() {

		ObjectMapper mapper = basicObjectMapper();
		mapper.registerModule(persistentEntityJackson2Module());

		return mapper;
	}

	/**
	 * The {@link HttpMessageConverter} used by Spring MVC to read and write JSON data.
	 * 
	 * @return
	 */
	@Bean
	public MappingJackson2HttpMessageConverter jacksonHttpMessageConverter() {

		List<MediaType> mediaTypes = new ArrayList<MediaType>();

		// Configure this mapper to be used if HAL is not the default media type
		if (!config().useHalAsDefaultJsonMediaType()) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		int order = config().useHalAsDefaultJsonMediaType() ? Ordered.LOWEST_PRECEDENCE - 1
				: Ordered.LOWEST_PRECEDENCE - 10;

		mediaTypes.addAll(Arrays.asList(RestMediaTypes.SCHEMA_JSON, //
				RestMediaTypes.JSON_PATCH_JSON, RestMediaTypes.MERGE_PATCH_JSON, //
				RestMediaTypes.SPRING_DATA_VERBOSE_JSON, RestMediaTypes.SPRING_DATA_COMPACT_JSON));

		MappingJackson2HttpMessageConverter jacksonConverter = new ResourceSupportHttpMessageConverter(order);
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
		if (config().useHalAsDefaultJsonMediaType()) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		int order = config().useHalAsDefaultJsonMediaType() ? Ordered.LOWEST_PRECEDENCE - 10
				: Ordered.LOWEST_PRECEDENCE - 1;

		MappingJackson2HttpMessageConverter converter = new ResourceSupportHttpMessageConverter(order);
		converter.setObjectMapper(halObjectMapper());
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	@Bean
	public ObjectMapper halObjectMapper() {

		RelProvider defaultedRelProvider = this.relProvider != null ? this.relProvider : new EvoInflectorRelProvider();

		HalHandlerInstantiator instantiator = new HalHandlerInstantiator(defaultedRelProvider, curieProvider);

		ObjectMapper mapper = basicObjectMapper();
		mapper.registerModule(persistentEntityJackson2Module());
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

		Collection<ResourceProcessor> beans = beanFactory.getBeansOfType(ResourceProcessor.class, false, false).values();
		List<ResourceProcessor<?>> processors = new ArrayList<ResourceProcessor<?>>(beans.size());

		for (ResourceProcessor<?> bean : beans) {
			processors.add(bean);
		}

		AnnotationAwareOrderComparator.sort(processors);

		// Forward conversion service to handler adapter
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(defaultConversionService());

		RepositoryRestHandlerAdapter handlerAdapter = new RepositoryRestHandlerAdapter(defaultMethodArgumentResolvers(),
				processors);
		handlerAdapter.setWebBindingInitializer(initializer);
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
	public RequestMappingHandlerMapping fallbackMapping() {
		return new BaseUriAwareHandlerMapping(config());
	}

	@Bean
	public ResourceMappings resourceMappings() {

		Repositories repositories = repositories();
		RepositoryRestConfiguration config = config();

		return new RepositoryResourceMappings(config, repositories);
	}

	/**
	 * Jackson module responsible for intelligently serializing and deserializing JSON that corresponds to an entity.
	 * 
	 * @return
	 */
	protected Module persistentEntityJackson2Module() {

		PersistentEntities entities = persistentEntities();

		return new PersistentEntityJackson2Module(resourceMappings(), entities, config(), new UriToEntityConverter(
				entities, defaultConversionService()));
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

		if (config().metadataConfiguration().alpsEnabled()) {
			messageConverters.add(new AlpsJsonHttpMessageConverter(alpsConverter()));
		}

		if (config().getDefaultMediaType().equals(MediaTypes.HAL_JSON)) {
			messageConverters.add(halJacksonHttpMessageConverter());
			messageConverters.add(jacksonHttpMessageConverter());
		} else {
			messageConverters.add(jacksonHttpMessageConverter());
			messageConverters.add(halJacksonHttpMessageConverter());
		}

		MappingJackson2HttpMessageConverter fallbackJsonConverter = new MappingJackson2HttpMessageConverter();
		fallbackJsonConverter.setObjectMapper(basicObjectMapper());

		messageConverters.add(fallbackJsonConverter);
		messageConverters.add(uriListHttpMessageConverter());

		return messageConverters;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration#pageableResolver()
	 */
	@Bean
	@Override
	public HateoasPageableHandlerMethodArgumentResolver pageableResolver() {

		HateoasPageableHandlerMethodArgumentResolver resolver = super.pageableResolver();
		resolver.setPageParameterName(config().getPageParamName());
		resolver.setSizeParameterName(config().getLimitParamName());
		resolver.setFallbackPageable(new PageRequest(0, config().getDefaultPageSize()));
		resolver.setMaxPageSize(config().getMaxPageSize());

		return resolver;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration#sortResolver()
	 */
	@Bean
	@Override
	public HateoasSortHandlerMethodArgumentResolver sortResolver() {

		HateoasSortHandlerMethodArgumentResolver resolver = super.sortResolver();
		resolver.setSortParameter(config().getSortParamName());

		return resolver;
	}

	@Bean
	public PluginRegistry<BackendIdConverter, Class<?>> backendIdConverterRegistry() {

		List<BackendIdConverter> converters = new ArrayList<BackendIdConverter>(idConverters.size());
		converters.addAll(this.idConverters);
		converters.add(DefaultIdConverter.INSTANCE);

		return OrderAwarePluginRegistry.create(converters);
	}

	protected List<HandlerMethodArgumentResolver> defaultMethodArgumentResolvers() {

		PersistentEntityResourceAssemblerArgumentResolver peraResolver = new PersistentEntityResourceAssemblerArgumentResolver(
				repositories(), entityLinks(), config().projectionConfiguration(), new ProxyProjectionFactory(beanFactory),
				resourceMappings());

		HateoasPageableHandlerMethodArgumentResolver pageableResolver = pageableResolver();
		HandlerMethodArgumentResolver defaultedPageableResolver = new DefaultedPageableHandlerMethodArgumentResolver(
				pageableResolver);

		return Arrays.asList(defaultedPageableResolver, pageableResolver, sortResolver(),
				serverHttpRequestMethodArgumentResolver(), repoRequestArgumentResolver(), persistentEntityArgumentResolver(),
				resourceMetadataHandlerMethodArgumentResolver(), HttpMethodHandlerMethodArgumentResolver.INSTANCE,
				peraResolver, backendIdHandlerMethodArgumentResolver());
	}

	@Autowired GeoModule geoModule;

	protected ObjectMapper basicObjectMapper() {

		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.registerModule(geoModule);

		Jackson2DatatypeHelper.configureObjectMapper(objectMapper);
		// Configure custom Modules
		configureJacksonObjectMapper(objectMapper);

		return objectMapper;
	}

	@SuppressWarnings("unchecked")
	private Set<Class<?>> getProjections(Repositories repositories) {

		Set<String> packagesToScan = new HashSet<String>();

		for (Class<?> domainType : repositories) {
			packagesToScan.add(domainType.getPackage().getName());
		}

		return new AnnotatedTypeScanner(Projection.class).findTypes(packagesToScan);
	}

	//
	// ALPS support
	//

	@Bean
	public RootResourceInformationToAlpsDescriptorConverter alpsConverter() {

		Repositories repositories = repositories();
		PersistentEntities persistentEntities = persistentEntities();
		RepositoryEntityLinks entityLinks = entityLinks();
		MessageSourceAccessor messageSourceAccessor = resourceDescriptionMessageSourceAccessor();
		RepositoryRestConfiguration config = config();
		ResourceMappings resourceMappings = resourceMappings();

		return new RootResourceInformationToAlpsDescriptorConverter(resourceMappings, repositories, persistentEntities,
				entityLinks, messageSourceAccessor, config, objectMapper());
	}

	@Bean
	public AlpsResourceProcessor alpsResourceProcessor() {
		return new AlpsResourceProcessor(config());
	}

	private static class ResourceSupportHttpMessageConverter extends TypeConstrainedMappingJackson2HttpMessageConverter
			implements Ordered {

		private final int order;

		/**
		 * Creates a new {@link ResourceSupportHttpMessageConverter} with the given order.
		 * 
		 * @param order the order for the {@link HttpMessageConverter}.
		 */
		public ResourceSupportHttpMessageConverter(int order) {
			super(ResourceSupport.class);
			this.order = order;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.Ordered#getOrder()
		 */
		@Override
		public int getOrder() {
			return order;
		}
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
