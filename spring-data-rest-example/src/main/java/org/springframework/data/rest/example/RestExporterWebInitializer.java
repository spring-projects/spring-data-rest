package org.springframework.data.rest.example;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.data.rest.example.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.example.mongodb.MongoDbRepositoryConfig;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author Jon Brisbin
 */
public class RestExporterWebInitializer implements WebApplicationInitializer {

	@Override public void onStartup(ServletContext servletContext) throws ServletException {
		AnnotationConfigWebApplicationContext rootCtx = new AnnotationConfigWebApplicationContext();
		rootCtx.register(
				JpaRepositoryConfig.class,
				MongoDbRepositoryConfig.class
				//GemfireRepositoryConfig.class,
				//Neo4jRepositoryConfig.class
		);

		servletContext.addListener(new ContextLoaderListener(rootCtx));
		//    servletContext.addFilter("springSecurity", DelegatingFilterProxy.class);
		//    servletContext.getFilterRegistration("springSecurity").addMappingForUrlPatterns(
		//        EnumSet.of(DispatcherType.REQUEST),
		//        false,
		//        "/*"
		//    );

		AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();
		webCtx.register(RestExporterExampleRestConfig.class);

		DispatcherServlet dispatcherServlet = new DispatcherServlet(webCtx);
		ServletRegistration.Dynamic reg = servletContext.addServlet("rest-exporter", dispatcherServlet);
		reg.setLoadOnStartup(1);
		reg.addMapping("/*");

	}

}
