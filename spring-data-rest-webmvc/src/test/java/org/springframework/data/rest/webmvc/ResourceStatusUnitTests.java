/*
 * Copyright 2016-2018 original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import lombok.Value;

import java.util.Date;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.annotation.Version;
import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentEntity;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.rest.webmvc.ResourceStatus.StatusAndHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link ResourceStatus}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceStatusUnitTests {

	ResourceStatus status;
	KeyValuePersistentEntity<?, ?> entity;

	@Mock HttpHeadersPreparer preparer;
	@Mock Supplier<PersistentEntityResource> supplier;

	@BeforeEach
	void setUp() {

		this.status = ResourceStatus.of(preparer);

		KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();
		this.entity = context.getRequiredPersistentEntity(Sample.class);

		doReturn(new HttpHeaders()).when(preparer).prepareHeaders(eq(entity), any());
	}

	@Test // DATAREST-835
	void rejectsNullPreparer() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> ResourceStatus.of(null));
	}

	@Test // DATAREST-835
	void returnsModifiedIfNoHeadersGiven() {
		assertModified(status.getStatusAndHeaders(new HttpHeaders(), new Sample(0), entity));
	}

	@Test // DATAREST-835
	void returnsNotModifiedForEntityWithRequestedETag() {

		HttpHeaders headers = new HttpHeaders();
		headers.setIfNoneMatch("\"1\"");

		assertNotModified(status.getStatusAndHeaders(headers, new Sample(1), entity));
	}

	@Test // DATAREST-835
	void returnsNotModifiedIfEntityIsStillConsideredValid() {

		doReturn(true).when(preparer).isObjectStillValid(any(), any(HttpHeaders.class));

		assertNotModified(status.getStatusAndHeaders(new HttpHeaders(), new Sample(0), entity));
	}

	@Test // DATAREST-1121
	void rejectsInvalidPersistentEntityDomainObjectCombination() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> assertModified(status.getStatusAndHeaders(new HttpHeaders(), new Date(), entity)))
				.withMessageContaining(entity.getType().getName());
	}

	private void assertModified(StatusAndHeaders statusAndHeaders) {

		assertThat(statusAndHeaders.isModified()).isTrue();
		assertThat(statusAndHeaders.toResponseEntity(supplier).getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(supplier).get();
	}

	private void assertNotModified(StatusAndHeaders statusAndHeaders) {

		assertThat(statusAndHeaders.isModified()).isFalse();
		assertThat(statusAndHeaders.toResponseEntity(supplier).getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
	}

	@Value
	static class Sample {
		@Version int version;
	}
}
