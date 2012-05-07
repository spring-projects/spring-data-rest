package org.springframework.data.rest.webmvc;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class RepositoryRestExporterServlet extends DispatcherServlet {

  public RepositoryRestExporterServlet() {
    configure();
  }

  public RepositoryRestExporterServlet(WebApplicationContext webApplicationContext) {
    super(webApplicationContext);
    configure();
  }

  private void configure() {
    setContextClass(AnnotationConfigWebApplicationContext.class);
    setContextConfigLocation(RepositoryRestMvcConfiguration.class.getName());
  }

}
