/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.rest.webmvc.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
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
@RunWith(MockitoJUnitRunner.class)
public class ETagUnitTests {

	KeyValueMappingContext context = new KeyValueMappingContext();

	@Test(expected = ETagDoesntMatchException.class) // DATAREST-160
	public void expectWrongEtag() throws Exception {

		ETag eTag = ETag.from("1");
		eTag.verify(context.getPersistentEntity(Sample.class), new Sample(0L));
	}

	@Test // DATAREST-160
	public void expectCorrectEtag() throws Exception {
		ETag.from("0").verify(context.getPersistentEntity(Sample.class), new Sample(0L));
	}

	@Test // DATAREST-160
	public void createsETagFromVersionValue() throws Exception {

		PersistentEntity<?, ?> entity = context.getPersistentEntity(Sample.class);
		ETag from = ETag.from(PersistentEntityResource.build(new Sample(0L), entity).build());

		assertThat(from.toString(), is((Object) "\"0\""));
	}

	@Test // DATAREST-160
	public void surroundsValueWithQuotationMarksOnToString() {
		assertThat(ETag.from("1").toString(), is("\"1\""));
	}

	@Test // DATAREST-160
	public void returnsNoEtagForNullStringSource() {
		assertThat(ETag.from((String) null), is(ETag.NO_ETAG));
	}

	@Test // DATAREST-160
	public void returnsNoEtagForNullPersistentEntityResourceSource() {
		assertThat(ETag.from((PersistentEntityResource) null), is(ETag.NO_ETAG));
	}

	@Test // DATAREST-160
	public void hasValueObjectEqualsSemantics() {

		ETag one = ETag.from("1");
		ETag two = ETag.from("2");
		ETag nullETag = ETag.from((String) null);

		assertThat(one.equals(one), is(true));
		assertThat(one.equals(two), is(false));
		assertThat(two.equals(one), is(false));
		assertThat(nullETag.equals(one), is(false));
		assertThat(one.equals(two), is(false));
		assertThat(one.equals(""), is(false));
	}

	@Test // DATAREST-160
	public void returnsNoEtagForEntityWithoutVersionProperty() {

		PersistentEntity<?, ?> entity = context.getPersistentEntity(SampleWithoutVersion.class);
		assertThat(ETag.from(PersistentEntityResource.build(new SampleWithoutVersion(), entity).build()), is(ETag.NO_ETAG));
	}

	@Test // DATAREST-160
	public void noETagReturnsNullForToString() {
		assertThat(ETag.NO_ETAG.toString(), is(nullValue()));
	}

	@Test // DATAREST-160
	public void noETagDoesNotRejectVerification() {
		ETag.NO_ETAG.verify(context.getPersistentEntity(Sample.class), new Sample(5L));
	}

	@Test // DATAREST-160
	public void verificationDoesNotRejectNullEntity() {
		ETag.from("5").verify(context.getPersistentEntity(Sample.class), null);
	}

	@Test // DATAREST-160
	public void stripsTrailingAndLeadingQuotesOnCreation() {

		assertThat(ETag.from("\"1\""), is(ETag.from("1")));
		assertThat(ETag.from("\"\"1\"\""), is(ETag.from("1")));
	}

	@Test // DATAREST-160
	public void addsETagToHeadersIfNotNoETag() {

		HttpHeaders headers = ETag.from("1").addTo(new HttpHeaders());
		assertThat(headers.getETag(), is(notNullValue()));
	}

	@Test // DATAREST-160
	public void doesNotAddHeaderForNoETag() {

		HttpHeaders headers = ETag.NO_ETAG.addTo(new HttpHeaders());

		assertThat(headers.containsKey("ETag"), is(false));
	}

	// tag::versioned-sample[]
	public class Sample {

		@Version Long version; // <1>

		Sample(Long version) {
			this.version = version;
		}
	}
	// end::versioned-sample[]

	public class SampleWithoutVersion {}
}
