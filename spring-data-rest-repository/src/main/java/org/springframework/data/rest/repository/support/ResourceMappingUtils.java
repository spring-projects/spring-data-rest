package org.springframework.data.rest.repository.support;

import static org.springframework.core.annotation.AnnotationUtils.*;
import static org.springframework.util.StringUtils.*;

import java.lang.reflect.Method;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.annotation.RestResource;

/**
 * Helper methods to get the default rel and path values or to use values supplied by annotations.
 *
 * @author Jon Brisbin
 */
public abstract class ResourceMappingUtils {

  protected ResourceMappingUtils() {
  }

  public static String findRel(Class<?> type) {
    RestResource anno;
    if(null != (anno = findAnnotation(type, RestResource.class))) {
      if(hasText(anno.rel())) {
        return anno.rel();
      }
    }

    return uncapitalize(type.getSimpleName().replaceAll("Repository", ""));
  }

  public static String findRel(Method method) {
    RestResource anno;
    if(null != (anno = findAnnotation(method, RestResource.class))) {
      if(hasText(anno.rel())) {
        return anno.rel();
      }
    }

    return method.getName();
  }

  public static String findPath(Class<?> type) {
    RestResource anno;
    if(null != (anno = findAnnotation(type, RestResource.class))) {
      if(hasText(anno.path())) {
        return anno.path();
      }
    }

    return uncapitalize(type.getSimpleName().replaceAll("Repository", ""));
  }

  public static String findPath(Method method) {
    RestResource anno;
    if(null != (anno = findAnnotation(method, RestResource.class))) {
      if(hasText(anno.path())) {
        return anno.path();
      }
    }

    return method.getName();
  }

  public static boolean findExported(Class<?> type) {
    RestResource anno;
    return null == (anno = findAnnotation(type, RestResource.class)) || anno.exported();
  }

  public static boolean findExported(Method method) {
    RestResource anno;
    return null == (anno = findAnnotation(method, RestResource.class)) || anno.exported();
  }

  public static ResourceMapping getResourceMapping(RepositoryRestConfiguration config,
                                                   PersistentEntity persistentEntity) {
    if(null == persistentEntity) {
      return null;
    }
    Class<?> domainType = persistentEntity.getType();
    ResourceMapping mapping = (null != config ? config.getResourceMappingForDomainType(domainType) : null);
    return merge(domainType, mapping);
  }

  public static ResourceMapping getResourceMapping(RepositoryRestConfiguration config,
                                                   RepositoryInformation repoInfo) {
    if(null == repoInfo) {
      return null;
    }
    Class<?> repoType = repoInfo.getRepositoryInterface();
    ResourceMapping mapping = (null != config ? config.getResourceMappingForRepository(repoType) : null);
    return merge(repoType, mapping);
  }

  public static ResourceMapping merge(Method method, ResourceMapping mapping) {
    ResourceMapping defaultMapping = new ResourceMapping(
        findRel(method),
        findPath(method),
        findExported(method)
    );
    if(null != mapping) {
      return new ResourceMapping(
          (null != mapping.getRel() ? mapping.getRel() : defaultMapping.getRel()),
          (null != mapping.getPath() ? mapping.getPath() : defaultMapping.getPath()),
          (mapping.isExported() != defaultMapping.isExported() ? mapping.isExported() : defaultMapping.isExported())
      );
    }
    return defaultMapping;
  }

  public static ResourceMapping merge(Class<?> type, ResourceMapping mapping) {
    ResourceMapping defaultMapping = new ResourceMapping(
        findRel(type),
        findPath(type),
        findExported(type)
    );
    if(null != mapping) {
      return new ResourceMapping(
          (null != mapping.getRel() ? mapping.getRel() : defaultMapping.getRel()),
          (null != mapping.getPath() ? mapping.getPath() : defaultMapping.getPath()),
          (mapping.isExported() != defaultMapping.isExported() ? mapping.isExported() : defaultMapping.isExported()))
          .addResourceMappings(mapping.getResourceMappings());
    }
    return defaultMapping;
  }

}
