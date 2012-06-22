package org.springframework.data.rest.webmvc;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.springframework.core.Ordered;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * @author Jon Brisbin
 */
public class RepositoryExporterHandlerMapping extends RequestMappingHandlerMapping {

  private EntityManagerFactory entityManagerFactory;

  public RepositoryExporterHandlerMapping(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
    setOrder(Ordered.HIGHEST_PRECEDENCE);
  }

  @Override protected boolean isHandler(Class<?> beanType) {
    return RepositoryRestController.class.isAssignableFrom(beanType);
  }

  @Override protected void extendInterceptors(List<Object> interceptors) {
    OpenEntityManagerInViewInterceptor omivi = new OpenEntityManagerInViewInterceptor();
    omivi.setEntityManagerFactory(entityManagerFactory);
    interceptors.add(omivi);
  }

}
