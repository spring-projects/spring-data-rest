/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.http.MediaType;

/**
 * Unit tests for {@link RepositoryRestConfiguration}.
 * 
 * @author Oliver Gierke
 * @soundtrack Adam F - Circles (Colors)
 */
public class RepositoryRestConfigurationUnitTests {

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void returnsBodiesIfAcceptHeaderPresentByDefault() {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();

		assertThat(configuration.returnBodyOnCreate(MediaType.APPLICATION_JSON_VALUE), is(true));
		assertThat(configuration.returnBodyOnUpdate(MediaType.APPLICATION_JSON_VALUE), is(true));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void doesNotReturnBodiesIfNoAcceptHeaderPresentByDefault() {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();

		assertThat(configuration.returnBodyOnCreate(null), is(false));
		assertThat(configuration.returnBodyOnUpdate(null), is(false));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void doesNotReturnBodiesIfEmptyAcceptHeaderPresentByDefault() {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();

		assertThat(configuration.returnBodyOnCreate(""), is(false));
		assertThat(configuration.returnBodyOnUpdate(""), is(false));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void doesNotReturnBodyForUpdateIfExplicitlyDeactivated() {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();
		configuration.setReturnBodyOnUpdate(false);

		assertThat(configuration.returnBodyOnUpdate(null), is(false));
		assertThat(configuration.returnBodyOnUpdate(""), is(false));
		assertThat(configuration.returnBodyOnUpdate(MediaType.APPLICATION_JSON_VALUE), is(false));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void doesNotReturnBodyForCreateIfExplicitlyDeactivated() {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();
		configuration.setReturnBodyOnCreate(false);

		assertThat(configuration.returnBodyOnCreate(null), is(false));
		assertThat(configuration.returnBodyOnCreate(""), is(false));
		assertThat(configuration.returnBodyOnCreate(MediaType.APPLICATION_JSON_VALUE), is(false));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void returnsBodyForUpdateIfExplicitlyActivated() {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();
		configuration.setReturnBodyOnUpdate(true);

		assertThat(configuration.returnBodyOnUpdate(null), is(true));
		assertThat(configuration.returnBodyOnUpdate(""), is(true));
		assertThat(configuration.returnBodyOnUpdate(MediaType.APPLICATION_JSON_VALUE), is(true));
	}

	/**
	 * @see DATAREST-34
	 */
	@Test
	public void returnsBodyForCreateIfExplicitlyActivated() {

		RepositoryRestConfiguration configuration = new RepositoryRestConfiguration();
		configuration.setReturnBodyOnCreate(true);

		assertThat(configuration.returnBodyOnCreate(null), is(true));
		assertThat(configuration.returnBodyOnCreate(""), is(true));
		assertThat(configuration.returnBodyOnCreate(MediaType.APPLICATION_JSON_VALUE), is(true));
	}
}
