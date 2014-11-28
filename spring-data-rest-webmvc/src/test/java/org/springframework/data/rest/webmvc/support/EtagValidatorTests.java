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

package org.springframework.data.rest.webmvc.support;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.persistence.Version;
import javax.persistence.metamodel.Metamodel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.jpa.mapping.JpaPersistentEntity;
import org.springframework.data.rest.core.mapping.MappingResourceMetadata;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpHeaders;

/**
 * Tests for the EtagValidator used for optimistic locking
 * 
 * @author Pablo Lozano
 */

@RunWith(MockitoJUnitRunner.class)
public class EtagValidatorTests {

	EtagValidator etagValidator;

	@Mock Metamodel model;

	JpaMetamodelMappingContext context;
	JpaPersistentEntity<?> entity;

	@Before
	public void setUp() {
		context = new JpaMetamodelMappingContext(model);
		entity = context.getPersistentEntity(Sample.class);
		etagValidator = new EtagValidator(new DefaultFormattingConversionService());
	}

	/**
	 * @see DATAREST-160
	 */
	@Test(expected = OptimisticLockingFailureException.class)
	public void expectWrongEtag() throws Exception {
		Sample sampleEntity = new Sample();
		sampleEntity.version = 0;

		ResourceMetadata resourceMetadata = new MappingResourceMetadata(entity);
		RootResourceInformation rootResourceInformation = new RootResourceInformation(resourceMetadata, entity, null);

		etagValidator.validateEtag("\"1\"", rootResourceInformation, sampleEntity);
	}

	/**
	 * @see DATAREST-160
	 */
	@Test
	public void expectCorrectEtag() throws Exception {
		Sample sampleEntity = new Sample();
		sampleEntity.version = 0;

		ResourceMetadata resourceMetadata = new MappingResourceMetadata(entity);
		RootResourceInformation rootResourceInformation = new RootResourceInformation(resourceMetadata, entity, null);

		etagValidator.validateEtag("\"0\"", rootResourceInformation, sampleEntity);
	}

	/**
	 * @see DATAREST-160
	 */
	@Test
	public void setCorrectEtagHeader() throws Exception {
		Sample sampleEntity = new Sample();
		sampleEntity.version = 0;

		HttpHeaders headers = new HttpHeaders();
		PersistentEntityResource perf = PersistentEntityResource.build(sampleEntity, entity).build();
		etagValidator.addEtagHeader(headers, perf);
		Object ifMatch = headers.getETag();

		assertThat(ifMatch, is((Object) "\"0\""));
	}

	public class Sample {

		private @Version long version;

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}

	}
}
