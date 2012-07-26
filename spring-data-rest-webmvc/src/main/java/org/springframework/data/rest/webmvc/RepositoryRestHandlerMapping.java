package org.springframework.data.rest.webmvc;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.data.rest.repository.RepositoryExporter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * @author Jon Brisbin
 */
public class RepositoryRestHandlerMapping extends RequestMappingHandlerMapping {

  @Autowired
  private EntityManagerFactory entityManagerFactory;
  @Autowired(required = false)
  private List<RepositoryExporter> repositoryExporters = Collections.emptyList();
  private Set<String>              repositoryNames     = new HashSet<String>();

  public RepositoryRestHandlerMapping() {
    setOrder(Ordered.HIGHEST_PRECEDENCE);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request)
      throws Exception {
    if(repositoryNames.isEmpty() && !repositoryExporters.isEmpty()) {
      for(RepositoryExporter re : repositoryExporters) {
        repositoryNames.addAll(re.repositoryNames());
      }
    }
    String[] parts = lookupPath.split("/");
    if(parts.length == 0) {
      // Root request
      return super.lookupHandlerMethod(lookupPath, request);
    } else {
      if(repositoryNames.contains(parts[1])) {
        return super.lookupHandlerMethod(lookupPath, request);
      } else {
        return null;
      }
    }
  }

  @Override protected boolean isHandler(Class<?> beanType) {
    return RepositoryRestController.class.isAssignableFrom(beanType);
  }

  @Override protected void extendInterceptors(List<Object> interceptors) {
    if(null != entityManagerFactory) {
      OpenEntityManagerInViewInterceptor omivi = new OpenEntityManagerInViewInterceptor();
      omivi.setEntityManagerFactory(entityManagerFactory);
      interceptors.add(omivi);
    }
  }

}
