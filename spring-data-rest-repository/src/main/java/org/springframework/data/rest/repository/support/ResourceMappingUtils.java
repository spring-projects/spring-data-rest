package org.springframework.data.rest.repository.support;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.annotation.RestResource;

import static java.util.Arrays.asList;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.data.rest.repository.support.ResourceStringUtils.hasText;
import static org.springframework.data.rest.repository.support.ResourceStringUtils.removeLeadingSlash;
import static org.springframework.util.StringUtils.uncapitalize;

/**
 * Helper methods to get the default rel and path values or to use values supplied by annotations.
 *
 * @author Jon Brisbin
 */
public abstract class ResourceMappingUtils {

  private final static Logger LOG = LoggerFactory.getLogger(ResourceMappingUtils.class);

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

  public static String formatRel(RepositoryRestConfiguration config,
                                 RepositoryInformation repoInfo,
                                 PersistentProperty persistentProperty) {
    if(null == persistentProperty) {
      return null;
    }

    ResourceMapping repoMapping = getResourceMapping(config, repoInfo);
    ResourceMapping entityMapping = getResourceMapping(config, persistentProperty.getOwner());
    ResourceMapping propertyMapping = entityMapping.getResourceMappingFor(persistentProperty.getName());

    return String.format("%s.%s.%s",
            repoMapping.getRel(),
            entityMapping.getRel(),
            (null != propertyMapping ? propertyMapping.getRel() : persistentProperty.getName()));
  }

  public static String findPath(Class<?> type) {
    RestResource anno;
    if(null != (anno = findAnnotation(type, RestResource.class))) {
      if(hasText(anno.path())) {
        return removeLeadingSlash(anno.path());
      }
    }

    return uncapitalize(type.getSimpleName().replaceAll("Repository", ""));
  }

  public static String findPath(Method method) {
    RestResource anno;
    if(null != (anno = findAnnotation(method, RestResource.class))) {
      if(hasText(anno.path())) {
        return removeLeadingSlash(anno.path());
      }
    }

    return method.getName();
  }

  public static boolean findExported(Class<?> type) {
    RestResource anno;
    return null == (anno = findAnnotation(type, RestResource.class)) || anno.exported();
  }

  /**
   * The provided method is marked as exported if not explicitly mentioned otherwise
   * and if all its relevant parameters are annotated with {@link Param}.
   */
  public static boolean findExported(Method method) {
    RestResource anno;
    anno = findAnnotation(method, RestResource.class);
    if (anno != null && !anno.exported()) {
      return false;
    }
    boolean result = allEntityParametersAnnotated(method, Param.class);
    if (!result) {
      LOG.warn("Method {} will not be exposed. One of its parameters is not annotated with @Param.", method);
    }
    return result;
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

  public static ResourceMapping getResourceMapping(RepositoryRestConfiguration config,
                                                   PersistentEntity persistentEntity) {
    if(null == persistentEntity) {
      return null;
    }
    Class<?> domainType = persistentEntity.getType();
    ResourceMapping mapping = (null != config ? config.getResourceMappingForDomainType(domainType) : null);
    return merge(domainType, mapping);
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

  private static boolean allEntityParametersAnnotated(Method method, Class<? extends Annotation> annotationClass) {
    Class<?>[][] actualAnnotationTypes = annotationTypes(method.getParameterAnnotations());
    Class<?>[] actualParameterTypes = method.getParameterTypes();
    for (int i = 0; i < actualParameterTypes.length; i++) {
      Class<?> parameterType = actualParameterTypes[i];
      Class<?>[] parameterAnnotationTypes = actualAnnotationTypes[i];
      if (isEntityParameter(parameterType)
          && !isExpectedAnnotationSet(annotationClass, parameterAnnotationTypes)) {
        return false;
      }
    }
    return true;
  }

  private static Class<?>[][] annotationTypes(Annotation[][] annotations) {
    final int length = annotations.length;
    Class<?>[][] result = new Class<?>[length][];
    for (int i = 0; i < length; i++) {
      Annotation[] parameterAnnotations = annotations[i];
      final int paramAnnotationCount = parameterAnnotations.length;
      result[i] = new Class<?>[paramAnnotationCount];
      for (int j = 0; j < paramAnnotationCount; j++) {
        result[i][j] = parameterAnnotations[j].annotationType();
      }
    }
    return result;
  }

  private static boolean isEntityParameter(Class<?> parameterType) {
    return /* paging and sorting */
           !Pageable.class.isAssignableFrom(parameterType)
           && !Sort.class.isAssignableFrom(parameterType)
           /* base repositories arguments */
           && parameterType != Object.class
           && parameterType != Iterable.class
           && parameterType != Serializable.class;
  }

  private static boolean isExpectedAnnotationSet(Class<? extends Annotation> annotationClass, Class<?>[] parameterAnnotationTypes) {
    return asList(parameterAnnotationTypes).contains(annotationClass);
  }


}
