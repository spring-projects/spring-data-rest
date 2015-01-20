/*
 * Copyright 2014 the original author or authors.
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

import static org.springframework.util.StringUtils.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

/**
 * Value object to be able to extract the lookup path within a configured base URI that forms a URI namespace.
 * 
 * @author Oliver Gierke
 */
public class BaseUri {

	public static final BaseUri NONE = new BaseUri(URI.create(""));
	private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

	private final URI baseUri;

	/**
	 * Creates a new {@link BaseUri} with the given URI as base.
	 * 
	 * @param uri must not be {@literal null}.
	 */
	public BaseUri(URI uri) {

		Assert.notNull(uri, "Base URI must not be null!");

		String uriString = uri.toString();
		this.baseUri = URI.create(trimTrailingCharacter(trimTrailingCharacter(uriString, '/'), '/'));
	}

	/**
	 * Creates a new {@link BaseUri} with the given URI as base.
	 * 
	 * @param uri must not be {@literal null}.
	 */
	public BaseUri(String uri) {
		this(URI.create(uri));
	}

	/**
	 * Returns the base URI.
	 * 
	 * @return
	 */
	public URI getUri() {
		return baseUri;
	}

	/**
	 * Extracts the actual lookup path within the Spring Data REST managed URI space. This includes stripping the
	 * necessary parts of the base URI from the source lookup path.
	 * 
	 * @param request must not be {@literal null}.
	 * @return the stripped lookup path with then the repository URI space or {@literal null} in case the lookup path is
	 *         not pointing into the repository URI space.
	 */
	public String getRepositoryLookupPath(NativeWebRequest request) {
		return getRepositoryLookupPath(request.getNativeRequest(HttpServletRequest.class));
	}

	/**
	 * Extracts the actual lookup path within the Spring Data REST managed URI space. This includes stripping the
	 * necessary parts of the base URI from the source lookup path.
	 * 
	 * @param request must not be {@literal null}.
	 * @return the stripped lookup path with then the repository URI space or {@literal null} in case the lookup path is
	 *         not pointing into the repository URI space.
	 */
	private String getRepositoryLookupPath(HttpServletRequest request) {

		String lookupPath = URL_PATH_HELPER.getLookupPathForRequest(request);
		return getRepositoryLookupPath(lookupPath);
	}

	/**
	 * Extracts the actual lookup path within the Spring Data REST managed URI space. This includes stripping the
	 * necessary parts of the base URI from the source lookup path.
	 * 
	 * @param lookupPath must not be {@literal null}.
	 * @return the stripped lookup path with then the repository URI space or {@literal null} in case the lookup path is
	 *         not pointing into the repository URI space.
	 */
	public String getRepositoryLookupPath(String lookupPath) {

		Assert.notNull(lookupPath, "Lookup path must not be null!");

		lookupPath = lookupPath.contains("{") ? lookupPath.substring(0, lookupPath.indexOf('{')) : lookupPath;
		lookupPath = trimTrailingCharacter(lookupPath, '/');

		if (!baseUri.isAbsolute()) {

			String uri = baseUri.toString();

			if (!StringUtils.hasText(uri)) {
				return lookupPath;
			}

			uri = uri.startsWith("/") ? uri : "/".concat(uri);
			return lookupPath.startsWith(uri) ? lookupPath.substring(uri.length(), lookupPath.length()) : null;
		}

		List<String> baseUriSegments = UriComponentsBuilder.fromUri(baseUri).build().getPathSegments();
		Collections.reverse(baseUriSegments);
		String tail = "";

		for (String tailSegment : baseUriSegments) {

			tail = "/".concat(tailSegment).concat(tail);

			if (lookupPath.startsWith(tail)) {
				return lookupPath.substring(tail.length(), lookupPath.length());
			}
		}

		return null;
	}

	/**
	 * Returns a new {@link UriComponentsBuilder} for the base URI. If the base URI is not absolute, it'll lokup the URI
	 * for the current servlet mapping and extend it accordingly.
	 * 
	 * @return
	 */
	public UriComponentsBuilder getUriComponentsBuilder() {

		if (baseUri.isAbsolute()) {
			return UriComponentsBuilder.fromUri(baseUri);
		}

		return ServletUriComponentsBuilder.fromCurrentServletMapping().path(baseUri.toString());
	}
}
