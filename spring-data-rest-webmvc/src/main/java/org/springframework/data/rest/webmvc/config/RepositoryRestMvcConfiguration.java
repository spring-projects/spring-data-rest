package org.springframework.data.rest.webmvc.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.convert.ISO8601DateConverter;
import org.springframework.data.rest.convert.UUIDConverter;
import org.springframework.data.rest.repository.UriDomainClassConverter;
import org.springframework.data.rest.repository.context.AnnotatedHandlerBeanPostProcessor;
import org.springframework.data.rest.repository.context.RepositoriesFactoryBean;
import org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener;
import org.springframework.data.rest.repository.json.PersistentEntityJackson2Module;
import org.springframework.data.rest.repository.json.PersistentEntityToJsonSchemaConverter;
import org.springframework.data.rest.repository.support.DomainObjectMerger;
import org.springframework.data.rest.webmvc.BaseUriMethodArgumentResolver;
import org.springframework.data.rest.webmvc.PagingAndSortingMethodArgumentResolver;
import org.springframework.data.rest.webmvc.PersistentEntityResourceHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.RepositoryController;
import org.springframework.data.rest.webmvc.RepositoryEntityController;
import org.springframework.data.rest.webmvc.RepositoryEntityLinksMethodArgumentResolver;
import org.springframework.data.rest.webmvc.RepositoryInformationHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.RepositoryPropertyReferenceController;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerAdapter;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.data.rest.webmvc.RepositoryRestRequestHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.RepositorySearchController;
import org.springframework.data.rest.webmvc.ServerHttpRequestMethodArgumentResolver;
import org.springframework.data.rest.webmvc.convert.JsonpResponseHttpMessageConverter;
import org.springframework.data.rest.webmvc.convert.UriListHttpMessageConverter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

/**
 * Main application configuration for Spring Data REST. To customize how the exporter works, subclass this and override
 * any of the {@literal configure*} methods.
 * <p/>
 * Any XML files located in the classpath under the {@literal META-INF/spring-data-rest/} path will be automatically
 * found and loaded into this {@link org.springframework.context.ApplicationContext}.
 *
 * @author Jon Brisbin
 */
@Configuration
@ImportResource("classpath*:META-INF/spring-data-rest/**/*.xml")
public class RepositoryRestMvcConfiguration {

  private static final boolean IS_HIBERNATE4_MODULE_AVAILABLE = ClassUtils.isPresent(
      "com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module",
      RepositoryRestMvcConfiguration.class.getClassLoader()
  );
  private static final boolean IS_JODA_MODULE_AVAILABLE       = ClassUtils.isPresent(
      "com.fasterxml.jackson.datatype.joda.JodaModule",
      RepositoryRestMvcConfiguration.class.getClassLoader()
  );

  @Bean public RepositoriesFactoryBean repositories() {
    return new RepositoriesFactoryBean();
  }

  @Bean public DefaultFormattingConversionService defaultConversionService() {
    DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
    conversionService.addConverter(UUIDConverter.INSTANCE);
    conversionService.addConverter(ISO8601DateConverter.INSTANCE);
    configureConversionService(conversionService);
    return conversionService;
  }

  @Bean public DomainClassConverter<?> domainClassConverter() {
    return new DomainClassConverter<DefaultFormattingConversionService>(defaultConversionService());
  }

  @Bean public UriDomainClassConverter uriDomainClassConverter() {
    return new UriDomainClassConverter();
  }

  /**
   * {@link org.springframework.context.ApplicationListener} implementation for invoking {@link
   * org.springframework.validation.Validator} instances assigned to specific domain types.
   */
  @Bean public ValidatingRepositoryEventListener validatingRepositoryEventListener() {
    ValidatingRepositoryEventListener listener = new ValidatingRepositoryEventListener();
    configureValidatingRepositoryEventListener(listener);
    return listener;
  }

  /**
   * Main configuration for the REST exporter.
   */
  @Bean public RepositoryRestConfiguration config() {
    RepositoryRestConfiguration config = new RepositoryRestConfiguration();
    configureRepositoryRestConfiguration(config);
    return config;
  }

  /**
   * For getting access to the {@link javax.persistence.EntityManagerFactory}.
   *
   * @return
   */
  @Bean public PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

  /**
   * {@link org.springframework.beans.factory.config.BeanPostProcessor} to turn beans annotated as {@link
   * org.springframework.data.rest.repository.annotation.RepositoryEventHandler}s.
   *
   * @return
   */
  @Bean public AnnotatedHandlerBeanPostProcessor annotatedHandlerBeanPostProcessor() {
    return new AnnotatedHandlerBeanPostProcessor();
  }

  /**
   * For merging incoming objects materialized from JSON with existing domain objects loaded from the repository.
   *
   * @return
   *
   * @throws Exception
   */
  @Bean public DomainObjectMerger domainObjectMerger() throws Exception {
    return new DomainObjectMerger(
        repositories().getObject(),
        defaultConversionService()
    );
  }

  /**
   * The controller that handles top-level requests for listing what repositories are available.
   *
   * @return
   *
   * @throws Exception
   */
  @Bean public RepositoryController repositoryController() throws Exception {
    return new RepositoryController(
        repositories().getObject(),
        config(),
        domainClassConverter(),
        defaultConversionService()
    );
  }

  /**
   * The controller responsible for handling requests to display or those that modify an entity.
   *
   * @return
   *
   * @throws Exception
   */
  @Bean public RepositoryEntityController repositoryEntityController() throws Exception {
    return new RepositoryEntityController(
        repositories().getObject(),
        config(),
        domainClassConverter(),
        defaultConversionService()
    );
  }

  /**
   * The controller responsible for managing links of property references.
   *
   * @return
   *
   * @throws Exception
   */
  @Bean public RepositoryPropertyReferenceController propertyReferenceController() throws Exception {
    return new RepositoryPropertyReferenceController(
        repositories().getObject(),
        config(),
        domainClassConverter(),
        defaultConversionService()
    );
  }

  /**
   * The controller responsible for performing searches.
   *
   * @return
   *
   * @throws Exception
   */
  @Bean public RepositorySearchController repositorySearchController() throws Exception {
    return new RepositorySearchController(
        repositories().getObject(),
        config(),
        domainClassConverter(),
        defaultConversionService()
    );
  }

  /**
   * Resolves the base {@link java.net.URI} under which this application is configured.
   *
   * @return
   */
  @Bean public BaseUriMethodArgumentResolver baseUriMethodArgumentResolver() {
    return new BaseUriMethodArgumentResolver();
  }

  /**
   * Resolves the paging and sorting information from the query parameters based on the current configuration settings.
   *
   * @return
   */
  @Bean public PagingAndSortingMethodArgumentResolver pagingAndSortingMethodArgumentResolver() {
    return new PagingAndSortingMethodArgumentResolver();
  }

  /**
   * Turns an {@link javax.servlet.http.HttpServletRequest} into a {@link org.springframework.http.server.ServerHttpRequest}.
   *
   * @return
   */
  @Bean public ServerHttpRequestMethodArgumentResolver serverHttpRequestMethodArgumentResolver() {
    return new ServerHttpRequestMethodArgumentResolver();
  }

  /**
   * Resolves the {@link org.springframework.data.repository.core.RepositoryInformation} for this request.
   *
   * @return
   */
  @Bean public RepositoryInformationHandlerMethodArgumentResolver repoInfoMethodArgumentResolver() {
    return new RepositoryInformationHandlerMethodArgumentResolver();
  }

  /**
   * A convenience resolver that pulls together all the information needed to service a request.
   *
   * @return
   */
  @Bean public RepositoryRestRequestHandlerMethodArgumentResolver repoRequestArgumentResolver() {
    return new RepositoryRestRequestHandlerMethodArgumentResolver();
  }

  @Bean public RepositoryEntityLinksMethodArgumentResolver entityLinksMethodArgumentResolver() {
    return new RepositoryEntityLinksMethodArgumentResolver();
  }

  /**
   * Reads incoming JSON into an entity.
   *
   * @return
   */
  @Bean public PersistentEntityResourceHandlerMethodArgumentResolver persistentEntityArgumentResolver() {
    List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();
    configureHttpMessageConverters(messageConverters);

    return new PersistentEntityResourceHandlerMethodArgumentResolver(messageConverters);
  }

  /**
   * Turns a domain class into a {@link org.springframework.data.rest.repository.json.JsonSchema}.
   *
   * @return
   */
  @Bean public PersistentEntityToJsonSchemaConverter jsonSchemaConverter() {
    return new PersistentEntityToJsonSchemaConverter();
  }

  /**
   * The Jackson {@link ObjectMapper} used internally.
   *
   * @return
   */
  @Bean public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    // Our special PersistentEntityResource Module
    objectMapper.registerModule(persistentEntityJackson2Module());
    // Hibernate types
    if(IS_HIBERNATE4_MODULE_AVAILABLE) {
      objectMapper.registerModule(new com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module());
    }
    // JODA time
    if(IS_JODA_MODULE_AVAILABLE) {
      objectMapper.registerModule(new com.fasterxml.jackson.datatype.joda.JodaModule());
    }
    // Configure custom Modules
    configureJacksonObjectMapper(objectMapper);

    return objectMapper;
  }

  /**
   * The {@link HttpMessageConverter} used by Spring MVC to read and write JSON data.
   *
   * @return
   */
  @Bean public MappingJackson2HttpMessageConverter jacksonHttpMessageConverter() {
    MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
    jacksonConverter.setObjectMapper(objectMapper());
    jacksonConverter.setSupportedMediaTypes(Arrays.asList(
        MediaType.APPLICATION_JSON,
        MediaType.valueOf("application/schema+json"),
        MediaType.valueOf("application/x-spring-data-verbose+json"),
        MediaType.valueOf("application/x-spring-data-compact+json")
    ));
    return jacksonConverter;
  }

  /**
   * The {@link HttpMessageConverter} used to create JSONP responses.
   *
   * @return
   */
  @Bean public JsonpResponseHttpMessageConverter jsonpHttpMessageConverter() {
    return new JsonpResponseHttpMessageConverter(jacksonHttpMessageConverter());
  }

  /**
   * The {@link HttpMessageConverter} used to create {@literal text/uri-list} responses.
   *
   * @return
   */
  @Bean public UriListHttpMessageConverter uriListHttpMessageConverter() {
    return new UriListHttpMessageConverter();
  }

  /**
   * Special {@link org.springframework.web.servlet.HandlerAdapter} that only recognizes handler methods defined in
   * the provided controller classes.
   *
   * @return
   */
  @Bean public RepositoryRestHandlerAdapter repositoryExporterHandlerAdapter() {
    List<HttpMessageConverter<?>> messageConverters = defaultMessageConverters();
    configureHttpMessageConverters(messageConverters);

    RepositoryRestHandlerAdapter handlerAdapter = new RepositoryRestHandlerAdapter();
    handlerAdapter.setMessageConverters(messageConverters);
    handlerAdapter.setCustomArgumentResolvers(defaultMethodArgumentResolvers());

    return handlerAdapter;
  }

  /**
   * Special {@link org.springframework.web.servlet.HandlerMapping} that only recognizes handler methods defined in
   * the provided controller classes.
   *
   * @return
   */
  @Bean public RepositoryRestHandlerMapping repositoryExporterHandlerMapping() {
    return new RepositoryRestHandlerMapping();
  }

  /**
   * Jackson module responsible for intelligently serializing and deserializing JSON that corresponds to an entity.
   *
   * @return
   */
  @Bean public PersistentEntityJackson2Module persistentEntityJackson2Module() {
    return new PersistentEntityJackson2Module(defaultConversionService());
  }

  /**
   * Bean for looking up methods annotated with {@link org.springframework.web.bind.annotation.ExceptionHandler}.
   *
   * @return
   */
  @Bean public ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver() {
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
    messageConverters.add(jsonpHttpMessageConverter());
    messageConverters.add(uriListHttpMessageConverter());
    return messageConverters;
  }

  private List<HandlerMethodArgumentResolver> defaultMethodArgumentResolvers() {
    return Arrays.asList(baseUriMethodArgumentResolver(),
                         pagingAndSortingMethodArgumentResolver(),
                         serverHttpRequestMethodArgumentResolver(),
                         repoInfoMethodArgumentResolver(),
                         repoRequestArgumentResolver(),
                         persistentEntityArgumentResolver(),
                         entityLinksMethodArgumentResolver());
  }

  /**
   * Override this method to add additional configuration.
   *
   * @param config
   *     Main configuration bean.
   */
  protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
  }

  /**
   * Override this method to add your own converters.
   *
   * @param conversionService
   *     Default ConversionService bean.
   */
  protected void configureConversionService(ConfigurableConversionService conversionService) {
  }

  /**
   * Override this method to add validators manually.
   *
   * @param validatingListener
   *     The {@link org.springframework.context.ApplicationListener} responsible for invoking {@link
   *     org.springframework.validation.Validator} instances.
   */
  protected void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
  }

  /**
   * Configure the {@link ExceptionHandlerExceptionResolver}.
   *
   * @param exceptionResolver
   *     The default exception resolver on which you can add custom argument resolvers.
   */
  protected void configureExceptionHandlerExceptionResolver(ExceptionHandlerExceptionResolver exceptionResolver) {
  }

  /**
   * Configure the available {@link HttpMessageConverter}s by adding your own.
   *
   * @param messageConverters
   *     The converters to be used by the system.
   */
  protected void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
  }

  /**
   * Configure the Jackson {@link ObjectMapper} directly.
   *
   * @param objectMapper
   *     The {@literal ObjectMapper} to be used by the system.
   */
  protected void configureJacksonObjectMapper(ObjectMapper objectMapper) {
  }

}
