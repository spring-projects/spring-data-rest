package org.springframework.data.rest.webmvc.json;

import static org.springframework.data.rest.core.util.UriUtils.*;
import static org.springframework.data.util.ClassTypeInformation.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.map.module.SimpleDeserializers;
import org.codehaus.jackson.map.module.SimpleKeyDeserializers;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.module.SimpleSerializers;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.rest.repository.AttributeMetadata;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.UriToDomainObjectUriResolver;
import org.springframework.data.rest.webmvc.EntityToResourceConverter;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Special implementation of a Jackson {@link org.codehaus.jackson.map.Module} to handle properly serializing and
 * deserializing entities with links.
 *
 * @author Jon Brisbin
 */
public class RepositoryAwareJacksonModule extends SimpleModule implements InitializingBean {

  @Autowired(required = false)
  protected List<RepositoryExporter>                           repositoryExporters  = Collections.emptyList();
  @Autowired(required = false)
  private   List<ConversionService>                            conversionServices   = Collections.emptyList();
  @Autowired(required = false)
  private   List<ResourceProcessor<Resource<?>>>               resourceProcessors   = Collections.emptyList();
  private   Multimap<Class<?>, ResourceProcessor<Resource<?>>> resourceProcessorMap = ArrayListMultimap.create();
  @Autowired
  private UriToDomainObjectUriResolver domainObjectResolver;
  private final GenericConversionService conversionService = new GenericConversionService();

  private final SimpleSerializers      sers     = new SimpleSerializers();
  private final SimpleDeserializers    dsers    = new SimpleDeserializers();
  private final SimpleSerializers      keySers  = new SimpleSerializers();
  private final SimpleKeyDeserializers keyDsers = new SimpleKeyDeserializers();

  public RepositoryAwareJacksonModule() {
    super("RepositoryAwareJacksonModule", Version.unknownVersion());
  }

  @SuppressWarnings({"unchecked"})
  @Override public void afterPropertiesSet() throws Exception {
    for(RepositoryExporter repoExp : repositoryExporters) {
      for(String repoName : new ArrayList<String>(repoExp.repositoryNames())) {
        RepositoryMetadata repoMeta = repoExp.repositoryMetadataFor(repoName);
        Class domainType = repoMeta.entityMetadata().type();
        TypeInformation<?> domainTypeInfo = from(domainType);

        for(ResourceProcessor<Resource<?>> rp : resourceProcessors) {
          TypeInformation<?> resourceType = from(rp.getClass())
              .getSuperTypeInformation(ResourceProcessor.class)
              .getComponentType();
          Class<?> processorType = resourceType.getType();
          TypeInformation<?> componentType = resourceType.getComponentType();

          if(Resource.class.isAssignableFrom(processorType) && componentType.isAssignableFrom(domainTypeInfo)) {
            resourceProcessorMap.put(domainType, rp);
          }
        }

        conversionService.addConverter(domainType, Resource.class, new EntityToResourceConverter(repoMeta));

        sers.addSerializer(domainType, new DomainObjectToResourceSerializer(domainType));
        keySers.addSerializer(domainType, new DomainObjectToStringKeySerializer(domainType, repoMeta));

        dsers.addDeserializer(domainType, new LinkToDomainObjectDeserializer(domainType, repoMeta));
        keyDsers.addDeserializer(domainType, new KeyToDomainObjectDeserializer());
      }
    }
  }

  @Override public void setupModule(SetupContext context) {
    context.addSerializers(sers);
    context.addKeySerializers(keySers);
    context.addDeserializers(dsers);
    context.addKeyDeserializers(keyDsers);
  }

  private class DomainObjectToResourceSerializer extends SerializerBase<Object> {
    private DomainObjectToResourceSerializer(Class<Object> t) {
      super(t);
    }

    @SuppressWarnings({"unchecked"})
    @Override public void serialize(Object value,
                                    JsonGenerator jgen,
                                    SerializerProvider provider) throws IOException,
                                                                        JsonGenerationException {
      if(null == value) {
        provider.defaultSerializeNull(jgen);
        return;
      }

      if(!conversionService.canConvert(value.getClass(), Resource.class)) {
        provider.defaultSerializeValue(value, jgen);
        return;
      }

      // Process the resource first to catch user stuff
      Resource<?> resource = new Resource<Object>(value);
      for(ResourceProcessor<Resource<?>> rp : resourceProcessorMap.get(value.getClass())) {
        resource = rp.process(resource);
      }
      // Maybe convert the resource so we can extract linked properties
      if(null == resource.getContent()) {
        provider.defaultSerializeNull(jgen);
        return;
      }

      Class<?> sourceType = resource.getContent().getClass();
      ConversionService entityConversionSvc = conversionService;
      for(ConversionService cs : conversionServices) {
        if(cs.canConvert(sourceType, Resource.class)) {
          entityConversionSvc = cs;
          break;
        }
      }
      if(entityConversionSvc.canConvert(sourceType, Resource.class)) {
        Set<Link> links = resource.getLinks();
        resource = entityConversionSvc.convert(value, Resource.class);
        resource.add(links);
      }

      jgen.writeObject(resource);

    }

  }

  private class DomainObjectToStringKeySerializer extends SerializerBase<Object> {

    private final RepositoryMetadata repoMeta;
    private final AttributeMetadata  idAttr;

    private DomainObjectToStringKeySerializer(Class<Object> t, RepositoryMetadata repoMeta) {
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
      String sId = null;
      for(ConversionService cs : conversionServices) {
        if(cs.canConvert(idAttr.type(), String.class)) {
          sId = cs.convert(serId, String.class);
          break;
        }
      }
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
      Object entity = BeanUtils.instantiateClass(getValueClass());
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

            if("links".equals(name)) {
              if((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
                while((tok = jp.nextToken()) != JsonToken.END_ARRAY) {
                  // Advance past the links
                }
              } else {
                throw new HttpMessageNotReadableException(
                    "Property 'links' is not of array type. Either eliminate this property from the document or make it an array.");
              }
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
				// advance past the start_array token
				jp.nextToken();
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
				// advance past the start_array token
				jp.nextToken();
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
