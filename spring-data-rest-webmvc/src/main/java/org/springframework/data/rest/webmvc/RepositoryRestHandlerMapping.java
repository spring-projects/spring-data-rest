package org.springframework.data.rest.webmvc;

import static org.springframework.util.StringUtils.*;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link RequestMappingHandlerMapping} implementation that will only find a handler method if a
 * {@link org.springframework.data.repository.Repository} is exported under that URL path segment. Also ensures the
 * {@link OpenEntityManagerInViewInterceptor} is registered in the application context. The OEMIVI is required for the
 * REST exporter to function properly.
 * 
 * @author Jon Brisbin
 */
public class RepositoryRestHandlerMapping extends RequestMappingHandlerMapping {

	@Autowired private Repositories repositories;
	@Autowired private RepositoryRestConfiguration config;
	@Autowired(required = false) private JpaHelper jpaHelper;

	private final ResourceMappings mappings;

	public RepositoryRestHandlerMapping(ResourceMappings mappings) {
		setOrder(Ordered.LOWEST_PRECEDENCE);
		this.mappings = mappings;
	}

	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest origRequest) throws Exception {
		String acceptType = origRequest.getHeader("Accept");
		if (null == acceptType) {
			acceptType = config.getDefaultMediaType().toString();
		}
		List<MediaType> acceptHeaderTypes = MediaType.parseMediaTypes(acceptType);
		List<MediaType> acceptableTypes = new ArrayList<MediaType>();
		for (MediaType mt : acceptHeaderTypes) {
			if (("*".equals(mt.getType()) && ("*".equals(mt.getSubtype())) || ("application".equals(mt.getType()) && "*"
					.equals(mt.getSubtype())))) {
				mt = config.getDefaultMediaType();
			}
			if (!acceptableTypes.contains(mt)) {
				acceptableTypes.add(mt);
			}
		}
		if (acceptableTypes.size() > 1) {
			acceptType = collectionToDelimitedString(acceptableTypes, ",");
		} else if (acceptableTypes.size() == 1) {
			acceptType = acceptableTypes.get(0).toString();
		} else {
			acceptType = config.getDefaultMediaType().toString();
		}

		HttpServletRequest request = new DefaultAcceptTypeHttpServletRequest(origRequest, acceptType);

		String requestUri = lookupPath;
		if (requestUri.startsWith("/")) {
			requestUri = requestUri.substring(1);
		}
		if (!hasText(requestUri)) {
			return super.lookupHandlerMethod(lookupPath, request);
		}
		String[] parts = requestUri.split("/");
		if (parts.length == 0) {
			// Root request
			return super.lookupHandlerMethod(lookupPath, request);
		}

		for (Class<?> domainType : repositories) {
			if (mappings.exportsMappingFor(domainType)) {
				return super.lookupHandlerMethod(lookupPath, request);
			}
		}

		return null;
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotationUtils.findAnnotation(beanType, RepositoryRestController.class) != null;
	}

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		if (null != jpaHelper) {
			for (Object o : jpaHelper.getInterceptors()) {
				interceptors.add(o);
			}
		}
	}

	private static class DefaultAcceptTypeHttpServletRequest extends HttpServletRequestWrapper {
		private final String defaultAcceptType;

		private DefaultAcceptTypeHttpServletRequest(HttpServletRequest request, String defaultAcceptType) {
			super(request);
			this.defaultAcceptType = defaultAcceptType;
		}

		@Override
		public String getHeader(String name) {
			if ("accept".equals(name.toLowerCase())) {
				return defaultAcceptType;
			} else {
				return super.getHeader(name);
			}
		}
	}

}
