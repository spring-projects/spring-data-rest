/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.auditing.MappingAuditableBeanWrapperFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.geo.GeoModule;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.StringToLdapNameConverter;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.core.config.MetadataConfiguration;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.core.config.ProjectionDefinitionConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.AnnotatedEventHandlerInvoker;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.support.DefaultSelfLinkProvider;
import org.springframework.data.rest.core.support.EntityLookup;
import org.springframework.data.rest.core.support.RepositoryRelProvider;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.core.support.UnwrappingRepositoryInvokerFactory;
import org.springframework.data.rest.webmvc.*;
import org.springframework.data.rest.webmvc.alps.AlpsJsonHttpMessageConverter;
import org.springframework.data.rest.webmvc.alps.RootResourceInformationToAlpsDescriptorConverter;
import org.springframework.data.rest.webmvc.convert.UriListHttpMessageConverter;
import org.springframework.data.rest.webmvc.json.*;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module.LookupObjectSerializer;
import org.springframework.data.rest.webmvc.json.PersistentEntityToJsonSchemaConverter.ValueTypeSchemaPropertyCustomizerFactory;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter;
import org.springframework.data.rest.webmvc.support.*;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule.HalHandlerInstantiator;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Main application configuration for Spring Data REST. To customize how the exporter works, subclass this and override
 * any of the {@literal configure*} methods.
 * <p/>
 * Any XML files located in the classpath under the {@literal META-INF/spring-data-rest/} path will be automatically
 * found and loaded into this {@link org.springframework.context.ApplicationContext}.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Mark Paluch
 */
@Configuration
@EnableHypermediaSupport(type = HypermediaType.HAL)
@ComponentScan(basePackageClasses = RepositoryRestController.class,
		includeFilters = @Filter(BasePathAwareController.class), useDefaultFilters = false)
@ImportResource("classpath*:META-INF/spring-data-rest/**/*.xml")
@Import({ SpringDataJacksonConfiguration.class, EnableSpringDataWebSupport.QuerydslActivator.class })
public class RepositoryRestMvcConfiguration extends HateoasAwareSpringDataWebConfiguration
		implements InitializingBean, BeanClassLoaderAware {

	private static final boolean IS_JPA_AVAILABLE = ClassUtils.isPresent("javax.persistence.EntityManager",
			RepositoryRestMvcConfiguration.class.getClassLoader());

	@Autowired ApplicationContext applicationContext;

	@Autowired(required = false) List<BackendIdConverter> idConverters = Collections.emptyList();
	@Autowired(required = false) List<RepositoryRestConfigurer> configurers = Collections.emptyList();
	@Autowired(required = false) List<EntityLookup<?>> lookups = Collections.emptyList();

	@Autowired Optional<LinkRelationProvider> relProvider;
	@Autowired Optional<CurieProvider> curieProvider;
	@Autowired Optional<HalConfiguration> halConfiguration;
	@Autowired ObjectProvider<ObjectMapper> objectMapper;
	@Autowired ObjectProvider<RepresentationModelProcessorInvoker> invoker;
	@Autowired MessageResolver resolver;

	private final Lazy<ObjectMapper> mapper;

	private RepositoryRestConfigurerDelegate configurerDelegate;
	private ClassLoader beanClassLoader;

	public RepositoryRestMvcConfiguration(ApplicationContext context,
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {

		super(context, conversionService);

		this.mapper = Lazy.of(() -> {

			Jdk8Module jdk8Module = new Jdk8Module();
			jdk8Module.configureAbsentsAsNulls(true);

			ObjectMapper mapper = basicObjectMapper();
			mapper.registerModule(persistentEntityJackson2Module());
			mapper.registerModule(jdk8Module);

			return mapper;
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		this.configurerDelegate = new RepositoryRestConfigurerDelegate(configurers);
	}

	@Bean
	public Repositories repositories() {
		return new Repositories(applicationContext);
	}

	@Bean
	public RepositoryRelProvider repositoryRelProvider(ObjectFactory<ResourceMappings> resourceMappings) {
		return new RepositoryRelProvider(resourceMappings);
	}

	@Bean
	public PersistentEntities persistentEntities() {

		List<MappingContext<?, ?>> arrayList = new ArrayList<MappingContext<?, ?>>();

		for (MappingContext<?, ?> context : BeanFactoryUtils
				.beansOfTypeIncludingAncestors(applicationContext, MappingContext.class).values()) {
			arrayList.add(context);
		}

		return new PersistentEntities(arrayList);
	}

	@Bean
	@Qualifier
	public DefaultFormattingConversionService defaultConversionService() {

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		// Add Spring Data Commons formatters
		conversionService.addConverter(uriToEntityConverter(conversionService));
		conversionService.addConverter(StringToLdapNameConverter.INSTANCE);
		addFormatters(conversionService);

		configurerDelegate.configureConversionService(conversionService);

		return conversionService;
	}

	/**
	 * {@link org.springframework.context.ApplicationListener} implementation for invoking
	 * {@link org.springframework.validation.Validator} instances assigned to specific domain types.
	 */
	@Bean
	public ValidatingRepositoryEventListener validatingRepositoryEventListener(
			ObjectFactory<PersistentEntities> entities) {

		ValidatingRepositoryEventListener listener = new ValidatingRepositoryEventListener(entities);
		configurerDelegate.configureValidatingRepositoryEventListener(listener);

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
	public RepositoryRestConfiguration repositoryRestConfiguration() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();

		// Register projections found in packages
		for (Class<?> projection : getProjections(repositories())) {
			configuration.addProjection(projection);
		}

		RepositoryRestConfiguration config = new RepositoryRestConfiguration(configuration, metadataConfiguration(),
				enumTranslator());
		configurerDelegate.configureRepositoryRestConfiguration(config);

		return config;
	}

	@Bean
	public static ProjectionDefinitionRegistar projectionDefinitionRegistrar(
			ObjectFactory<RepositoryRestConfiguration> config) {
		return new ProjectionDefinitionRegistar(config);
	}

	@Bean
	public MetadataConfiguration metadataConfiguration() {
		return new MetadataConfiguration();
	}

	@Bean
	public BaseUri baseUri() {
		return new BaseUri(repositoryRestConfiguration().getBaseUri());
	}

	/**
	 * {@link org.springframework.beans.factory.config.BeanPostProcessor} to turn beans annotated as
	 * {@link org.springframework.data.rest.repository.annotation.RepositoryEventHandler}s.
	 *
	 * @return
	 */
	@Bean
	public static AnnotatedEventHandlerInvoker annotatedEventHandlerInvoker() {
		return new AnnotatedEventHandlerInvoker();
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

		if (QuerydslUtils.QUERY_DSL_PRESENT) {

			QuerydslBindingsFactory factory = applicationContext.getBean(QuerydslBindingsFactory.class);
			QuerydslPredicateBuilder predicateBuilder = new QuerydslPredicateBuilder(defaultConversionService(),
					factory.getEntityPathResolver());

			return new QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(repositories(),
					repositoryInvokerFactory(defaultConversionService()), resourceMetadataHandlerMethodArgumentResolver(),
					predicateBuilder, factory);
		}

		return new RootResourceInformationHandlerMethodArgumentResolver(repositories(),
				repositoryInvokerFactory(defaultConversionService()), resourceMetadataHandlerMethodArgumentResolver());
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

	@Bean
	public ETagArgumentResolver eTagArgumentResolver() {
		return new ETagArgumentResolver();
	}

	/**
	 * A special {@link org.springframework.hateoas.server.EntityLinks} implementation that takes repository and current
	 * configuration into account when generating links.
	 *
	 * @return
	 * @throws Exception
	 */
	@Bean
	public RepositoryEntityLinks entityLinks() {

		PagingAndSortingTemplateVariables templateVariables = new ArgumentResolverPagingAndSortingTemplateVariables(
				pageableResolver(), sortResolver());

		return new RepositoryEntityLinks(repositories(), resourceMappings(), repositoryRestConfiguration(),
				templateVariables, backendIdConverterRegistry());
	}

	/**
	 * Reads incoming JSON into an entity.
	 *
	 * @return
	 */
	@Bean
	public PersistentEntityResourceHandlerMethodArgumentResolver persistentEntityArgumentResolver() {

		PluginRegistry<EntityLookup<?>, Class<?>> lookups = PluginRegistry.of(getEntityLookups());

		return new PersistentEntityResourceHandlerMethodArgumentResolver(defaultMessageConverters(),
				repoRequestArgumentResolver(), backendIdHandlerMethodArgumentResolver(),
				new DomainObjectReader(persistentEntities(), associationLinks()), lookups);
	}

	/**
	 * Turns a domain class into a {@link org.springframework.data.rest.webmvc.json.JsonSchema}.
	 *
	 * @return
	 */
	@Bean
	public PersistentEntityToJsonSchemaConverter jsonSchemaConverter() {

		return new PersistentEntityToJsonSchemaConverter(persistentEntities(), associationLinks(), resolver, objectMapper(),
				repositoryRestConfiguration(),
				new ValueTypeSchemaPropertyCustomizerFactory(repositoryInvokerFactory(defaultConversionService())));
	}

	/**
	 * The Jackson {@link ObjectMapper} used internally.
	 *
	 * @return
	 */
	public ObjectMapper objectMapper() {
		return mapper.get();
	}

	/**
	 * The {@link HttpMessageConverter} used by Spring MVC to read and write JSON data.
	 *
	 * @return
	 */
	@Bean
	public TypeConstrainedMappingJackson2HttpMessageConverter jacksonHttpMessageConverter() {

		List<MediaType> mediaTypes = new ArrayList<MediaType>();

		// Configure this mapper to be used if HAL is not the default media type
		if (!repositoryRestConfiguration().useHalAsDefaultJsonMediaType()) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		int order = repositoryRestConfiguration().useHalAsDefaultJsonMediaType() ? Ordered.LOWEST_PRECEDENCE - 1
				: Ordered.LOWEST_PRECEDENCE - 10;

		mediaTypes.addAll(Arrays.asList(RestMediaTypes.SCHEMA_JSON, //
				RestMediaTypes.JSON_PATCH_JSON, RestMediaTypes.MERGE_PATCH_JSON, //
				RestMediaTypes.SPRING_DATA_VERBOSE_JSON, RestMediaTypes.SPRING_DATA_COMPACT_JSON));

		TypeConstrainedMappingJackson2HttpMessageConverter jacksonConverter = new ResourceSupportHttpMessageConverter(
				order);
		jacksonConverter.setObjectMapper(objectMapper());
		jacksonConverter.setSupportedMediaTypes(mediaTypes);

		return jacksonConverter;
	}

	//
	// HAL setup
	//

	@Bean
	public TypeConstrainedMappingJackson2HttpMessageConverter halJacksonHttpMessageConverter() {

		ArrayList<MediaType> mediaTypes = new ArrayList<MediaType>();
		mediaTypes.add(MediaTypes.HAL_JSON);

		// Enable returning HAL if application/json is asked if it's configured to be the default type
		if (repositoryRestConfiguration().useHalAsDefaultJsonMediaType()) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		int order = repositoryRestConfiguration().useHalAsDefaultJsonMediaType() ? Ordered.LOWEST_PRECEDENCE - 10
				: Ordered.LOWEST_PRECEDENCE - 1;

		TypeConstrainedMappingJackson2HttpMessageConverter converter = new ResourceSupportHttpMessageConverter(order);
		converter.setObjectMapper(halObjectMapper());
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	public ObjectMapper halObjectMapper() {

		LinkRelationProvider defaultedRelProvider = this.relProvider.orElseGet(EvoInflectorLinkRelationProvider::new);
		HalConfiguration halConfiguration = this.halConfiguration.orElseGet(HalConfiguration::new);

		HalHandlerInstantiator instantiator = new HalHandlerInstantiator(defaultedRelProvider,
				curieProvider.orElse(new DefaultCurieProvider(Collections.emptyMap())), resolver, halConfiguration);

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

		// Forward conversion service to handler adapter
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(defaultConversionService());

		RepositoryRestHandlerAdapter handlerAdapter = new RepositoryRestHandlerAdapter(defaultMethodArgumentResolvers());
		handlerAdapter.setWebBindingInitializer(initializer);
		handlerAdapter.setMessageConverters(defaultMessageConverters());

		if (repositoryRestConfiguration().getMetadataConfiguration().alpsEnabled()) {
			handlerAdapter.setResponseBodyAdvice(Arrays.<ResponseBodyAdvice<?>> asList(alpsJsonHttpMessageConverter()));
		}

		return handlerAdapter;
	}

	/**
	 * The {@link HandlerMapping} to delegate requests to Spring Data REST controllers. Sets up a
	 * {@link DelegatingHandlerMapping} to make sure manually implemented {@link BasePathAwareController} instances that
	 * register custom handlers for certain media types don't cause the {@link RepositoryRestHandlerMapping} to be
	 * omitted. See DATAREST-490.
	 *
	 * @return
	 */
	@Bean
	public DelegatingHandlerMapping restHandlerMapping() {

		Map<String, CorsConfiguration> corsConfigurations = repositoryRestConfiguration().getCorsRegistry()
				.getCorsConfigurations();

		RepositoryRestHandlerMapping repositoryMapping = new RepositoryRestHandlerMapping(resourceMappings(),
				repositoryRestConfiguration(), repositories());
		repositoryMapping.setJpaHelper(jpaHelper());
		repositoryMapping.setApplicationContext(applicationContext);
		repositoryMapping.setCorsConfigurations(corsConfigurations);
		repositoryMapping.afterPropertiesSet();

		BasePathAwareHandlerMapping basePathMapping = new BasePathAwareHandlerMapping(repositoryRestConfiguration());
		basePathMapping.setApplicationContext(applicationContext);
		basePathMapping.setCorsConfigurations(corsConfigurations);
		basePathMapping.afterPropertiesSet();

		List<HandlerMapping> mappings = new ArrayList<HandlerMapping>();
		mappings.add(basePathMapping);
		mappings.add(repositoryMapping);

		return new DelegatingHandlerMapping(mappings);
	}

	@Bean
	public RepositoryResourceMappings resourceMappings() {
		return new RepositoryResourceMappings(repositories(), persistentEntities(), repositoryRestConfiguration());
	}

	/**
	 * Jackson module responsible for intelligently serializing and deserializing JSON that corresponds to an entity.
	 *
	 * @return
	 */
	protected Module persistentEntityJackson2Module() {

		PersistentEntities entities = persistentEntities();
		ConversionService conversionService = defaultConversionService();

		UriToEntityConverter uriToEntityConverter = uriToEntityConverter(conversionService);
		RepositoryInvokerFactory repositoryInvokerFactory = repositoryInvokerFactory(conversionService);

		EmbeddedResourcesAssembler assembler = new EmbeddedResourcesAssembler(entities, associationLinks(),
				excerptProjector());
		LookupObjectSerializer lookupObjectSerializer = new LookupObjectSerializer(PluginRegistry.of(getEntityLookups()));

		return new PersistentEntityJackson2Module(associationLinks(), entities, uriToEntityConverter, linkCollector(),
				repositoryInvokerFactory, lookupObjectSerializer, invoker.getObject(), assembler);
	}

	@Bean
	protected LinkCollector linkCollector() {
		return new LinkCollector(persistentEntities(), selfLinkProvider(), associationLinks());
	}

	protected UriToEntityConverter uriToEntityConverter(ConversionService conversionService) {
		return new UriToEntityConverter(persistentEntities(), repositoryInvokerFactory(conversionService), repositories());
	}

	@Bean
	public ExcerptProjector excerptProjector() {

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
		projectionFactory.setBeanFactory(applicationContext);

		return new DefaultExcerptProjector(projectionFactory, resourceMappings());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#extendHandlerExceptionResolvers(java.util.List)
	 */
	@Override
	public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {

		ExceptionHandlerExceptionResolver er = new ExceptionHandlerExceptionResolver();
		er.setCustomArgumentResolvers(defaultMethodArgumentResolvers());
		er.setMessageConverters(defaultMessageConverters());

		configurerDelegate.configureExceptionHandlerExceptionResolver(er);

		er.afterPropertiesSet();

		exceptionResolvers.add(0, er);
	}

	@Bean
	public RepositoryRestExceptionHandler repositoryRestExceptionHandler() {
		return new RepositoryRestExceptionHandler(applicationContext);
	}

	@Bean
	public RepositoryInvokerFactory repositoryInvokerFactory(@Qualifier ConversionService defaultConversionService) {

		return new UnwrappingRepositoryInvokerFactory(
				new DefaultRepositoryInvokerFactory(repositories(), defaultConversionService), getEntityLookups());
	}

	@Bean
	public List<HttpMessageConverter<?>> defaultMessageConverters() {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

		if (repositoryRestConfiguration().getMetadataConfiguration().alpsEnabled()) {
			messageConverters.add(alpsJsonHttpMessageConverter());
		}

		if (repositoryRestConfiguration().getDefaultMediaType().equals(MediaTypes.HAL_JSON)) {
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

		configurerDelegate.configureHttpMessageConverters(messageConverters);

		return messageConverters;
	}

	@Bean
	public AlpsJsonHttpMessageConverter alpsJsonHttpMessageConverter() {
		return new AlpsJsonHttpMessageConverter(alpsConverter());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration#pageableResolver()
	 */
	@Bean
	@Override
	public HateoasPageableHandlerMethodArgumentResolver pageableResolver() {

		HateoasPageableHandlerMethodArgumentResolver resolver = super.pageableResolver();
		resolver.setPageParameterName(repositoryRestConfiguration().getPageParamName());
		resolver.setSizeParameterName(repositoryRestConfiguration().getLimitParamName());
		resolver.setFallbackPageable(PageRequest.of(0, repositoryRestConfiguration().getDefaultPageSize()));
		resolver.setMaxPageSize(repositoryRestConfiguration().getMaxPageSize());

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
		resolver.setSortParameter(repositoryRestConfiguration().getSortParamName());

		return resolver;
	}

	@Bean
	public PluginRegistry<BackendIdConverter, Class<?>> backendIdConverterRegistry() {

		List<BackendIdConverter> converters = new ArrayList<BackendIdConverter>(idConverters.size());
		converters.addAll(this.idConverters);
		converters.add(DefaultIdConverter.INSTANCE);

		return PluginRegistry.of(converters);
	}

	@Bean
	public AuditableBeanWrapperFactory auditableBeanWrapperFactory() {
		return new MappingAuditableBeanWrapperFactory(persistentEntities());
	}

	@Bean
	public HttpHeadersPreparer httpHeadersPreparer() {
		return new HttpHeadersPreparer(auditableBeanWrapperFactory());
	}

	@Bean
	public SelfLinkProvider selfLinkProvider() {
		return new DefaultSelfLinkProvider(persistentEntities(), entityLinks(), getEntityLookups());
	}

	@Bean
	public Associations associationLinks() {
		return new Associations(resourceMappings(), repositoryRestConfiguration());
	}

	protected List<EntityLookup<?>> getEntityLookups() {

		List<EntityLookup<?>> lookups = new ArrayList<EntityLookup<?>>();
		lookups.addAll(repositoryRestConfiguration().getEntityLookups(repositories()));
		lookups.addAll(this.lookups);

		return lookups;
	}

	protected List<HandlerMethodArgumentResolver> defaultMethodArgumentResolvers() {

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
		projectionFactory.setBeanFactory(applicationContext);
		projectionFactory.setBeanClassLoader(beanClassLoader);

		PersistentEntityResourceAssemblerArgumentResolver peraResolver = new PersistentEntityResourceAssemblerArgumentResolver(
				persistentEntities(), selfLinkProvider(), repositoryRestConfiguration().getProjectionConfiguration(),
				projectionFactory, associationLinks());

		PageableHandlerMethodArgumentResolver pageableResolver = pageableResolver();

		JacksonMappingAwareSortTranslator sortTranslator = new JacksonMappingAwareSortTranslator(objectMapper(),
				repositories(), DomainClassResolver.of(repositories(), resourceMappings(), baseUri()), persistentEntities(),
				associationLinks());

		HandlerMethodArgumentResolver sortResolver = new MappingAwareSortArgumentResolver(sortTranslator, sortResolver());
		HandlerMethodArgumentResolver jacksonPageableResolver = new MappingAwarePageableArgumentResolver(sortTranslator,
				pageableResolver);
		HandlerMethodArgumentResolver defaultedPageableResolver = new MappingAwareDefaultedPageableArgumentResolver(
				sortTranslator, pageableResolver);

		return Arrays.asList(defaultedPageableResolver, jacksonPageableResolver, sortResolver,
				serverHttpRequestMethodArgumentResolver(), repoRequestArgumentResolver(), persistentEntityArgumentResolver(),
				resourceMetadataHandlerMethodArgumentResolver(), HttpMethodHandlerMethodArgumentResolver.INSTANCE, peraResolver,
				backendIdHandlerMethodArgumentResolver(), eTagArgumentResolver());
	}

	@Autowired GeoModule geoModule;

	protected ObjectMapper basicObjectMapper() {

		ObjectMapper mapper = this.objectMapper.getIfAvailable();
		ObjectMapper objectMapper = mapper == null ? new ObjectMapper() : mapper.copy();

		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		// Configure custom Modules
		configurerDelegate.configureJacksonObjectMapper(objectMapper);

		objectMapper.registerModule(geoModule);

		if (repositoryRestConfiguration().isEnableEnumTranslation()) {
			objectMapper.registerModule(new JacksonSerializers(enumTranslator()));
		}

		Jackson2DatatypeHelper.configureObjectMapper(objectMapper);

		return objectMapper;
	}

	@Bean
	public EnumTranslator enumTranslator() {
		return new EnumTranslator(resolver);
	}

	private Set<Class<?>> getProjections(Repositories repositories) {

		Set<String> packagesToScan = new HashSet<>();

		for (Class<?> domainType : repositories) {
			packagesToScan.add(domainType.getPackage().getName());
		}

		AnnotatedTypeScanner scanner = new AnnotatedTypeScanner(Projection.class);
		scanner.setEnvironment(applicationContext.getEnvironment());
		scanner.setResourceLoader(applicationContext);

		return scanner.findTypes(packagesToScan);
	}

	//
	// ALPS support
	//

	@Bean
	public RootResourceInformationToAlpsDescriptorConverter alpsConverter() {

		Repositories repositories = repositories();
		PersistentEntities persistentEntities = persistentEntities();
		RepositoryEntityLinks entityLinks = entityLinks();
		RepositoryRestConfiguration config = repositoryRestConfiguration();

		return new RootResourceInformationToAlpsDescriptorConverter(associationLinks(), repositories, persistentEntities,
				entityLinks, resolver, config, objectMapper(), enumTranslator());
	}

	@Bean
	public ProfileResourceProcessor profileResourceProcessor(RepositoryRestConfiguration config) {
		return new ProfileResourceProcessor(config);
	}

	//
	// HAL Browser
	//

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry)
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		RepositoryRestConfiguration configuration = repositoryRestConfiguration();

		SpringFactoriesLoader.loadFactories(StaticResourceProvider.class, beanClassLoader)
				.forEach(it -> it.customizeResources(registry, configuration));
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
			super(RepresentationModel.class);
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
}
