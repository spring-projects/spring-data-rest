/*
 * Copyright 2016-2017 original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import lombok.Value;

import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.Version;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.rest.core.util.Supplier;
import org.springframework.data.rest.webmvc.ResourceStatus.StatusAndHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link ResourceStatus}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceStatusUnitTests {

	ResourceStatus status;
	KeyValuePersistentEntity<?> entity;

	@Mock HttpHeadersPreparer preparer;
	@Mock Supplier<PersistentEntityResource> supplier;

	public @Rule ExpectedException exception = ExpectedException.none();

	@Before
	public void setUp() {

		this.status = ResourceStatus.of(preparer);

		KeyValueMappingContext context = new KeyValueMappingContext();
		this.entity = context.getPersistentEntity(Sample.class);

		doReturn(new HttpHeaders()).when(preparer).prepareHeaders(eq(entity), Matchers.any());
	}

	@Test(expected = IllegalArgumentException.class) // DATAREST-835
	public void rejectsNullPreparer() {
		ResourceStatus.of(null);
	}

	@Test // DATAREST-835
	public void returnsModifiedIfNoHeadersGiven() {
		assertModified(status.getStatusAndHeaders(new HttpHeaders(), new Sample(0), entity));
	}

	@Test // DATAREST-835
	public void returnsNotModifiedForEntityWithRequestedETag() {

		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch("\"1\"");

		assertNotModified(status.getStatusAndHeaders(headers, new Sample(1), entity));
	}

	@Test // DATAREST-835
	public void returnsNotModifiedIfEntityIsStillConsideredValid() {

		doReturn(true).when(preparer).isObjectStillValid(Matchers.any(), Matchers.any(HttpHeaders.class));

		assertNotModified(status.getStatusAndHeaders(new HttpHeaders(), new Sample(0), entity));
	}

	@Test // DATAREST-1121
	public void rejectsInvalidPersistentEntityDomainObjectCombination() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(entity.getType().getName());

		assertModified(status.getStatusAndHeaders(new HttpHeaders(), new Date(), entity));
	}

	private void assertModified(StatusAndHeaders statusAndHeaders) {

		assertThat(statusAndHeaders.isModified(), is(true));
		assertThat(statusAndHeaders.toResponseEntity(supplier).getStatusCode(), is(HttpStatus.OK));
		verify(supplier).get();
	}

	private void assertNotModified(StatusAndHeaders statusAndHeaders) {

		assertThat(statusAndHeaders.isModified(), is(false));
		assertThat(statusAndHeaders.toResponseEntity(supplier).getStatusCode(), is(HttpStatus.NOT_MODIFIED));
	}

	@Value
	static class Sample {
		@Version int version;
	}
}
