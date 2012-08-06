package org.springframework.data.rest.webmvc;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Convenience {@link DispatcherServlet} that sets the 'contextClass' and 'contextConfigLocation' properties to the
 * correct values for using the REST exporter in a web.xml file.
 *
 * @author Jon Brisbin
 */
public class RepositoryRestExporterServlet extends DispatcherServlet {

  private static final long serialVersionUID = 1L;

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
