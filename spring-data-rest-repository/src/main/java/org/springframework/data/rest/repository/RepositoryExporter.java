package org.springframework.data.rest.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.rest.repository.annotation.RestPathSegment;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class RepositoryExporter<M extends RepositoryMetadata<R, E>,
    R extends Repository<? extends Object, ? extends Serializable>,
    E extends EntityMetadata<? extends AttributeMetadata>>
    implements ApplicationContextAware,
               InitializingBean {

  protected ApplicationContext applicationContext;
  protected EntityManager entityManager;
  protected Map<String, M> repositoryMetadata;

  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @SuppressWarnings({"unchecked"})
  @Override public void afterPropertiesSet() throws Exception {
  }

  public Set<String> repositoryNames() {
    maybeCacheRepositoryFactoryInfo();
    return repositoryMetadata.keySet();
  }

  public boolean hasRepositoryFor(Class<?> domainType) {
    maybeCacheRepositoryFactoryInfo();
    for (M repoMeta : repositoryMetadata.values()) {
      if (repoMeta.domainType().isAssignableFrom(domainType)) {
        return true;
      }
    }
    return false;
  }

  public M repositoryMetadataFor(Class<?> domainType) {
    maybeCacheRepositoryFactoryInfo();
    for (M repoMeta : repositoryMetadata.values()) {
      if (repoMeta.domainType().isAssignableFrom(domainType)) {
        return repoMeta;
      }
    }
    return null;
  }

  public M repositoryMetadataFor(String name) {
    maybeCacheRepositoryFactoryInfo();
    return repositoryMetadata.get(name);
  }

  protected abstract M createRepositoryMetadata(
      Class repoClass,
      R repo,
      String name,
      EntityInformation entityInfo
  );

  @SuppressWarnings({"unchecked"})
  private void maybeCacheRepositoryFactoryInfo() {
    if (null == repositoryMetadata) {
      repositoryMetadata = new HashMap<String, M>();
      Collection<RepositoryFactoryInformation> providers = BeanFactoryUtils.beansOfTypeIncludingAncestors(
          applicationContext,
          RepositoryFactoryInformation.class
      ).values();

      for (RepositoryFactoryInformation entry : providers) {
        EntityInformation entityInfo = entry.getEntityInformation();
        Class repoClass = entry.getRepositoryInterface();
        String name;
        RestPathSegment pathSeg = AnnotationUtils.findAnnotation(repoClass, RestPathSegment.class);
        if (null != pathSeg) {
          name = pathSeg.value();
        } else {
          name = StringUtils.uncapitalize(repoClass.getSimpleName().replaceAll("Repository", ""));
        }
        R repo = (R) BeanFactoryUtils.beanOfTypeIncludingAncestors(applicationContext, repoClass);
        M repoMeta = createRepositoryMetadata(repoClass, repo, name, entityInfo);
        repositoryMetadata.put(name, repoMeta);
      }
    }
  }

}
