/*
 * Copyright 2015-2018 original author or authors.
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

import org.junit.Test;

/**
 * Unit tests for {@link RepositorySearchesResource}
 *
 * @author Oliver Gierke
 * @soundtrack Bakkushan - Gefahr (Bakkushan)
 */
public class RepositorySearchesResourceUnitTests {

	@Test(expected = IllegalArgumentException.class) // DATAREST-515
	public void rejectsNullDomainType() {
		new RepositorySearchesResource(null);
	}

	@Test // DATAREST-515
	public void returnsConfiguredDomainType() {
		assertThat(new RepositorySearchesResource(String.class).getDomainType()).isAssignableFrom(String.class);
	}
}
