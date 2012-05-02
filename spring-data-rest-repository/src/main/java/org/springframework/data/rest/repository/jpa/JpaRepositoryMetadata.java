package org.springframework.data.rest.repository.jpa;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.RepositoryQueryMethod;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class JpaRepositoryMetadata<R extends Repository<Object, Serializable>> implements RepositoryMetadata<R, JpaEntityMetadata> {

  private final String name;
  private final Class<? extends Repository<? extends Object, ? extends Serializable>> repoClass;
  private final R repository;
  private final EntityInformation entityInfo;
  private final Map<String, RepositoryQueryMethod> queryMethods = new HashMap<String, RepositoryQueryMethod>();

  private String rel;
  private JpaEntityMetadata entityMetadata;

  @SuppressWarnings({"unchecked"})
  public JpaRepositoryMetadata(Repositories repositories,
                               String name,
                               final Class<? extends Repository<? extends Object, ? extends Serializable>> repoClass,
                               R repository,
                               EntityInformation entityInfo,
                               EntityManager entityManager) {
    this.name = name;
    this.repoClass = repoClass;
    this.repository = repository;
    this.entityInfo = entityInfo;

    RestResource resourceAnno = repoClass.getAnnotation(RestResource.class);
    if (null != resourceAnno && StringUtils.hasText(resourceAnno.rel())) {
      rel = resourceAnno.rel();
    } else {
      rel = name;
    }

    ReflectionUtils.doWithMethods(
        repoClass,
        new ReflectionUtils.MethodCallback() {
          @Override public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
            RestResource resourceAnno = method.getAnnotation(RestResource.class);
            String pathSeg = resourceAnno.path();
            ReflectionUtils.makeAccessible(method);
            queryMethods.put(pathSeg, new RepositoryQueryMethod(method));
          }
        },
        new ReflectionUtils.MethodFilter() {
          @Override public boolean matches(Method method) {
            return (!method.isSynthetic()
                && !method.isBridge()
                && method.getDeclaringClass() != Object.class
                && !method.getName().contains("$")
                && null != method.getAnnotation(RestResource.class));
          }
        }
    );

    Metamodel metamodel = entityManager.getMetamodel();
    entityMetadata = new JpaEntityMetadata(repositories, metamodel.entity(entityInfo.getJavaType()));
  }

  @Override public String name() {
    return name;
  }

  @Override public String rel() {
    return rel;
  }

  @Override public Class<? extends Object> domainType() {
    return entityMetadata.type();
  }

  @Override public Class<? extends Repository<? extends Object, ? extends Serializable>> repositoryClass() {
    return repoClass;
  }

  @Override public R repository() {
    return repository;
  }

  @Override public JpaEntityMetadata entityMetadata() {
    return entityMetadata;
  }

  @Override public RepositoryQueryMethod queryMethod(String key) {
    return queryMethods.get(key);
  }

  @Override public Map<String, RepositoryQueryMethod> queryMethods() {
    return Collections.unmodifiableMap(queryMethods);
  }

  @Override public String toString() {
    return "JpaRepositoryMetadata{" +
        "name='" + name + '\'' +
        ", repoClass=" + repoClass +
        ", repository=" + repository +
        ", entityInfo=" + entityInfo +
        ", queryMethods=" + queryMethods +
        ", entityMetadata=" + entityMetadata +
        '}';
  }

}
