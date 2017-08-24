/*
 * Copyright 2016 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Calendar;

import org.springframework.data.auditing.AuditableBeanWrapper;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Value object to prepare {@link HttpHeaders} for {@link PersistentEntityResource} and {@link PersistentEntity}
 * instances.
 * 
 * @author Oliver Gierke
 * @soundtrack Ron Spielman Trio - Matchstick
 */
@RequiredArgsConstructor
public class HttpHeadersPreparer {

	private final @NonNull AuditableBeanWrapperFactory auditableBeanWrapperFactory;

	/**
	 * Returns the default headers to be returned for the given {@link PersistentEntityResource}. Will set {@link ETag}
	 * and {@code Last-Modified} headers if applicable.
	 * 
	 * @param resource can be {@literal null}.
	 * @return
	 */
	public HttpHeaders prepareHeaders(PersistentEntityResource resource) {
		return resource == null ? new HttpHeaders() : prepareHeaders(resource.getPersistentEntity(), resource.getContent());
	}

	/**
	 * Returns the default headers to be returned for the given {@link PersistentEntity} and value. Will set {@link ETag}
	 * and {@code Last-Modified} headers if applicable.
	 * 
	 * @param entity must not be {@literal null}.
	 * @param value must not be {@literal null}.
	 * @return
	 */
	public HttpHeaders prepareHeaders(PersistentEntity<?, ?> entity, Object value) {

		Assert.notNull(entity, "PersistentEntity must not be null!");
		Assert.notNull(value, "Entity value must not be null!");

		// Add ETag
		HttpHeaders headers = ETag.from(entity, value).addTo(new HttpHeaders());

		// Add Last-Modified
		AuditableBeanWrapper wrapper = getAuditableBeanWrapper(value);

		if (wrapper == null) {
			return headers;
		}

		Calendar lastModifiedDate = wrapper.getLastModifiedDate();

		if (lastModifiedDate != null) {
			headers.setLastModified(lastModifiedDate.getTimeInMillis());
		}

		return headers;
	}

	/**
	 * Returns whether the given object is still valid in the context of the given {@link HttpHeaders}' requirements.
	 * 
	 * @param source must not be {@literal null}.
	 * @param headers must not be {@literal null}.
	 * @return
	 */
	public boolean isObjectStillValid(Object source, HttpHeaders headers) {

		Assert.notNull(source, "Source object must not be null!");
		Assert.notNull(headers, "HttpHeaders must not be null!");

		if (headers.getIfModifiedSince() == -1) {
			return false;
		}

		AuditableBeanWrapper wrapper = auditableBeanWrapperFactory.getBeanWrapperFor(source);
		long current = wrapper.getLastModifiedDate().getTimeInMillis() / 1000 * 1000;

		return current <= headers.getIfModifiedSince();
	}

	/**
	 * Returns the {@link AuditableBeanWrapper} for the given source.
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	private AuditableBeanWrapper getAuditableBeanWrapper(Object source) {
		return auditableBeanWrapperFactory.getBeanWrapperFor(source);
	}
}
