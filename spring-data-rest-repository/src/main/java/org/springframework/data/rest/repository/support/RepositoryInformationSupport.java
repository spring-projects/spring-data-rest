package org.springframework.data.rest.repository.support;

import static org.springframework.data.rest.repository.support.ResourceMappingUtils.*;
import static org.springframework.util.ReflectionUtils.*;
import static org.springframework.util.StringUtils.*;

import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.invoke.RepositoryMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Jon Brisbin
 */
public abstract class RepositoryInformationSupport {

  protected Repositories                repositories;
  protected RepositoryRestConfiguration config;
  protected MultiValueMap<Class<?>, RepositoryMethod> repositoryMethods = new LinkedMultiValueMap<Class<?>, RepositoryMethod>();

  public Repositories getRepositories() {
    return repositories;
  }

  @Autowired
  public void setRepositories(Repositories repositories) {
    this.repositories = repositories;
    for(Class<?> domainType : repositories) {
      final RepositoryInformation repoInfo = repositories.getRepositoryInformationFor(domainType);
      doWithMethods(repoInfo.getRepositoryInterface(), new MethodCallback() {
        @Override public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
          repositoryMethods.add(repoInfo.getRepositoryInterface(), new RepositoryMethod(method));
        }
      });
    }
  }

  public RepositoryRestConfiguration getConfig() {
    return config;
  }

  @Autowired
  public void setConfig(RepositoryRestConfiguration config) {
    this.config = config;
  }

  protected RepositoryInformation findRepositoryInfoFor(String pathSegment) {
    if(!hasText(pathSegment)) {
      return null;
    }
    for(Class<?> domainType : repositories) {
      RepositoryInformation repoInfo = findRepositoryInfoFor(domainType);
      ResourceMapping mapping = getResourceMapping(config, repoInfo);
      if(pathSegment.equals(mapping.getPath()) && mapping.isExported()) {
        return repoInfo;
      }
    }
    return null;
  }

  protected RepositoryInformation findRepositoryInfoFor(Class<?> domainType) {
    PersistentEntity entity = repositories.getPersistentEntity(domainType);
    if(null != entity) {
      return repositories.getRepositoryInformationFor(domainType);
    }
    return null;
  }

}
