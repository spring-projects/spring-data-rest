package org.springframework.data.rest.repository.json;

import static org.springframework.data.rest.core.util.UriUtils.*;
import static org.springframework.data.rest.repository.json.PersistentEntityJackson2Module.*;
import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;
import static org.springframework.util.StringUtils.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.annotation.Description;
import org.springframework.data.rest.repository.support.RepositoryInformationSupport;
import org.springframework.hateoas.Link;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityToJsonSchemaConverter
    extends RepositoryInformationSupport
    implements ConditionalGenericConverter,
               InitializingBean {

  private static final TypeDescriptor       STRING_TYPE      = TypeDescriptor.valueOf(String.class);
  private static final TypeDescriptor       SCHEMA_TYPE      = TypeDescriptor.valueOf(JsonSchema.class);
  private              Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();

  @Override public void afterPropertiesSet() throws Exception {
    for(Class<?> domainType : repositories) {
      convertiblePairs.add(new ConvertiblePair(domainType, JsonSchema.class));
    }
  }

  @Override public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return (Class.class.isAssignableFrom(sourceType.getType()) && JsonSchema.class.isAssignableFrom(targetType.getType()));
  }

  @Override public Set<ConvertiblePair> getConvertibleTypes() {
    return convertiblePairs;
  }

  public JsonSchema convert(Class<?> domainType) {
    return (JsonSchema)convert(domainType, STRING_TYPE, SCHEMA_TYPE);
  }

  @SuppressWarnings({"unchecked"})
  @Override public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    PersistentEntity persistentEntity = repositories.getPersistentEntity((Class<?>)source);
    final ResourceMapping repoMapping = getResourceMapping(config,
                                                           repositories.getRepositoryInformationFor(persistentEntity.getType()));
    final ResourceMapping entityMapping = getResourceMapping(config, persistentEntity);
    final URI baseEntityUri = buildUri(config.getBaseUri(), repoMapping.getPath(), "{id}");
    String entityDesc = persistentEntity.getType().isAnnotationPresent(Description.class)
                        ? ((Description)persistentEntity.getType().getAnnotation(Description.class)).value()
                        : null;

    final JsonSchema jsonSchema = new JsonSchema(persistentEntity.getName(), entityDesc);
    persistentEntity.doWithProperties(new PropertyHandler() {
      @Override public void doWithPersistentProperty(PersistentProperty persistentProperty) {
        Class<?> propertyType = persistentProperty.getType();
        String type = uncapitalize(propertyType.getSimpleName());
        boolean notNull = (persistentProperty.getField().isAnnotationPresent(Nonnull.class)
            || persistentProperty.getGetter().isAnnotationPresent(Nonnull.class))
            || (persistentProperty.getField().isAnnotationPresent(NotNull.class)
            || persistentProperty.getGetter().isAnnotationPresent(NotNull.class));
        String desc = persistentProperty.getField().isAnnotationPresent(Description.class)
                      ? persistentProperty.getField().getAnnotation(Description.class).value()
                      : persistentProperty.getGetter().isAnnotationPresent(Description.class)
                        ? persistentProperty.getGetter().getAnnotation(Description.class).value()
                        : null;

        JsonSchema.Property property;
        if(persistentProperty.isCollectionLike()) {
          property = new JsonSchema.ArrayProperty("array", desc, notNull);
        } else {
          property = new JsonSchema.Property(type, desc, notNull);
        }
        jsonSchema.addProperty(persistentProperty.getName(), property);
      }
    });

    final List<Link> links = new ArrayList<Link>();
    persistentEntity.doWithAssociations(new AssociationHandler() {
      @Override public void doWithAssociation(Association association) {
        PersistentProperty persistentProperty = association.getInverse();
        ResourceMapping propertyMapping = entityMapping.getResourceMappingFor(persistentProperty.getName());
        if(null != propertyMapping && !propertyMapping.isExported()) {
          return;
        }
        maybeAddAssociationLink(repositories,
                                config,
                                baseEntityUri,
                                propertyMapping,
                                persistentProperty,
                                links);
      }
    });
    jsonSchema.add(links);

    return jsonSchema;
  }

}
