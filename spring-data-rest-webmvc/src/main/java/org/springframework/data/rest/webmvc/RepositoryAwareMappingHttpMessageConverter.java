package org.springframework.data.rest.webmvc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.UriToDomainObjectUriResolver;
import org.springframework.data.rest.webmvc.json.RepositoryAwareJacksonModule;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

/**
 * @author Jon Brisbin
 */
public class RepositoryAwareMappingHttpMessageConverter
    extends MappingJacksonHttpMessageConverter
    implements ApplicationEventPublisherAware,
               InitializingBean {

  private final ObjectMapper                 mapper               = new ObjectMapper();
  @Autowired(required = false)
  protected     List<ConversionService>      conversionServices   = Arrays.<ConversionService>asList(new DefaultFormattingConversionService());
  @Autowired(required = false)
  protected     List<RepositoryExporter>     repositoryExporters  = Collections.emptyList();
  @Autowired(required = false)
  protected     List<Module>                 modules              = Collections.emptyList();
  @Autowired
  protected     UriToDomainObjectUriResolver domainObjectResolver = null;
  @Autowired
  protected     RepositoryAwareJacksonModule jacksonModule        = null;
  protected     ApplicationEventPublisher    eventPublisher       = null;

  public RepositoryAwareMappingHttpMessageConverter() {
    setSupportedMediaTypes(Arrays.asList(
        MediaType.APPLICATION_JSON,
        MediaTypes.COMPACT_JSON,
        MediaTypes.VERBOSE_JSON
    ));
    setObjectMapper(mapper);
  }

  @Override public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.eventPublisher = applicationEventPublisher;
  }

  @Override public void afterPropertiesSet() throws Exception {
    boolean builtInModuleRegistered = false;
    for(Module m : modules) {
      mapper.registerModule(m);
      if(m.getClass() == RepositoryAwareJacksonModule.class) {
        builtInModuleRegistered = true;
      }
    }

    if(!builtInModuleRegistered) {
      mapper.registerModule(jacksonModule);
    }
  }

  public List<ConversionService> getConversionServices() {
    return conversionServices;
  }

  public RepositoryAwareMappingHttpMessageConverter setConversionServices(List<ConversionService> conversionServices) {
    this.conversionServices = conversionServices;
    return this;
  }

  public List<RepositoryExporter> getRepositoryExporters() {
    return repositoryExporters;
  }

  @SuppressWarnings({"unchecked"})
  public RepositoryAwareMappingHttpMessageConverter setRepositoryExporters(List<RepositoryExporter> repositoryExporters) {
    this.repositoryExporters = repositoryExporters;
    return this;
  }

  public List<Module> getModules() {
    return modules;
  }

  public RepositoryAwareMappingHttpMessageConverter setModules(List<Module> modules) {
    this.modules = modules;
    return this;
  }

  public UriToDomainObjectUriResolver getDomainObjectResolver() {
    return domainObjectResolver;
  }

  public RepositoryAwareMappingHttpMessageConverter setDomainObjectResolver(UriToDomainObjectUriResolver domainObjectResolver) {
    this.domainObjectResolver = domainObjectResolver;
    return this;
  }

  @Override public boolean canWrite(Class<?> clazz, MediaType mediaType) {
    if(!canWrite(mediaType)) {
      return false;
    }
    return supports(clazz);
  }

  @Override public boolean canRead(Class<?> clazz, MediaType mediaType) {
    if(!canRead(mediaType)) {
      return false;
    }
    return supports(clazz);
  }

  @SuppressWarnings({"unchecked"})
  @Override protected boolean supports(Class<?> clazz) {
    for(RepositoryExporter repoExp : repositoryExporters) {
      for(String repoName : new ArrayList<String>(repoExp.repositoryNames())) {
        RepositoryMetadata repoMeta = repoExp.repositoryMetadataFor(repoName);
        Class domainType = repoMeta.entityMetadata().type();
        if(domainType.isAssignableFrom(clazz)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override protected void writeInternal(Object object,
                                         HttpOutputMessage outputMessage) throws IOException,
                                                                                 HttpMessageNotWritableException {
    JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
    // Believe it or not, this is the only way to get pretty-printing from Jackson in this configuration
    JsonGenerator jsonGenerator = mapper
        .getJsonFactory()
        .createJsonGenerator(outputMessage.getBody(), encoding)
        .useDefaultPrettyPrinter();
    try {
      mapper.writeValue(jsonGenerator, object);
    } catch(IOException ex) {
      throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
    }
  }

  @Override protected Object readInternal(Class<?> clazz,
                                          HttpInputMessage inputMessage) throws IOException,
                                                                                HttpMessageNotReadableException {
    try {
      return mapper.readValue(inputMessage.getBody(), clazz);
    } catch(IOException ex) {
      throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
    }
  }

}
