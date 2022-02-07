/*
 * Copyright 2016-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.auditing.AuditableBeanWrapper;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Value object to prepare {@link HttpHeaders} for {@link PersistentEntityResource} and {@link PersistentEntity}
 * instances.
 *
 * @author Oliver Gierke
 * @author Dario Seidl
 * @soundtrack Ron Spielman Trio - Matchstick
 */
public class HttpHeadersPreparer {

	private final AuditableBeanWrapperFactory auditableBeanWrapperFactory;
	private final ConfigurableConversionService conversionService = new DefaultConversionService();

	public HttpHeadersPreparer(AuditableBeanWrapperFactory auditableBeanWrapperFactory) {

		Assert.notNull(auditableBeanWrapperFactory, "AuditableBeanWrapperFactory must not be null!");

		Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);

		this.auditableBeanWrapperFactory = auditableBeanWrapperFactory;
	}

	/**
	 * Returns the default headers to be returned for the given {@link PersistentEntityResource}. Will set {@link ETag}
	 * and {@code Last-Modified} headers if applicable.
	 *
	 * @param resource can be {@literal null}.
	 * @return
	 */
	public HttpHeaders prepareHeaders(Optional<PersistentEntityResource> resource) {

		return resource//
				.map(it -> prepareHeaders(it.getPersistentEntity(), it.getTarget()))//
				.orElseGet(() -> new HttpHeaders());
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
		Assert.isInstanceOf(entity.getType(), value, () ->
			String.format("Target bean of type %s is not of type of the persistent entity (%s)!", value.getClass().getName(), entity.getType().getName()));

		// Add ETag
		HttpHeaders headers = ETag.from(entity, value).addTo(new HttpHeaders());

		// Add Last-Modified
		getLastModifiedInMilliseconds(value).ifPresent(it -> headers.setLastModified(it));

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

		return getLastModifiedInMilliseconds(source)//
				.map(it -> it / 1000 * 1000 <= headers.getIfModifiedSince())//
				.orElse(true);
	}

	/**
	 * Returns the {@link AuditableBeanWrapper} for the given source.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	private Optional<AuditableBeanWrapper<Object>> getAuditableBeanWrapper(Object source) {
		return auditableBeanWrapperFactory.getBeanWrapperFor(source);
	}

	private Optional<Long> getLastModifiedInMilliseconds(Object object) {

		return getAuditableBeanWrapper(object)//
				.flatMap(it -> it.getLastModifiedDate())//
				.map(it -> conversionService.convert(it, Date.class))//
				.map(it -> conversionService.convert(it, Instant.class))//
				.map(it -> it.toEpochMilli());
	}
}
