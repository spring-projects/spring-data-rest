package org.springframework.data.rest.repository;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.rest.repository.support.RepositoryInformationSupport;

/**
 * A {@link ConditionalGenericConverter} that can convert a {@link URI} domain entity.
 *
 * @author Jon Brisbin
 */
public class UriDomainClassConverter
    extends RepositoryInformationSupport
    implements ConditionalGenericConverter,
               InitializingBean {

  private static TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);

  @Autowired
  private DomainClassConverter domainClassConverter;
  private Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();

  @Override public void afterPropertiesSet() throws Exception {
    for(Class<?> domainType : repositories) {
      convertiblePairs.add(new ConvertiblePair(URI.class, domainType));
    }
  }

  @Override public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return URI.class.isAssignableFrom(sourceType.getType())
        && (null != repositories.getPersistentEntity(targetType.getType()));
  }

  @Override public Set<ConvertiblePair> getConvertibleTypes() {
    return convertiblePairs;
  }

  @Override public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    PersistentEntity entity = repositories.getPersistentEntity(targetType.getType());
    if(null == entity || !domainClassConverter.matches(STRING_TYPE, targetType)) {
      throw new ConversionFailedException(
          sourceType,
          targetType,
          source,
          new IllegalArgumentException("No PersistentEntity information available for " + targetType.getType())
      );
    }

    URI uri = (URI)source;
    String[] parts = uri.getPath().split("/");
    if(parts.length < 2) {
      throw new ConversionFailedException(
          sourceType,
          targetType,
          source,
          new IllegalArgumentException("Cannot resolve URI " + uri + ". Is it local or remote? Only local URIs are resolvable.")
      );
    }

    return domainClassConverter.convert(parts[parts.length - 1], STRING_TYPE, targetType);
  }

}
