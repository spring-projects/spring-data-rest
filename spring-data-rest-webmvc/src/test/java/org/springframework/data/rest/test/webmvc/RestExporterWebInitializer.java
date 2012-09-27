package org.springframework.data.rest.test.webmvc;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.data.rest.webmvc.RepositoryRestExporterServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;

/**
 * @author Jon Brisbin
 */
public class RestExporterWebInitializer implements WebApplicationInitializer {

  @Override public void onStartup(ServletContext servletContext) throws ServletException {
    // Create the 'root' Spring application context
    AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
    rootContext.register(ApplicationConfig.class);

    // Manage the lifecycle of the root application context
    servletContext.addListener(new ContextLoaderListener(rootContext));

    // Register and map the dispatcher servlet
    DispatcherServlet servlet = new RepositoryRestExporterServlet();
    ServletRegistration.Dynamic dispatcher = servletContext.addServlet("dispatcher", servlet);
    dispatcher.setLoadOnStartup(1);
    dispatcher.addMapping("/*");

    new DefaultServletHandlerConfigurer(servletContext).enable();
  }

}
