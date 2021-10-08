/*
 * Copyright 2012-2021 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import org.springframework.data.rest.webmvc.mapping.DefaultLinkCollector;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter;
import org.springframework.data.rest.webmvc.support.BackendIdHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.support.DefaultExcerptProjector;
import org.springframework.data.rest.webmvc.support.DomainClassResolver;
import org.springframework.data.rest.webmvc.support.ETagArgumentResolver;
import org.springframework.data.rest.webmvc.support.ExcerptProjector;
import org.springframework.data.rest.webmvc.support.HttpMethodHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.data.rest.webmvc.support.PagingAndSortingTemplateVariables;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
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
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.Jackson2HalFormsModule;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
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
import org.springframework.web.util.pattern.PathPatternParser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Main application configuration for Spring Data REST. To customize how the exporter works, subclass this and override
 * any of the {@literal configure*} methods.
 * <p>
 * Any XML files located in the classpath under the {@literal META-INF/spring-data-rest/} path will be automatically
 * found and loaded into this {@link org.springframework.context.ApplicationContext}.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@Configuration(proxyBeanMethods = false)
@EnableHypermediaSupport(type = { HypermediaType.HAL, HypermediaType.HAL_FORMS })
@Import({ RestControllerImportSelector.class, //
		SpringDataJacksonConfiguration.class, //
		EnableSpringDataWebSupport.QuerydslActivator.class })
public class RepositoryRestMvcConfiguration extends HateoasAwareSpringDataWebConfiguration
		implements BeanClassLoaderAware {

	private static final boolean IS_JPA_AVAILABLE = ClassUtils.isPresent("jakarta.persistence.EntityManager",
			RepositoryRestMvcConfiguration.class.getClassLoader());

	private final ApplicationContext applicationContext;
	private final ConversionService defaultConversionService;

	private final ObjectProvider<LinkRelationProvider> relProvider;
	private final ObjectProvider<CurieProvider> curieProvider;
	private final ObjectProvider<HalConfiguration> halConfiguration;
	private final ObjectProvider<ObjectMapper> objectMapper;
	private final ObjectProvider<RepresentationModelProcessorInvoker> invoker;
	private final ObjectProvider<MessageResolver> resolver;
	private final ObjectProvider<GeoModule> geoModule;
	private final ObjectProvider<PathPatternParser> parser;

	private final Lazy<ObjectMapper> mapper;
	private final Lazy<? extends List<EntityLookup<?>>> lookups;
	private final Lazy<? extends List<HttpMessageConverter<?>>> defaultMessageConverters;
	private final Lazy<RepositoryRestConfigurerDelegate> configurerDelegate;
	private final Lazy<SelfLinkProvider> selfLinkProvider;
	private final Lazy<PersistentEntityResourceHandlerMethodArgumentResolver> persistentEntityArgumentResolver;
	private final Lazy<RootResourceInformationHandlerMethodArgumentResolver> repoRequestArgumentResolver;
	private final Lazy<BaseUri> baseUri;
	private final Lazy<RepositoryResourceMappings> resourceMappings;
	private final Lazy<Repositories> repositories;
	private final Lazy<ResourceMetadataHandlerMethodArgumentResolver> resourceMetadataHandlerMethodArgumentResolver;
	private final Lazy<ExcerptProjector> excerptProjector;
	private final Lazy<PersistentEntities> persistentEntities;
	private final Lazy<BackendIdHandlerMethodArgumentResolver> backendIdHandlerMethodArgumentResolver;
	private final Lazy<Associations> associationLinks;
	private final Lazy<EnumTranslator> enumTranslator;
	private final Lazy<ServerHttpRequestMethodArgumentResolver> serverHttpRequestMethodArgumentResolver;
	private final Lazy<ETagArgumentResolver> eTagArgumentResolver;
	private final Lazy<RepositoryInvokerFactory> repositoryInvokerFactory;
	private final Lazy<RepositoryRestConfiguration> repositoryRestConfiguration;
	private final Lazy<HateoasPageableHandlerMethodArgumentResolver> pageableResolver;
	private final Lazy<HateoasSortHandlerMethodArgumentResolver> sortResolver;

	private ClassLoader beanClassLoader;

	public RepositoryRestMvcConfiguration( //
			ApplicationContext context, //
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService, //
			ObjectProvider<LinkRelationProvider> relProvider, //
			ObjectProvider<CurieProvider> curieProvider, //
			ObjectProvider<HalConfiguration> halConfiguration, //
			ObjectProvider<ObjectMapper> objectMapper, //
			ObjectProvider<RepresentationModelProcessorInvoker> invoker, //
			ObjectProvider<MessageResolver> resolver, //
			ObjectProvider<GeoModule> geoModule, //
			ObjectProvider<PathPatternParser> parser) {

		super(context, conversionService);

		this.applicationContext = context;
		this.relProvider = relProvider;
		this.curieProvider = curieProvider;
		this.halConfiguration = halConfiguration;
		this.objectMapper = objectMapper;
		this.invoker = invoker;
		this.resolver = resolver;
		this.geoModule = geoModule;
		this.parser = parser;
		this.defaultConversionService = new DefaultFormattingConversionService();

		this.mapper = Lazy.of(() -> {

			Jdk8Module jdk8Module = new Jdk8Module();
			jdk8Module.configureAbsentsAsNulls(true);

			ObjectMapper mapper = basicObjectMapper();

			LinkCollector linkCollector = context.getBean(LinkCollector.class);

			mapper.registerModule(persistentEntityJackson2Module(linkCollector));
			mapper.registerModule(jdk8Module);

			return mapper;
		});

		this.selfLinkProvider = Lazy.of(() -> context.getBean(SelfLinkProvider.class));
		this.persistentEntityArgumentResolver = Lazy
				.of(() -> context.getBean(PersistentEntityResourceHandlerMethodArgumentResolver.class));
		this.repoRequestArgumentResolver = Lazy
				.of(() -> context.getBean(RootResourceInformationHandlerMethodArgumentResolver.class));
		this.baseUri = Lazy.of(() -> context.getBean(BaseUri.class));
		this.resourceMappings = Lazy.of(() -> context.getBean(RepositoryResourceMappings.class));
		this.repositories = Lazy.of(() -> context.getBean(Repositories.class));
		this.resourceMetadataHandlerMethodArgumentResolver = Lazy
				.of(() -> context.getBean(ResourceMetadataHandlerMethodArgumentResolver.class));
		this.excerptProjector = Lazy.of(() -> context.getBean(ExcerptProjector.class));
		this.persistentEntities = Lazy.of(() -> context.getBean(PersistentEntities.class));
		this.backendIdHandlerMethodArgumentResolver = Lazy
				.of(() -> context.getBean(BackendIdHandlerMethodArgumentResolver.class));
		this.associationLinks = Lazy.of(() -> context.getBean(Associations.class));
		this.enumTranslator = Lazy.of(() -> context.getBean(EnumTranslator.class));
		this.serverHttpRequestMethodArgumentResolver = Lazy
				.of(() -> context.getBean(ServerHttpRequestMethodArgumentResolver.class));
		this.eTagArgumentResolver = Lazy.of(() -> context.getBean(ETagArgumentResolver.class));

		this.repositoryInvokerFactory = Lazy.of(() -> new UnwrappingRepositoryInvokerFactory(
				new DefaultRepositoryInvokerFactory(repositories.get(), defaultConversionService), getEntityLookups()));

		this.configurerDelegate = Lazy.of(() -> {

			return new RepositoryRestConfigurerDelegate(context.getBeanProvider(RepositoryRestConfigurer.class)
					.orderedStream()
					.collect(Collectors.toList()));
		});

		this.repositoryRestConfiguration = Lazy.of(() -> context.getBean(RepositoryRestConfiguration.class));
		this.pageableResolver = Lazy.of(() -> context.getBean(HateoasPageableHandlerMethodArgumentResolver.class));
		this.sortResolver = Lazy.of(() -> context.getBean(HateoasSortHandlerMethodArgumentResolver.class));

		// Resolution via ResolvableType needed to make the wildcard assignment work

		this.lookups = beansOfType(context, EntityLookup.class);
		this.defaultMessageConverters = beansOfType(context, HttpMessageConverter.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
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

		List<MappingContext<?, ?>> arrayList = new ArrayList<>();

		for (MappingContext<?, ?> context : BeanFactoryUtils
				.beansOfTypeIncludingAncestors(applicationContext, MappingContext.class).values()) {
			arrayList.add(context);
		}

		return new PersistentEntities(arrayList);
	}

	@Bean
	@Qualifier
	public DefaultFormattingConversionService defaultConversionService(PersistentEntities persistentEntities,
			RepositoryInvokerFactory repositoryInvokerFactory, Repositories repositories) {

		DefaultFormattingConversionService conversionService = (DefaultFormattingConversionService) defaultConversionService;

		// Add Spring Data Commons formatters
		conversionService
				.addConverter(new UriToEntityConverter(persistentEntities, repositoryInvokerFactory, repositories));
		conversionService.addConverter(StringToLdapNameConverter.INSTANCE);
		addFormatters(conversionService);

		configurerDelegate.get().configureConversionService(conversionService);

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
		configurerDelegate.get().configureValidatingRepositoryEventListener(listener);

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
	@SuppressWarnings("unchecked")
	public <T extends RepositoryRestConfiguration & CorsConfigurationAware> T repositoryRestConfiguration() {

		ProjectionDefinitionConfiguration configuration = new ProjectionDefinitionConfiguration();

		// Register projections found in packages
		for (Class<?> projection : getProjections(repositories.get())) {
			configuration.addProjection(projection);
		}

		RepositoryCorsRegistry registry = new RepositoryCorsRegistry();

		WebMvcRepositoryRestConfiguration config = new WebMvcRepositoryRestConfiguration(configuration,
				new MetadataConfiguration(), new EnumTranslator(MessageResolver.DEFAULTS_ONLY), registry);

		configurerDelegate.get().configureRepositoryRestConfiguration(config, registry);

		return (T) config;
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
	public BaseUri baseUri(RepositoryRestConfiguration repositoryRestConfiguration) {
		return new BaseUri(repositoryRestConfiguration.getBasePath());
	}

	@Bean
	public static AnnotatedEventHandlerInvoker annotatedEventHandlerInvoker() {
		return new AnnotatedEventHandlerInvoker();
	}

	/**
	 * Turns an {@link HttpServletRequest} into a {@link ServerHttpRequest}.
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
	public RootResourceInformationHandlerMethodArgumentResolver repoRequestArgumentResolver(Repositories repositories,
			ResourceMetadataHandlerMethodArgumentResolver resourceMetadataHandlerMethodArgumentResolver,
			@Qualifier RepositoryInvokerFactory repositoryInvokerFactory) {

		if (QuerydslUtils.QUERY_DSL_PRESENT) {

			QuerydslBindingsFactory factory = applicationContext.getBean(QuerydslBindingsFactory.class);
			QuerydslPredicateBuilder predicateBuilder = new QuerydslPredicateBuilder(defaultConversionService,
					factory.getEntityPathResolver());

			return new QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(repositories,
					repositoryInvokerFactory, resourceMetadataHandlerMethodArgumentResolver, predicateBuilder, factory);
		}

		return new RootResourceInformationHandlerMethodArgumentResolver(repositories, repositoryInvokerFactory,
				resourceMetadataHandlerMethodArgumentResolver);
	}

	@Bean
	public ResourceMetadataHandlerMethodArgumentResolver resourceMetadataHandlerMethodArgumentResolver(
			Repositories repositories, RepositoryResourceMappings resourceMappings, BaseUri baseUri) {
		return new ResourceMetadataHandlerMethodArgumentResolver(repositories, resourceMappings, baseUri);
	}

	@Bean
	public BackendIdHandlerMethodArgumentResolver backendIdHandlerMethodArgumentResolver(
			PluginRegistry<BackendIdConverter, Class<?>> backendIdConverterRegistry,
			ResourceMetadataHandlerMethodArgumentResolver resourceMetadataHandlerMethodArgumentResolver, BaseUri baseUri) {

		return new BackendIdHandlerMethodArgumentResolver(backendIdConverterRegistry,
				resourceMetadataHandlerMethodArgumentResolver, baseUri);
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
	 */
	@Bean
	public RepositoryEntityLinks entityLinks(ObjectFactory<HateoasPageableHandlerMethodArgumentResolver> pageableResolver, //
			Repositories repositories, //
			RepositoryResourceMappings resourceMappings, //
			PluginRegistry<BackendIdConverter, //
					Class<?>> backendIdConverterRegistry, //
			RepositoryRestConfiguration repositoryRestConfiguration, //
			ObjectFactory<HateoasSortHandlerMethodArgumentResolver> sortResolver) {

		Lazy<PagingAndSortingTemplateVariables> templateVariables = Lazy
				.of(() -> new ArgumentResolverPagingAndSortingTemplateVariables(pageableResolver.getObject(),
						sortResolver.getObject()));

		return new RepositoryEntityLinks(repositories, resourceMappings, repositoryRestConfiguration, templateVariables,
				backendIdConverterRegistry);
	}

	/**
	 * Reads incoming JSON into an entity.
	 *
	 * @return
	 */
	@Bean
	public PersistentEntityResourceHandlerMethodArgumentResolver persistentEntityArgumentResolver(
			@Qualifier("defaultMessageConverters") List<HttpMessageConverter<?>> defaultMessageConverters,
			RootResourceInformationHandlerMethodArgumentResolver repoRequestArgumentResolver, Associations associationLinks,
			BackendIdHandlerMethodArgumentResolver backendIdHandlerMethodArgumentResolver,
			PersistentEntities persistentEntities) {

		PluginRegistry<EntityLookup<?>, Class<?>> lookups = PluginRegistry.of(getEntityLookups());

		return new PersistentEntityResourceHandlerMethodArgumentResolver(defaultMessageConverters,
				repoRequestArgumentResolver, backendIdHandlerMethodArgumentResolver,
				new DomainObjectReader(persistentEntities, associationLinks), lookups);
	}

	/**
	 * Turns a domain class into a {@link org.springframework.data.rest.webmvc.json.JsonSchema}.
	 *
	 * @return
	 */
	@Bean
	public PersistentEntityToJsonSchemaConverter jsonSchemaConverter(PersistentEntities persistentEntities,
			Associations associationLinks, @Qualifier RepositoryInvokerFactory repositoryInvokerFactory,
			RepositoryRestConfiguration repositoryRestConfiguration) {

		return new PersistentEntityToJsonSchemaConverter(persistentEntities, associationLinks, resolver.getObject(),
				objectMapper(),
				repositoryRestConfiguration, new ValueTypeSchemaPropertyCustomizerFactory(repositoryInvokerFactory));
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
	public TypeConstrainedMappingJackson2HttpMessageConverter jacksonHttpMessageConverter(
			RepositoryRestConfiguration repositoryRestConfiguration) {

		List<MediaType> mediaTypes = new ArrayList<>();

		// Configure this mapper to be used if HAL is not the default media type
		if (!repositoryRestConfiguration.useHalAsDefaultJsonMediaType()) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		int order = repositoryRestConfiguration.useHalAsDefaultJsonMediaType() ? Ordered.LOWEST_PRECEDENCE - 1
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
	public TypeConstrainedMappingJackson2HttpMessageConverter halJacksonHttpMessageConverter(LinkCollector linkCollector,
			RepositoryRestConfiguration repositoryRestConfiguration) {

		ArrayList<MediaType> mediaTypes = new ArrayList<>();
		mediaTypes.add(MediaTypes.HAL_JSON);

		// Enable returning HAL if application/json is asked if it's configured to be the default type
		if (repositoryRestConfiguration.useHalAsDefaultJsonMediaType()) {
			mediaTypes.add(MediaType.APPLICATION_JSON);
		}

		int order = repositoryRestConfiguration.useHalAsDefaultJsonMediaType() ? Ordered.LOWEST_PRECEDENCE - 10
				: Ordered.LOWEST_PRECEDENCE - 1;

		TypeConstrainedMappingJackson2HttpMessageConverter converter = new ResourceSupportHttpMessageConverter(order);
		converter.setObjectMapper(halObjectMapper(linkCollector));
		converter.setSupportedMediaTypes(mediaTypes);

		return converter;
	}

	/**
	 * {@link HttpMessageConverter} to support rendering HAL FORMS.
	 *
	 * @param linkCollector
	 * @return
	 * @since 3.5
	 */
	@Bean
	TypeConstrainedMappingJackson2HttpMessageConverter halFormsJacksonHttpMessageConverter(LinkCollector linkCollector) {

		LinkRelationProvider defaultedRelProvider = this.relProvider.getIfUnique(EvoInflectorLinkRelationProvider::new);
		HalFormsConfiguration configuration = new HalFormsConfiguration(
				halConfiguration.getIfUnique(() -> new HalConfiguration()));
		CurieProvider curieProvider = this.curieProvider
				.getIfUnique(() -> new DefaultCurieProvider(Collections.emptyMap()));
		ObjectMapper mapper = basicObjectMapper();

		mapper.registerModule(persistentEntityJackson2Module(linkCollector));
		mapper.registerModule(new Jackson2HalFormsModule());
		mapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(
				defaultedRelProvider, curieProvider, resolver.getObject(), configuration.getHalConfiguration(),
				applicationContext.getAutowireCapableBeanFactory()));

		TypeConstrainedMappingJackson2HttpMessageConverter converter = new TypeConstrainedMappingJackson2HttpMessageConverter(
				RepresentationModel.class);
		converter.setSupportedMediaTypes(Collections.singletonList(MediaTypes.HAL_FORMS_JSON));
		converter.setObjectMapper(mapper);

		return converter;
	}

	public ObjectMapper halObjectMapper(LinkCollector linkCollector) {

		LinkRelationProvider defaultedRelProvider = this.relProvider.getIfUnique(EvoInflectorLinkRelationProvider::new);
		HalConfiguration halConfiguration = this.halConfiguration.getIfUnique(HalConfiguration::new);
		CurieProvider curieProvider = this.curieProvider
				.getIfUnique(() -> new DefaultCurieProvider(Collections.emptyMap()));
		HalHandlerInstantiator instantiator = new HalHandlerInstantiator(defaultedRelProvider, curieProvider,
				resolver.getObject(), halConfiguration, applicationContext.getAutowireCapableBeanFactory());

		ObjectMapper mapper = basicObjectMapper();
		mapper.registerModule(persistentEntityJackson2Module(linkCollector));
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
	 * @return
	 */
	@Bean
	public RequestMappingHandlerAdapter repositoryExporterHandlerAdapter(
			@Qualifier("defaultMessageConverters") List<HttpMessageConverter<?>> defaultMessageConverters,
			AlpsJsonHttpMessageConverter alpsJsonHttpMessageConverter, SelfLinkProvider selfLinkProvider,
			PersistentEntityResourceHandlerMethodArgumentResolver persistentEntityArgumentResolver,
			RootResourceInformationHandlerMethodArgumentResolver repoRequestArgumentResolver,
			RepositoryRestConfiguration repositoryRestConfiguration) {

		// Forward conversion service to handler adapter
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(defaultConversionService);

		RepositoryRestHandlerAdapter handlerAdapter = new RepositoryRestHandlerAdapter(defaultMethodArgumentResolvers(
				selfLinkProvider, persistentEntityArgumentResolver, repoRequestArgumentResolver));
		handlerAdapter.setWebBindingInitializer(initializer);
		handlerAdapter.setMessageConverters(defaultMessageConverters);

		List<ResponseBodyAdvice<?>> advices = new ArrayList<>();
		advices.add(new HalFormsAdaptingResponseBodyAdvice<>());

		if (repositoryRestConfiguration.getMetadataConfiguration().alpsEnabled()) {
			advices.addAll(Arrays.asList(alpsJsonHttpMessageConverter));
		}

		handlerAdapter.setResponseBodyAdvice(advices);

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
	public DelegatingHandlerMapping restHandlerMapping(Repositories repositories,
			RepositoryResourceMappings resourceMappings, Optional<JpaHelper> jpaHelper,
			RepositoryRestConfiguration repositoryRestConfiguration, CorsConfigurationAware corsRestConfiguration) {

		Map<String, CorsConfiguration> corsConfigurations = corsRestConfiguration.getCorsConfigurations();
		PathPatternParser parser = this.parser.getIfAvailable();

		RepositoryRestHandlerMapping repositoryMapping = new RepositoryRestHandlerMapping(resourceMappings,
				repositoryRestConfiguration, repositories);
		repositoryMapping.setJpaHelper(jpaHelper.orElse(null));
		repositoryMapping.setApplicationContext(applicationContext);
		repositoryMapping.setCorsConfigurations(corsConfigurations);
		repositoryMapping.setPatternParser(parser);
		repositoryMapping.afterPropertiesSet();

		BasePathAwareHandlerMapping basePathMapping = new BasePathAwareHandlerMapping(repositoryRestConfiguration);
		basePathMapping.setApplicationContext(applicationContext);
		basePathMapping.setCorsConfigurations(corsConfigurations);
		basePathMapping.setPatternParser(parser);
		basePathMapping.afterPropertiesSet();

		List<HandlerMapping> mappings = new ArrayList<>();
		mappings.add(basePathMapping);
		mappings.add(repositoryMapping);

		return new DelegatingHandlerMapping(mappings, parser);
	}

	@Bean
	public RepositoryResourceMappings resourceMappings(Repositories repositories, PersistentEntities persistentEntities,
			RepositoryRestConfiguration repositoryRestConfiguration) {
		return new RepositoryResourceMappings(repositories, persistentEntities, repositoryRestConfiguration);
	}

	/**
	 * Jackson module responsible for intelligently serializing and deserializing JSON that corresponds to an entity.
	 *
	 * @return
	 */
	protected Module persistentEntityJackson2Module(LinkCollector linkCollector) {

		EmbeddedResourcesAssembler assembler = new EmbeddedResourcesAssembler(persistentEntities.get(),
				associationLinks.get(), excerptProjector.get());
		LookupObjectSerializer lookupObjectSerializer = new LookupObjectSerializer(PluginRegistry.of(getEntityLookups()));

		return new PersistentEntityJackson2Module(associationLinks.get(), persistentEntities.get(),
				new UriToEntityConverter(persistentEntities.get(), repositoryInvokerFactory.get(), repositories.get()),
				linkCollector, repositoryInvokerFactory.get(), lookupObjectSerializer, invoker.getObject(), assembler);
	}

	@Bean
	protected LinkCollector linkCollector(PersistentEntities persistentEntities, SelfLinkProvider selfLinkProvider,
			Associations associationLinks) {

		return configurerDelegate.get()
				.customizeLinkCollector(new DefaultLinkCollector(persistentEntities, selfLinkProvider, associationLinks));
	}

	@Bean
	public ExcerptProjector excerptProjector(RepositoryResourceMappings resourceMappings) {

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
		projectionFactory.setBeanFactory(applicationContext);

		return new DefaultExcerptProjector(projectionFactory, resourceMappings);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#extendHandlerExceptionResolvers(java.util.List)
	 */
	@Override
	public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {

		ExceptionHandlerExceptionResolver er = new ExceptionHandlerExceptionResolver();
		er.setCustomArgumentResolvers(defaultMethodArgumentResolvers(selfLinkProvider.get(),
				persistentEntityArgumentResolver.get(), repoRequestArgumentResolver.get()));
		er.setMessageConverters(defaultMessageConverters.get());

		configurerDelegate.get().configureExceptionHandlerExceptionResolver(er);

		er.afterPropertiesSet();

		exceptionResolvers.add(0, er);
	}

	@Bean
	public RepositoryRestExceptionHandler repositoryRestExceptionHandler() {
		return new RepositoryRestExceptionHandler(applicationContext);
	}

	@Bean
	@Qualifier
	public RepositoryInvokerFactory repositoryInvokerFactory() {
		return new UnwrappingRepositoryInvokerFactory(
				new DefaultRepositoryInvokerFactory(repositories.get(), defaultConversionService), getEntityLookups());
	}

	@Bean
	public List<HttpMessageConverter<?>> defaultMessageConverters(
			@Qualifier("jacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter jacksonHttpMessageConverter,
			@Qualifier("halJacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter halJacksonHttpMessageConverter,
			@Qualifier("halFormsJacksonHttpMessageConverter") TypeConstrainedMappingJackson2HttpMessageConverter halFormsJacksonHttpMessageConverter,
			AlpsJsonHttpMessageConverter alpsJsonHttpMessageConverter,
			UriListHttpMessageConverter uriListHttpMessageConverter,
			RepositoryRestConfiguration repositoryRestConfiguration) {

		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

		if (repositoryRestConfiguration.getMetadataConfiguration().alpsEnabled()) {
			messageConverters.add(alpsJsonHttpMessageConverter);
		}

		if (repositoryRestConfiguration.getDefaultMediaType().equals(MediaTypes.HAL_JSON)) {
			messageConverters.add(halJacksonHttpMessageConverter);
			messageConverters.add(jacksonHttpMessageConverter);
		} else {
			messageConverters.add(jacksonHttpMessageConverter);
			messageConverters.add(halJacksonHttpMessageConverter);
		}

		messageConverters.add(halFormsJacksonHttpMessageConverter);

		MappingJackson2HttpMessageConverter fallbackJsonConverter = new MappingJackson2HttpMessageConverter();
		fallbackJsonConverter.setObjectMapper(basicObjectMapper());

		messageConverters.add(fallbackJsonConverter);
		messageConverters.add(uriListHttpMessageConverter);

		configurerDelegate.get().configureHttpMessageConverters(messageConverters);

		return messageConverters;
	}

	@Bean
	public AlpsJsonHttpMessageConverter alpsJsonHttpMessageConverter(
			RootResourceInformationToAlpsDescriptorConverter alpsConverter) {
		return new AlpsJsonHttpMessageConverter(alpsConverter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration#pageableResolver()
	 */
	@Bean
	@Override
	public HateoasPageableHandlerMethodArgumentResolver pageableResolver() {

		HateoasPageableHandlerMethodArgumentResolver resolver = super.pageableResolver();

		resolver.setPageParameterName(repositoryRestConfiguration.get().getPageParamName());
		resolver.setSizeParameterName(repositoryRestConfiguration.get().getLimitParamName());
		resolver.setFallbackPageable(PageRequest.of(0, repositoryRestConfiguration.get().getDefaultPageSize()));
		resolver.setMaxPageSize(repositoryRestConfiguration.get().getMaxPageSize());

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
		resolver.setSortParameter(repositoryRestConfiguration.get().getSortParamName());

		return resolver;
	}

	@Bean
	public PluginRegistry<BackendIdConverter, Class<?>> backendIdConverterRegistry(
			List<BackendIdConverter> backendIdConverter) {

		List<BackendIdConverter> converters = new ArrayList<>(backendIdConverter);
		converters.add(DefaultIdConverter.INSTANCE);

		return PluginRegistry.of(converters);
	}

	@Bean
	public AuditableBeanWrapperFactory auditableBeanWrapperFactory(PersistentEntities persistentEntities) {

		AuditableBeanWrapperFactory factory = new MappingAuditableBeanWrapperFactory(persistentEntities);

		return configurerDelegate.get().customizeAuditableBeanWrapperFactory(factory);
	}

	@Bean
	public HttpHeadersPreparer httpHeadersPreparer(AuditableBeanWrapperFactory auditableBeanWrapperFactory) {
		return new HttpHeadersPreparer(auditableBeanWrapperFactory);
	}

	@Bean
	public SelfLinkProvider selfLinkProvider(PersistentEntities persistentEntities, RepositoryEntityLinks entityLinks,
			@Qualifier("mvcConversionService") ObjectProvider<ConversionService> conversionService) {
		return new DefaultSelfLinkProvider(persistentEntities, entityLinks, getEntityLookups(),
				conversionService.getIfUnique(() -> defaultConversionService));
	}

	@Bean
	public Associations associationLinks(RepositoryResourceMappings resourceMappings,
			RepositoryRestConfiguration repositoryRestConfiguration) {
		return new Associations(resourceMappings, repositoryRestConfiguration);
	}

	protected List<EntityLookup<?>> getEntityLookups() {

		List<EntityLookup<?>> lookups = new ArrayList<>();
		lookups.addAll(repositoryRestConfiguration.get().getEntityLookups(repositories.get()));
		lookups.addAll(this.lookups.get());

		return lookups;
	}

	protected List<HandlerMethodArgumentResolver> defaultMethodArgumentResolvers(SelfLinkProvider selfLinkProvider,
			PersistentEntityResourceHandlerMethodArgumentResolver persistentEntityArgumentResolver,
			RootResourceInformationHandlerMethodArgumentResolver repoRequestArgumentResolver) {

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();
		projectionFactory.setBeanFactory(applicationContext);
		projectionFactory.setBeanClassLoader(beanClassLoader);

		JacksonMappingAwareSortTranslator sortTranslator = new JacksonMappingAwareSortTranslator(objectMapper(),
				repositories.get(), DomainClassResolver.of(repositories.get(), resourceMappings.get(), baseUri.get()),
				persistentEntities.get(), associationLinks.get());

		return Arrays.asList( //
				new MappingAwareDefaultedPageableArgumentResolver(sortTranslator, pageableResolver.get()), //
				new MappingAwarePageableArgumentResolver(sortTranslator, pageableResolver.get()), //
				new MappingAwareSortArgumentResolver(sortTranslator, this.sortResolver.get()), //
				serverHttpRequestMethodArgumentResolver.get(), //
				repoRequestArgumentResolver, //
				persistentEntityArgumentResolver, //
				resourceMetadataHandlerMethodArgumentResolver.get(), //
				HttpMethodHandlerMethodArgumentResolver.INSTANCE, //
				new PersistentEntityResourceAssemblerArgumentResolver(persistentEntities.get(), selfLinkProvider,
						repositoryRestConfiguration.get().getProjectionConfiguration(), projectionFactory, associationLinks.get()), //
				backendIdHandlerMethodArgumentResolver.get(), //
				eTagArgumentResolver.get());
	}

	protected ObjectMapper basicObjectMapper() {

		ObjectMapper mapper = this.objectMapper.getIfAvailable();
		ObjectMapper objectMapper = mapper == null ? new ObjectMapper() : mapper.copy();

		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		// Configure custom Modules
		configurerDelegate.get().configureJacksonObjectMapper(objectMapper);

		objectMapper.registerModule(geoModule.getObject());
		objectMapper.registerModule(new AggregateReferenceResolvingModule(
				new UriToEntityConverter(persistentEntities.get(), repositoryInvokerFactory.get(), repositories.get()),
				resourceMappings.get()));

		if (repositoryRestConfiguration.get().isEnableEnumTranslation()) {
			objectMapper.registerModule(new JacksonSerializers(enumTranslator.get()));
		}

		Jackson2DatatypeHelper.configureObjectMapper(objectMapper);

		return objectMapper;
	}

	@Bean
	public EnumTranslator enumTranslator(MessageResolver resolver) {
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
	public RootResourceInformationToAlpsDescriptorConverter alpsConverter(Repositories repositories,
			PersistentEntities persistentEntities, RepositoryEntityLinks entityLinks, EnumTranslator enumTranslator,
			Associations associationLinks, RepositoryRestConfiguration repositoryRestConfiguration) {

		return new RootResourceInformationToAlpsDescriptorConverter(associationLinks, repositories, persistentEntities,
				entityLinks, resolver.getObject(), repositoryRestConfiguration, objectMapper(), enumTranslator);
	}

	@Bean
	public ProfileResourceProcessor profileResourceProcessor(RepositoryRestConfiguration repositoryRestConfiguration) {
		return new ProfileResourceProcessor(repositoryRestConfiguration);
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

		SpringFactoriesLoader.loadFactories(StaticResourceProvider.class, beanClassLoader)
				.forEach(it -> it.customizeResources(registry, repositoryRestConfiguration.get()));
	}

	/**
	 * Helper to be able to obtain a {@link List} of generic types. Otherwise the assignment from {@code Foo} to
	 * {@code Foo<?>} doesn't work.
	 *
	 * @param <S>
	 * @param context
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static <S> Lazy<List<S>> beansOfType(ApplicationContext context, Class<?> type) {

		return Lazy.of(() -> (List<S>) context.getBeanProvider(type)
				.orderedStream()
				.collect(StreamUtils.toUnmodifiableList()));
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
