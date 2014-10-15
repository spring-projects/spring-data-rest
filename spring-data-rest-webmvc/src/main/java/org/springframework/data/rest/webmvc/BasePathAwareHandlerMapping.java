/*
 * Copyright 2014-2015 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.springframework.core.Ordered;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * A {@link RequestMappingHandlerMapping} that augments the request mappings
 * 
 * @author Oliver Gierke
 */
public class BasePathAwareHandlerMapping extends RequestMappingHandlerMapping {

	private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

	private final RepositoryRestConfiguration configuration;

	private String prefix;

	/**
	 * Creates a new {@link BasePathAwareHandlerMapping} using the given {@link RepositoryRestConfiguration}.
	 * 
	 * @param configuration must not be {@literal null}.
	 */
	public BasePathAwareHandlerMapping(RepositoryRestConfiguration configuration) {

		Assert.notNull(configuration, "RepositoryRestConfiguration must not be null!");
		this.configuration = configuration;
		setOrder(Ordered.LOWEST_PRECEDENCE - 150);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#getMappingForMethod(java.lang.reflect.Method, java.lang.Class)
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {

		RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

		if (info == null) {
			return null;
		}

		PatternsRequestCondition patternsCondition = info.getPatternsCondition();

		Set<String> patterns = patternsCondition.getPatterns();
		String[] augmentedPatterns = new String[patterns.size()];
		int count = 0;

		for (String pattern : patterns) {
			augmentedPatterns[count++] = prefix.concat(pattern);
		}

		PatternsRequestCondition condition = new PatternsRequestCondition(augmentedPatterns, getUrlPathHelper(),
				getPathMatcher(), useSuffixPatternMatch(), useTrailingSlashMatch(), getFileExtensions());

		return new RequestMappingInfo(condition, info.getMethodsCondition(), info.getParamsCondition(),
				info.getHeadersCondition(), info.getConsumesCondition(), info.getProducesCondition(), info.getCustomCondition());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler(java.lang.Class)
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return beanType.getAnnotation(BasePathAwareController.class) != null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		URI baseUri = configuration.getBaseUri();

		if (baseUri.isAbsolute()) {
			HttpServletRequest request = new UriAwareHttpServletRequest(getServletContext(), baseUri);
			this.prefix = URL_PATH_HELPER.getPathWithinApplication(request);
		} else {
			this.prefix = baseUri.toString();
		}

		super.afterPropertiesSet();
	}

	private class UriAwareHttpServletRequest implements HttpServletRequest {

		private final ServletContext context;
		private final String path;

		/**
		 * @param context
		 * @param uri
		 */
		public UriAwareHttpServletRequest(ServletContext context, URI uri) {
			this.context = context;
			this.path = uri.getPath();
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
		 */
		@Override
		public Object getAttribute(String name) {
			return null;
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.ServletRequest#getCharacterEncoding()
		 */
		@Override
		public String getCharacterEncoding() {
			return null;
		}

		@Override
		public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getContentLength() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContentType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getParameter(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Enumeration<String> getParameterNames() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String[] getParameterValues(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getProtocol() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getScheme() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getServerName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getServerPort() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BufferedReader getReader() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRemoteAddr() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRemoteHost() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setAttribute(String name, Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeAttribute(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Locale getLocale() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Enumeration<Locale> getLocales() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isSecure() {
			throw new UnsupportedOperationException();
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRealPath(String path) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getRemotePort() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLocalName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLocalAddr() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getLocalPort() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.ServletRequest#getServletContext()
		 */
		@Override
		public ServletContext getServletContext() {
			return context;
		}

		@Override
		public AsyncContext startAsync() throws IllegalStateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IllegalStateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAsyncStarted() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAsyncSupported() {
			throw new UnsupportedOperationException();
		}

		@Override
		public AsyncContext getAsyncContext() {
			throw new UnsupportedOperationException();
		}

		@Override
		public DispatcherType getDispatcherType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getAuthType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Cookie[] getCookies() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long getDateHeader(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getHeader(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getIntHeader(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getMethod() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPathInfo() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPathTranslated() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequest#getContextPath()
		 */
		@Override
		public String getContextPath() {
			return context.getContextPath();
		}

		@Override
		public String getQueryString() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRemoteUser() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isUserInRole(String role) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Principal getUserPrincipal() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRequestedSessionId() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * @see javax.servlet.http.HttpServletRequest#getRequestURI()
		 */
		@Override
		public String getRequestURI() {
			return path;
		}

		@Override
		public StringBuffer getRequestURL() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getServletPath() {
			throw new UnsupportedOperationException();
		}

		@Override
		public HttpSession getSession(boolean create) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HttpSession getSession() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isRequestedSessionIdValid() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isRequestedSessionIdFromCookie() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isRequestedSessionIdFromURL() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isRequestedSessionIdFromUrl() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void login(String username, String password) throws ServletException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void logout() throws ServletException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Part> getParts() throws IOException, ServletException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Part getPart(String name) throws IOException, ServletException {
			throw new UnsupportedOperationException();
		}
	}
}
