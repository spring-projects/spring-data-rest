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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * Simple abstraction to capture the status of a resource to determine whether it has been modified or not and produce
 * {@link ResponseEntity} via its {@link StatusAndHeaders} sub component.
 * 
 * @author Oliver Gierke
 * @since 2.6
 * @soundtrack Blumentopf - Dass ich nicht lache (Kein Zufall)
 */
@RequiredArgsConstructor(staticName = "of")
class ResourceStatus {

	private static final String INVALID_DOMAIN_OBJECT = "Domain object %s is not an instance of the given PersistentEntity of type %s!";

	private final @NonNull HttpHeadersPreparer preparer;

	/**
	 * Returns the {@link StatusAndHeaders} calculated from the given {@link HttpHeaders}, domain object and
	 * {@link RootResourceInformation.
	 * 
	 * @param requestHeaders must not be {@literal null}.
	 * @param domainObject must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	public StatusAndHeaders getStatusAndHeaders(HttpHeaders requestHeaders, Object domainObject,
			PersistentEntity<?, ?> entity) {

		Assert.notNull(requestHeaders, "Request headers must not be null!");
		Assert.notNull(domainObject, "Domain object must not be null!");
		Assert.notNull(entity, "PersistentEntity must not be null!");
		Assert.isTrue(entity.getType().isInstance(domainObject),
				() -> String.format(INVALID_DOMAIN_OBJECT, domainObject, entity.getType()));

		// Check ETag for If-Non-Match

		List<String> ifNoneMatch = requestHeaders.getIfNoneMatch();
		ETag eTag = ifNoneMatch.isEmpty() ? ETag.NO_ETAG : ETag.from(ifNoneMatch.get(0));
		HttpHeaders responseHeaders = preparer.prepareHeaders(entity, domainObject);

		// Check last modification for If-Modified-Since

		return eTag.matches(entity, domainObject) || preparer.isObjectStillValid(domainObject, requestHeaders)
				? StatusAndHeaders.notModified(responseHeaders)
				: StatusAndHeaders.modified(responseHeaders);
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class StatusAndHeaders {

		private final @NonNull HttpHeaders headers;
		private final @Getter(AccessLevel.PACKAGE) boolean modified;

		private static StatusAndHeaders notModified(HttpHeaders headers) {
			return new StatusAndHeaders(headers, false);
		}

		private static StatusAndHeaders modified(HttpHeaders headers) {
			return new StatusAndHeaders(headers, true);
		}

		/**
		 * Creates a {@link ResponseEntity} based on the given {@link PersistentEntityResource}.
		 * 
		 * @param supplier a {@link Supplier} to provide a {@link PersistentEntityResource} eventually, must not be
		 *          {@literal null}.
		 * @return
		 */
		public ResponseEntity<Resource<?>> toResponseEntity(Supplier<PersistentEntityResource> supplier) {
			return modified ? new ResponseEntity<Resource<?>>(supplier.get(), headers, HttpStatus.OK)
					: new ResponseEntity<Resource<?>>(headers, HttpStatus.NOT_MODIFIED);
		}
	}
}
