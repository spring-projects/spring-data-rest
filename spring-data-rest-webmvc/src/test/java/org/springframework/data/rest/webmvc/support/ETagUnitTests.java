/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Version;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.http.HttpHeaders;

/**
 * Tests for the ETagValidator used for optimistic locking
 *
 * @author Pablo Lozano
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class ETagUnitTests {

	KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();

	@Test // DATAREST-160
	void expectWrongEtag() throws Exception {

		ETag eTag = ETag.from("1");

		assertThatExceptionOfType(ETagDoesntMatchException.class) //
				.isThrownBy(() -> eTag.verify(context.getRequiredPersistentEntity(Sample.class), new Sample(0L)));
	}

	@Test // DATAREST-160
	void expectCorrectEtag() throws Exception {
		ETag.from("0").verify(context.getRequiredPersistentEntity(Sample.class), new Sample(0L));
	}

	@Test // DATAREST-160
	void createsETagFromVersionValue() throws Exception {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(Sample.class);
		ETag from = ETag.from(PersistentEntityResource.build(new Sample(0L), entity).build());

		assertThat(from.toString()).isEqualTo((Object) "\"0\"");
	}

	@Test // DATAREST-160
	void surroundsValueWithQuotationMarksOnToString() {
		assertThat(ETag.from("1").toString()).isEqualTo("\"1\"");
	}

	@Test // DATAREST-160
	void returnsNoEtagForNullStringSource() {
		assertThat(ETag.from((String) null)).isEqualTo(ETag.NO_ETAG);
	}

	@Test // DATAREST-160
	void returnsNoEtagForNullPersistentEntityResourceSource() {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			ETag.from((PersistentEntityResource) null);
		});
	}

	@Test // DATAREST-160
	void hasValueObjectEqualsSemantics() {

		ETag one = ETag.from("1");
		ETag two = ETag.from("2");
		ETag nullETag = ETag.from((String) null);

		assertThat(one.equals(one)).isTrue();
		assertThat(one.equals(two)).isFalse();
		assertThat(two.equals(one)).isFalse();
		assertThat(nullETag.equals(one)).isFalse();
		assertThat(one.equals(two)).isFalse();
		assertThat(one.equals("")).isFalse();
	}

	@Test // DATAREST-160
	void returnsNoEtagForEntityWithoutVersionProperty() {

		PersistentEntity<?, ?> entity = context.getRequiredPersistentEntity(SampleWithoutVersion.class);
		assertThat(ETag.from(PersistentEntityResource.build(new SampleWithoutVersion(), entity).build()))
				.isEqualTo(ETag.NO_ETAG);
	}

	@Test // DATAREST-160
	void noETagReturnsNullForToString() {
		assertThat(ETag.NO_ETAG.toString()).isNull();
	}

	@Test // DATAREST-160
	void noETagDoesNotRejectVerification() {
		ETag.NO_ETAG.verify(context.getRequiredPersistentEntity(Sample.class), new Sample(5L));
	}

	@Test // DATAREST-160
	void verificationDoesNotRejectNullEntity() {
		ETag.from("5").verify(context.getRequiredPersistentEntity(Sample.class), null);
	}

	@Test // DATAREST-160
	void stripsTrailingAndLeadingQuotesOnCreation() {

		assertThat(ETag.from("\"1\"")).isEqualTo(ETag.from("1"));
		assertThat(ETag.from("\"\"1\"\"")).isEqualTo(ETag.from("1"));
	}

	@Test // DATAREST-160
	void addsETagToHeadersIfNotNoETag() {

		HttpHeaders headers = ETag.from("1").addTo(new HttpHeaders());
		assertThat(headers.getETag()).isNotNull();
	}

	@Test // DATAREST-160
	void doesNotAddHeaderForNoETag() {

		HttpHeaders headers = ETag.NO_ETAG.addTo(new HttpHeaders());

		assertThat(headers.containsKey("ETag")).isFalse();
	}

	// tag::versioned-sample[]
	class Sample {

		@Version Long version; // <1>

		Sample(Long version) {
			this.version = version;
		}
	}
	// end::versioned-sample[]

	class SampleWithoutVersion {}
}
