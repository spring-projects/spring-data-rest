/*
 * Copyright 2018-present the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * A user profile entity used to test URI-to-entity deserialization when the ANNOTATED repository detection strategy is
 * active. The corresponding {@link ProfileRepository} is intentionally <em>not</em> annotated with
 * {@code @RepositoryRestResource}, so it is not exported as an HTTP endpoint under the ANNOTATED strategy. This
 * reproduces the scenario described in
 * <a href="https://github.com/spring-projects/spring-data-rest/issues/1515">GH-1515</a>.
 *
 * @author Spring Data REST team
 * @author Steve Rutherford
 * @see ProfileRepository
 * @see Member
 */
@Entity
public class Profile {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	protected Profile() {}

	public Profile(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
