package org.springframework.data.rest.repository;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class JpaRepositoryMetadata implements InitializingBean, ApplicationContextAware {

  private ApplicationContext applicationContext;
  private Map<Class<?>, RepositoryCacheEntry> repositories = new HashMap<Class<?>, RepositoryCacheEntry>();
  private EntityManager entityManager;
  private Metamodel metamodel;

  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
    this.metamodel = entityManager.getMetamodel();
  }

  public EntityManager entityManager() {
    return this.entityManager;
  }

  @SuppressWarnings({"unchecked"})
  public <T> CrudRepository<T, ? extends Serializable> repositoryFor(String name) {
    if (null != name) {
      for (Map.Entry<Class<?>, RepositoryCacheEntry> entry : repositories.entrySet()) {
        if (name.equals(repositoryNameFor(entry.getValue().repository))) {
          return entry.getValue().repository;
        }
      }
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  public <T> CrudRepository<T, ? extends Serializable> repositoryFor(Class<T> domainClass) {
    RepositoryCacheEntry entry = repositories.get(domainClass);
    if (null != entry) {
      return entry.repository;
    }
    return null;
  }

  @SuppressWarnings({"unchecked"})
  public <T> EntityInformation<T, ? extends Serializable> entityInfoFor(Class<T> domainClass) {
    RepositoryCacheEntry entry = repositories.get(domainClass);
    if (null != entry) {
      return entry.entityInfo;
    }
    return null;
  }

  public <T> EntityType<T> entityTypeFor(Class<T> domainClass) {
    return metamodel.entity(domainClass);
  }

  @SuppressWarnings({"unchecked"})
  public <T> EntityInformation<T, ? extends Serializable> entityInfoFor(CrudRepository<T, ? extends Serializable> repository) {
    for (Map.Entry<Class<?>, RepositoryCacheEntry> entry : repositories.entrySet()) {
      if (entry.getValue().repository == repository) {
        return entry.getValue().entityInfo;
      }
    }
    return null;
  }

  public <T> JpaEntityMetadata entityMetadataFor(Class<T> domainClass) {
    RepositoryCacheEntry entry = repositories.get(domainClass);
    if (null == entry.entityMetadata) {
      entry.entityMetadata = new JpaEntityMetadata(metamodel.entity(domainClass), this);
    }
    return entry.entityMetadata;
  }

  public String repositoryNameFor(Class<?> domainClass) {
    RepositoryCacheEntry entry = repositories.get(domainClass);
    if (null != entry) {
      return entry.name;
    }
    return null;
  }

  public String repositoryNameFor(CrudRepository repository) {
    for (Map.Entry<Class<?>, RepositoryCacheEntry> entry : repositories.entrySet()) {
      if (entry.getValue().repository == repository) {
        return entry.getValue().name;
      }
    }
    return null;
  }

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public List<String> repositoryNames() {
    List<String> names = new ArrayList<String>();
    for (Map.Entry<Class<?>, RepositoryCacheEntry> entry : repositories.entrySet()) {
      names.add(entry.getValue().name);
    }
    return names;
  }

  public void setRepositories(Collection<? extends CrudRepository> repositories) {
    for (CrudRepository repository : repositories) {
      Class<?> repoClass = AopUtils.getTargetClass(repository);
      Field infoField = ReflectionUtils.findField(repoClass, "entityInformation");
      ReflectionUtils.makeAccessible(infoField);
      Method m = ReflectionUtils.findMethod(repository.getClass(), "getTargetSource");
      ReflectionUtils.makeAccessible(m);
      try {
        SingletonTargetSource targetRepo = (SingletonTargetSource) m.invoke(repository);
        EntityInformation entityInfo = (EntityInformation) infoField.get(targetRepo.getTarget());
        Class<?>[] intfs = repository.getClass().getInterfaces();
        String name = StringUtils.uncapitalize(intfs[0].getSimpleName().replaceAll("Repository", ""));
        this.repositories.put(entityInfo.getJavaType(), new RepositoryCacheEntry(name, repository, entityInfo, null));
      } catch (Throwable t) {
        throw new IllegalStateException(t);
      }
    }
  }

  @Override public void afterPropertiesSet() throws Exception {
    if (this.repositories.isEmpty()) {
      ApplicationContext appCtx = applicationContext;
      while (null != appCtx) {
        Map<String, CrudRepository> beans = appCtx.getBeansOfType(CrudRepository.class);
        setRepositories(beans.values());
        appCtx = appCtx.getParent();
      }
    }
  }

  private class RepositoryCacheEntry {
    String name;
    CrudRepository repository;
    EntityInformation entityInfo;
    JpaEntityMetadata entityMetadata;

    private RepositoryCacheEntry(String name,
                                 CrudRepository repository,
                                 EntityInformation entityInfo,
                                 JpaEntityMetadata entityMetadata) {
      this.name = name;
      this.repository = repository;
      this.entityInfo = entityInfo;
      this.entityMetadata = entityMetadata;
    }
  }

}
