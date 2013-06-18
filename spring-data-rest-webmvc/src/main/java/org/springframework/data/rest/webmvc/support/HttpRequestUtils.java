package org.springframework.data.rest.webmvc.support;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.springframework.data.rest.webmvc.RepositoryRestDispatcherServlet;

/**
 * Helper class to HttpServletRequest helpers.
 * 
 * @author Jon Brisbin
 */
public abstract class HttpRequestUtils {

	/**
	 * Strip a servlet registration mapping from the request URI.
	 * 
	 * @param requestUri The request URI to strip.
	 * @param ctx The servlet context in which to search for registration mappings.
	 * @return The stripped request URI.
	 */
	public static String stripRegistrationMapping(String requestUri, ServletContext ctx) {
		for (ServletRegistration reg : ctx.getServletRegistrations().values()) {
			if (reg.getClassName().equals(RepositoryRestDispatcherServlet.class.getName())
					|| reg.getName().equals("rest-exporter")) {
				for (String mapping : reg.getMappings()) {
					if (mapping.contains("*")) {
						mapping = mapping.substring(0, mapping.indexOf('*'));
					}
					if (requestUri.startsWith(mapping)) {
						return requestUri.replaceAll(mapping, "");
					}
				}
			}
		}
		return requestUri;
	}

}
