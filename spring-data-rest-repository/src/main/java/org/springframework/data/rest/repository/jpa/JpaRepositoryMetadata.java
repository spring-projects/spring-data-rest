package org.springframework.data.rest.repository.jpa;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.annotation.RestPathSegment;
import org.springframework.util.ReflectionUtils;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class JpaRepositoryMetadata<R extends Repository<Object, Serializable>> implements RepositoryMetadata<R, JpaEntityMetadata> {

  private final String name;
  private final Class<?> repoClass;
  private final R repository;
  private final EntityInformation entityInfo;
  private final Map<String, Method> queryMethods = new HashMap<String, Method>();
  private JpaEntityMetadata entityMetadata;

  @SuppressWarnings({"unchecked"})
  public JpaRepositoryMetadata(Repositories repositories,
                               String name,
                               final Class<?> repoClass,
                               R repository,
                               EntityInformation entityInfo,
                               EntityManager entityManager) {
    this.name = name;
    this.repoClass = repoClass;
    this.repository = repository;
    this.entityInfo = entityInfo;

    ReflectionUtils.doWithMethods(
        repoClass,
        new ReflectionUtils.MethodCallback() {
          @Override public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
            String pathSeg = AnnotationUtils.findAnnotation(method, RestPathSegment.class).value();
            ReflectionUtils.makeAccessible(method);
            queryMethods.put(pathSeg, method);
          }
        },
        new ReflectionUtils.MethodFilter() {
          @Override public boolean matches(Method method) {
            return (!method.isSynthetic()
                && !method.isBridge()
                && method.getDeclaringClass() != Object.class
                && !method.getName().contains("$")
                && null != AnnotationUtils.findAnnotation(method, RestPathSegment.class));
          }
        }
    );

    Metamodel metamodel = entityManager.getMetamodel();
    entityMetadata = new JpaEntityMetadata(repositories, metamodel.entity(entityInfo.getJavaType()));
  }

  @Override public String name() {
    return name;
  }

  @Override public Class<? extends Object> domainType() {
    return entityMetadata.type();
  }

  @Override public R repository() {
    return repository;
  }

  @Override public JpaEntityMetadata entityMetadata() {
    return entityMetadata;
  }

  @Override public Method queryMethod(String key) {
    return queryMethods.get(key);
  }

  @Override public Map<String, Method> queryMethods() {
    return Collections.unmodifiableMap(queryMethods);
  }

}
