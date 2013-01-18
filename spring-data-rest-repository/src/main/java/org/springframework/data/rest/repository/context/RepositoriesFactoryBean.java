package org.springframework.data.rest.repository.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.support.Repositories;

/**
 * @author Jon Brisbin
 */
public class RepositoriesFactoryBean implements FactoryBean<Repositories>,
                                                ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override public Repositories getObject() throws Exception {
    return new Repositories(applicationContext);
  }

  @Override public Class<?> getObjectType() {
    return Repositories.class;
  }

  @Override public boolean isSingleton() {
    return false;
  }

}
