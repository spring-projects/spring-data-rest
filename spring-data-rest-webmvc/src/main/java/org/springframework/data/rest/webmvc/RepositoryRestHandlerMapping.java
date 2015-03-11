/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link RequestMappingHandlerMapping} implementation that will only find a handler method if a
 * {@link org.springframework.data.repository.Repository} is exported under that URL path segment. Also ensures the
 * {@link OpenEntityManagerInViewInterceptor} is registered in the application context. The OEMIVI is required for the
 * REST exporter to function properly.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class RepositoryRestHandlerMapping extends BasePathAwareHandlerMapping {

	private final ResourceMappings mappings;
	private final RepositoryRestConfiguration configuration;

	private JpaHelper jpaHelper;

	/**
	 * Creates a new {@link RepositoryRestHandlerMapping} for the given {@link ResourceMappings} and
	 * {@link RepositoryRestConfiguration}.
	 * 
	 * @param mappings must not be {@literal null}.
	 * @param config must not be {@literal null}.
	 */
	public RepositoryRestHandlerMapping(ResourceMappings mappings, RepositoryRestConfiguration config) {

		super(config);

		Assert.notNull(mappings, "ResourceMappings must not be null!");
		Assert.notNull(config, "RepositoryRestConfiguration must not be null!");

		this.mappings = mappings;
		this.configuration = config;
	}

	/**
	 * @param jpaHelper the jpaHelper to set
	 */
	public void setJpaHelper(JpaHelper jpaHelper) {
		this.jpaHelper = jpaHelper;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {

		HandlerMethod handlerMethod = super.lookupHandlerMethod(lookupPath, request);

		if (handlerMethod == null) {
			return null;
		}

		String repositoryLookupPath = new BaseUri(configuration.getBaseUri()).getRepositoryLookupPath(lookupPath);

		// Repository root resource
		if (!StringUtils.hasText(repositoryLookupPath)) {
			return handlerMethod;
		}

		return mappings.exportsTopLevelResourceFor(getRepositoryBasePath(repositoryLookupPath)) ? handlerMethod : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping#handleNoMatch(java.util.Set, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos, String lookupPath,
			HttpServletRequest request) throws ServletException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler(java.lang.Class)
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotationUtils.findAnnotation(beanType, RepositoryRestController.class) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#extendInterceptors(java.util.List)
	 */
	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		if (null != jpaHelper) {
			for (Object o : jpaHelper.getInterceptors()) {
				interceptors.add(o);
			}
		}
	}

	/**
	 * Returns the first segment of the given repository lookup path.
	 * 
	 * @param repositoryLookupPath must not be {@literal null}.
	 * @return
	 */
	private static String getRepositoryBasePath(String repositoryLookupPath) {

		int secondSlashIndex = repositoryLookupPath.indexOf('/', repositoryLookupPath.startsWith("/") ? 1 : 0);
		return secondSlashIndex == -1 ? repositoryLookupPath : repositoryLookupPath.substring(0, secondSlashIndex);
	}
}
