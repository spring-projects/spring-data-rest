package org.springframework.data.rest.webmvc;

import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Special {@link DispatcherServlet} subclass that certain exporter components can recognize.
 *
 * @author Jon Brisbin
 */
public class RepositoryRestDispatcherServlet extends DispatcherServlet {
  public RepositoryRestDispatcherServlet(WebApplicationContext webApplicationContext) {
    super(webApplicationContext);
    setContextClass(AnnotationConfigWebApplicationContext.class);
    setContextConfigLocation(RepositoryRestMvcConfiguration.class.getName());
  }
}
