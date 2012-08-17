package org.springframework.data.rest.webmvc;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.map.module.SimpleAbstractTypeResolver;
import org.codehaus.jackson.map.module.SimpleDeserializers;
import org.codehaus.jackson.map.module.SimpleKeyDeserializers;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.module.SimpleSerializers;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.rest.core.Link;
import org.springframework.data.rest.core.ResourceLink;
import org.springframework.data.rest.repository.AttributeMetadata;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.UriToDomainObjectResolver;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin
 */
public class RepositoryAwareMappingHttpMessageConverter
    extends MappingJacksonHttpMessageConverter
    implements InitializingBean {

  private final ObjectMapper mapper = new ObjectMapper();

  @Autowired(required = false)
  protected ConversionService        conversionService   = new DefaultFormattingConversionService();
  @Autowired(required = false)
  protected List<RepositoryExporter> repositoryExporters = Collections.emptyList();
  @Autowired(required = false)
  protected List<Module>             modules             = Collections.emptyList();
  @Autowired
  protected UriToDomainObjectResolver domainObjectResolver;

  public RepositoryAwareMappingHttpMessageConverter() {
    setSupportedMediaTypes(Arrays.asList(
        MediaType.APPLICATION_JSON,
        MediaTypes.COMPACT_JSON,
        MediaTypes.VERBOSE_JSON
    ));
    setObjectMapper(mapper);
  }

  @Override public void afterPropertiesSet() throws Exception {
    mapper.registerModule(new RepositoryAwareModule());
    for(Module m : modules) {
      mapper.registerModule(m);
    }
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  public RepositoryAwareMappingHttpMessageConverter setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
    return this;
  }

  public List<RepositoryExporter> getRepositoryExporters() {
    return repositoryExporters;
  }

  @Autowired(required = false)
  @SuppressWarnings({"unchecked"})
  public RepositoryAwareMappingHttpMessageConverter setRepositoryExporters(List<RepositoryExporter> repositoryExporters) {
    this.repositoryExporters = repositoryExporters;
    this.mapper.registerModule(new RepositoryAwareModule());
    return this;
  }

  public List<Module> getModules() {
    return modules;
  }

  public RepositoryAwareMappingHttpMessageConverter setModules(List<Module> modules) {
    this.modules = modules;
    return this;
  }

  public UriToDomainObjectResolver getDomainObjectResolver() {
    return domainObjectResolver;
  }

  public RepositoryAwareMappingHttpMessageConverter setDomainObjectResolver(UriToDomainObjectResolver domainObjectResolver) {
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

  @SuppressWarnings({"unchecked"})
  private RepositoryMetadata repositoryMetadataFor(Class<?> domainType) {
    for(RepositoryExporter repoExp : repositoryExporters) {
      if(repoExp.hasRepositoryFor(domainType)) {
        return repoExp.repositoryMetadataFor(domainType);
      }
    }
    return null;
  }

  private URI buildUri(URI baseUri, String... pathSegments) {
    return UriComponentsBuilder.fromUri(baseUri).pathSegment(pathSegments).build().toUri();
  }

  private class RepositoryAwareModule extends SimpleModule {

    SimpleSerializers      sers     = new SimpleSerializers();
    SimpleDeserializers    dsers    = new SimpleDeserializers();
    SimpleSerializers      keySers  = new SimpleSerializers();
    SimpleKeyDeserializers keyDsers = new SimpleKeyDeserializers();

    private RepositoryAwareModule() {
      super("RepositoryAwareModule", new Version(1, 0, 0, "SNAPSHOT"));
    }

    @SuppressWarnings({"unchecked"})
    @Override public void setupModule(SetupContext context) {
      context.addAbstractTypeResolver(
          new SimpleAbstractTypeResolver()
              .addMapping(Link.class, ResourceLink.class)
      );

      for(RepositoryExporter repoExp : repositoryExporters) {
        for(String repoName : new ArrayList<String>(repoExp.repositoryNames())) {
          RepositoryMetadata repoMeta = repoExp.repositoryMetadataFor(repoName);
          Class domainType = repoMeta.entityMetadata().type();

          sers.addSerializer(domainType, new DomainObjectToLinkSerializer(domainType, repoMeta));
          keySers.addSerializer(domainType, new DomainObjectToStringKeySerializer(domainType, repoMeta));

          dsers.addDeserializer(domainType, new LinkToDomainObjectDeserializer(domainType, repoMeta));
          keyDsers.addDeserializer(domainType, new KeyToDomainObjectDeserializer());
        }
      }

      context.addSerializers(sers);
      context.addKeySerializers(keySers);
      context.addDeserializers(dsers);
      context.addKeyDeserializers(keyDsers);
    }
  }

  private class DomainObjectToLinkSerializer extends SerializerBase<Object> {

    protected final RepositoryMetadata repoMeta;
    protected final AttributeMetadata  idAttr;

    private DomainObjectToLinkSerializer(Class<Object> t, RepositoryMetadata repoMeta) {
      super(t);
      this.repoMeta = repoMeta;
      if(null != repoMeta) {
        idAttr = repoMeta.entityMetadata().idAttribute();
      } else {
        idAttr = null;
      }
    }

    @Override public void serialize(Object value,
                                    JsonGenerator jgen,
                                    SerializerProvider provider) throws IOException,
                                                                        JsonGenerationException {
      if(null == value) {
        provider.defaultSerializeNull(jgen);
        return;
      }
      if(null == repoMeta) {
        provider.defaultSerializeValue(value, jgen);
        return;
      }

      Serializable serId = (Serializable)idAttr.get(value);
      String sId = conversionService.convert(serId, String.class);
      if(null == sId) {
        sId = serId.toString();
      }

      String rel = repoMeta.rel() + "." + repoMeta.domainType().getSimpleName();
      URI selfUri = buildUri(RepositoryRestController.BASE_URI.get(), repoMeta.name(), sId);

      jgen.writeObject(new ResourceLink(rel, selfUri));
    }

  }

  private class DomainObjectToStringKeySerializer extends DomainObjectToLinkSerializer {

    private DomainObjectToStringKeySerializer(Class<Object> t, RepositoryMetadata repoMeta) {
      super(t, repoMeta);
    }

    @Override public void serialize(Object value,
                                    JsonGenerator jgen,
                                    SerializerProvider provider) throws IOException,
                                                                        JsonGenerationException {
      if(null == value) {
        provider.defaultSerializeNull(jgen);
        return;
      }
      if(null == repoMeta) {
        provider.defaultSerializeValue(value, jgen);
        return;
      }

      Serializable serId = (Serializable)idAttr.get(value);
      String sId = conversionService.convert(serId, String.class);
      if(null == sId) {
        sId = serId.toString();
      }

      URI href = buildUri(RepositoryRestController.BASE_URI.get(), repoMeta.name(), sId);

      jgen.writeString("@" + href.toString());
    }

  }

  private class LinkToDomainObjectDeserializer extends StdDeserializer<Object> {

    protected final RepositoryMetadata repoMeta;

    private LinkToDomainObjectDeserializer(Class<?> vc, RepositoryMetadata repoMeta) {
      super(vc);
      this.repoMeta = repoMeta;
    }

    @SuppressWarnings({"unchecked"})
    @Override public Object deserialize(JsonParser jp,
                                        DeserializationContext ctxt) throws IOException,
                                                                            JsonProcessingException {
      Object entity;
      try {
        entity = getValueClass().newInstance();
      } catch(InstantiationException e) {
        throw ctxt.instantiationException(getValueClass(), e);
      } catch(IllegalAccessException e) {
        throw ctxt.instantiationException(getValueClass(), e);
      }

      for(JsonToken tok = jp.nextToken(); tok != JsonToken.END_OBJECT; tok = jp.nextToken()) {
        String name = jp.getCurrentName();
        switch(tok) {
          case FIELD_NAME: {
            // Read the attribute metadata
            AttributeMetadata attrMeta = repoMeta.entityMetadata().attribute(name);
            Object val = null;

            if(name.startsWith("@http")) {
              entity = domainObjectResolver.resolve(
                  RepositoryRestController.BASE_URI.get(),
                  URI.create(name.substring(1))
              );
              continue;
            }

            if("href".equals(name)) {
              entity = domainObjectResolver.resolve(
                  RepositoryRestController.BASE_URI.get(),
                  URI.create(jp.nextTextValue())
              );
              continue;
            }

            if("rel".equals(name)) {
              // rel is currently ignored
              continue;
            }

            if(null == attrMeta) {
              // do nothing
              continue;
            }

            // Try and read the value of this attribute.
            // The method of doing that varies based on the type of the property.
            if(attrMeta.isCollectionLike()) {
              Collection c = attrMeta.asCollection(entity);
              if(null == c || c == Collections.emptyList()) {
                c = new ArrayList();
              }

              if((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
                do {
                  Object cval = jp.readValueAs(attrMeta.elementType());
                  c.add(cval);
                } while((tok = jp.nextToken()) != JsonToken.END_ARRAY);

                val = c;

              } else if(tok == JsonToken.VALUE_NULL) {
                val = null;
              } else {
                throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Collection.");
              }
            } else if(attrMeta.isSetLike()) {
              Set s = attrMeta.asSet(entity);
              if(null == s || s == Collections.emptySet()) {
                s = new HashSet();
              }

              if((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
                do {
                  Object sval = jp.readValueAs(attrMeta.elementType());
                  s.add(sval);
                } while((tok = jp.nextToken()) != JsonToken.END_ARRAY);

                val = s;

              } else if(tok == JsonToken.VALUE_NULL) {
                val = null;
              } else {
                throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Set.");
              }
            } else if(attrMeta.isMapLike()) {
              Map m = attrMeta.asMap(entity);
              if(null == m || m == Collections.emptyMap()) {
                m = new HashMap();
              }

              if((tok = jp.nextToken()) == JsonToken.START_OBJECT) {
                do {
                  name = jp.getCurrentName();
                  Object mkey = (
                      name.startsWith("@http")
                      ? domainObjectResolver.resolve(
                          RepositoryRestController.BASE_URI.get(),
                          URI.create(name.substring(1))
                      )
                      : name
                  );
                  tok = jp.nextToken();
                  Object mval = jp.readValueAs(attrMeta.elementType());

                  m.put(mkey, mval);
                } while((tok = jp.nextToken()) != JsonToken.END_OBJECT);

                val = m;

              } else if(tok == JsonToken.VALUE_NULL) {
                val = null;
              } else {
                throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Map.");
              }
            } else {
              if((tok = jp.nextToken()) != JsonToken.VALUE_NULL) {
                val = jp.readValueAs(attrMeta.type());
              }
            }

            if(null != val) {
              attrMeta.set(val, entity);
            }

            break;
          }
        }
      }

      return entity;
    }

  }

  private class KeyToDomainObjectDeserializer extends KeyDeserializer {
    @Override public Object deserializeKey(String key,
                                           DeserializationContext ctxt) throws IOException,
                                                                               JsonProcessingException {
      if(key.startsWith("@http")) {
        return domainObjectResolver.resolve(
            RepositoryRestController.BASE_URI.get(),
            URI.create(key.substring(1))
        );
      } else {
        return key;
      }
    }
  }

}
