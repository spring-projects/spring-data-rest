package org.springframework.data.rest.repository.json;

import static org.springframework.beans.BeanUtils.*;
import static org.springframework.data.rest.core.util.UriUtils.*;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.PersistentEntityResource;
import org.springframework.data.rest.repository.UriDomainClassConverter;
import org.springframework.hateoas.Link;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityJackson2Module extends SimpleModule implements InitializingBean {

  private static final Logger         LOG      = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);
  private static final TypeDescriptor URI_TYPE = TypeDescriptor.valueOf(URI.class);
  private final ConversionService           conversionService;
  @Autowired
  private       Repositories                repositories;
  @Autowired
  private       RepositoryRestConfiguration config;
  @Autowired
  private       UriDomainClassConverter     uriDomainClassConverter;

  public PersistentEntityJackson2Module(ConversionService conversionService) {
    super(new Version(1, 1, 0, "BUILD-SNAPSHOT", "org.springframework.data.rest", "jackson-module"));
    this.conversionService = conversionService;

    addSerializer(new ResourceSerializer());
  }

  public static boolean maybeAddAssociationLink(Repositories repositories,
                                                RepositoryRestConfiguration config,
                                                URI baseEntityUri,
                                                ResourceMapping propertyMapping,
                                                PersistentProperty persistentProperty,
                                                List<Link> links) {
    Class<?> propertyType = persistentProperty.getType();
    if(persistentProperty.isCollectionLike() || persistentProperty.isArray()) {
      propertyType = persistentProperty.getComponentType();
    }

    String propertyPath = (null != propertyMapping
                           ? propertyMapping.getPath()
                           : persistentProperty.getName());
    // In case a property mapping is specified but no path is set
    if(null == propertyPath) {
      propertyPath = persistentProperty.getName();
    }
    // entityRel + "." +
    String propertyRel = (null != propertyMapping
                          ? propertyMapping.getRel()
                          : propertyPath);
    if(repositories.hasRepositoryFor(propertyType)) {
      // This is a managed type, generate a Link
      RepositoryInformation linkedRepoInfo = repositories.getRepositoryInformationFor(propertyType);
      ResourceMapping linkedRepoMapping = getResourceMapping(config, linkedRepoInfo);
      if(linkedRepoMapping.isExported()) {
        URI uri = buildUri(baseEntityUri, propertyPath);
        Link l = new Link(uri.toString(), propertyRel);
        links.add(l);
        // This is an association. We added a Link.
        return true;
      }
    }
    // This is not an association. No Link was added.
    return false;
  }

  @SuppressWarnings({"unchecked"})
  @Override public void afterPropertiesSet() throws Exception {
    for(Class<?> domainType : repositories) {
      addDeserializer(domainType, new ResourceDeserializer(repositories.getPersistentEntity(domainType)));
    }
  }

  private class ResourceDeserializer<T extends Object> extends StdDeserializer<T> {

    private final PersistentEntity persistentEntity;
    private final Object           defaultObject;
    private final Map<String, Object> defaultValues = new HashMap<String, Object>();

    @SuppressWarnings({"unchecked"})
    private ResourceDeserializer(PersistentEntity persistentEntity) {
      super(persistentEntity.getType());
      this.persistentEntity = persistentEntity;
      this.defaultObject = instantiateClass(getValueClass());

      final BeanWrapper wrapper = BeanWrapper.create(defaultObject, conversionService);
      persistentEntity.doWithProperties(new PropertyHandler() {
        @Override public void doWithPersistentProperty(PersistentProperty prop) {
          Object defaultValue = wrapper.getProperty(prop);
          if(null != defaultValue) {
            defaultValues.put(prop.getName(), defaultValue);
          }
        }
      });
    }

    @SuppressWarnings({"unchecked"})
    @Override public T deserialize(JsonParser jp,
                                   DeserializationContext ctxt) throws IOException,
                                                                       JsonProcessingException {
      Object entity = instantiateClass(getValueClass());
      BeanWrapper wrapper = BeanWrapper.create(entity, conversionService);
      ResourceMapping domainMapping = config.getResourceMappingForDomainType(getValueClass());

      for(JsonToken tok = jp.nextToken(); tok != JsonToken.END_OBJECT; tok = jp.nextToken()) {
        String name = jp.getCurrentName();
        switch(tok) {
          case FIELD_NAME: {
            if("href".equals(name)) {
              URI uri = URI.create(jp.nextTextValue());
              TypeDescriptor entityType = TypeDescriptor.forObject(entity);
              if(uriDomainClassConverter.matches(URI_TYPE, entityType)) {
                entity = uriDomainClassConverter.convert(uri, URI_TYPE, entityType);
              }
              continue;
            }

            if("rel".equals(name)) {
              // rel is currently ignored
              continue;
            }

            PersistentProperty persistentProperty = persistentEntity.getPersistentProperty(name);
            if(null == persistentProperty) {
              String errMsg = "Property '" + name + "' not found for entity " + getValueClass().getName();
              if(null == domainMapping) {
                throw new HttpMessageNotReadableException(errMsg);
              }
              String propertyName = domainMapping.getNameForPath(name);
              if(null == propertyName) {
                throw new HttpMessageNotReadableException(errMsg);
              }
              persistentProperty = persistentEntity.getPersistentProperty(propertyName);
              if(null == persistentProperty) {
                throw new HttpMessageNotReadableException(errMsg);
              }
            }

            Object val = null;

            if("links".equals(name)) {
              if((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
                while((tok = jp.nextToken()) != JsonToken.END_ARRAY) {
                  // Advance past the links
                }
              } else if(tok == JsonToken.VALUE_NULL) {
                // skip null value
              } else {
                throw new HttpMessageNotReadableException(
                    "Property 'links' is not of array type. Either eliminate this property from the document or make it an array.");
              }
              continue;
            }

            if(null == persistentProperty) {
              // do nothing
              continue;
            }

            // Try and read the value of this attribute.
            // The method of doing that varies based on the type of the property.
            if(persistentProperty.isCollectionLike()) {
              Class<? extends Collection> ctype = (Class<? extends Collection>)persistentProperty.getType();
              Collection c = (Collection)wrapper.getProperty(persistentProperty, ctype, false);
              if(null == c || c == Collections.EMPTY_LIST || c == Collections.EMPTY_SET) {
                if(Collection.class.isAssignableFrom(ctype)) {
                  c = new ArrayList();
                } else if(Set.class.isAssignableFrom(ctype)) {
                  c = new HashSet();
                }
              }

              if((tok = jp.nextToken()) == JsonToken.START_ARRAY) {
                while((tok = jp.nextToken()) != JsonToken.END_ARRAY) {
                  Object cval = jp.readValueAs(persistentProperty.getComponentType());
                  c.add(cval);
                }

                val = c;
              } else if(tok == JsonToken.VALUE_NULL) {
                val = null;
              } else {
                throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Collection.");
              }
            } else if(persistentProperty.isMap()) {
              Class<? extends Map> mtype = (Class<? extends Map>)persistentProperty.getType();
              Map m = (Map)wrapper.getProperty(persistentProperty, mtype, false);
              if(null == m || m == Collections.EMPTY_MAP) {
                m = new HashMap();
              }

              if((tok = jp.nextToken()) == JsonToken.START_OBJECT) {
                do {
                  name = jp.getCurrentName();
                  // TODO resolve domain object from URI
                  tok = jp.nextToken();
                  Object mval = jp.readValueAs(persistentProperty.getMapValueType());

                  m.put(name, mval);
                } while((tok = jp.nextToken()) != JsonToken.END_OBJECT);

                val = m;
              } else if(tok == JsonToken.VALUE_NULL) {
                val = null;
              } else {
                throw new HttpMessageNotReadableException("Cannot read a JSON " + tok + " as a Map.");
              }
            } else {
              if((tok = jp.nextToken()) != JsonToken.VALUE_NULL) {
                val = jp.readValueAs(persistentProperty.getType());
              }
            }

            if(null != val) {
              Object defaultValue = defaultValues.get(persistentProperty.getName());
              if(null == defaultValue || defaultValue != val) {
                wrapper.setProperty(persistentProperty, val, false);
              }
            }

            break;
          }
        }
      }

      return (T)entity;
    }
  }

  private class ResourceSerializer extends StdSerializer<PersistentEntityResource> {

    private ResourceSerializer() {
      super(PersistentEntityResource.class);
    }

    @SuppressWarnings({"unchecked"})
    @Override public void serialize(final PersistentEntityResource resource,
                                    final JsonGenerator jgen,
                                    final SerializerProvider provider) throws IOException,
                                                                              JsonGenerationException {
      if(LOG.isDebugEnabled()) {
        LOG.debug("Serializing PersistentEntity " + resource.getPersistentEntity());
      }

      Object obj = resource.getContent();

      final PersistentEntity persistentEntity = resource.getPersistentEntity();
      final ResourceMapping entityMapping = getResourceMapping(config, persistentEntity);

      final RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(persistentEntity.getType());
      final ResourceMapping repoMapping = getResourceMapping(config, repoInfo);

      final BeanWrapper wrapper = BeanWrapper.create(obj, conversionService);
      final Object entityId = wrapper.getProperty(persistentEntity.getIdProperty());

      final URI baseEntityUri = buildUri(resource.getBaseUri(),
                                         repoMapping.getPath(),
                                         entityId.toString());

      final List<Link> links = new ArrayList<Link>();
      // Start with ResourceProcessor-added links
      links.addAll(resource.getLinks());

      jgen.writeStartObject();
      try {
        persistentEntity.doWithProperties(new PropertyHandler() {
          @Override public void doWithPersistentProperty(PersistentProperty persistentProperty) {
            if(persistentProperty.isIdProperty() && !config.isIdExposedFor(persistentEntity.getType())) {
              return;
            }
            ResourceMapping propertyMapping = entityMapping.getResourceMappingFor(persistentProperty.getName());
            if(null != propertyMapping && !propertyMapping.isExported()) {
              return;
            }

            if(persistentProperty.isEntity() && maybeAddAssociationLink(repositories,
                                                                        config,
                                                                        baseEntityUri,
                                                                        propertyMapping,
                                                                        persistentProperty,
                                                                        links)) {
              return;
            }

            // Property is a normal or non-managed property.
            String propertyName = (null != propertyMapping ? propertyMapping.getPath() : persistentProperty.getName());
            Object propertyValue = wrapper.getProperty(persistentProperty);
            try {
              jgen.writeObjectField(propertyName, propertyValue);
            } catch(IOException e) {
              throw new IllegalStateException(e);
            }
          }
        });

        // Add associations as links
        persistentEntity.doWithAssociations(new AssociationHandler() {
          @Override public void doWithAssociation(Association association) {
            PersistentProperty persistentProperty = association.getInverse();
            ResourceMapping propertyMapping = entityMapping.getResourceMappingFor(persistentProperty.getName());
            if(null != propertyMapping && !propertyMapping.isExported()) {
              return;
            }
            if(maybeAddAssociationLink(repositories,
                                       config,
                                       baseEntityUri,
                                       propertyMapping,
                                       persistentProperty,
                                       links)) {
              return;
            }
            // Association Link was not added, probably because this isn't a managed type. Add value of property inline.
            Object propertyValue = wrapper.getProperty(persistentProperty);
            try {
              jgen.writeObjectField(persistentProperty.getName(), propertyValue);
            } catch(IOException e) {
              throw new IllegalStateException(e);
            }
          }
        });

        jgen.writeArrayFieldStart("links");
        for(Link l : links) {
          jgen.writeObject(l);
        }
        jgen.writeEndArray();

      } catch(IllegalStateException e) {
        throw (IOException)e.getCause();
      } finally {
        jgen.writeEndObject();
      }
    }
  }

}
