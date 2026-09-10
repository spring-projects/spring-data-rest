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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * A member entity that holds a {@link ManyToOne} association to a {@link Profile}. Used to reproduce the bug
 * described in <a href="https://github.com/spring-projects/spring-data-rest/issues/1515">GH-1515</a>: when the
 * {@code ANNOTATED} repository detection strategy is active and {@link ProfileRepository} is not annotated with
 * {@code @RepositoryRestResource}, submitting a URI string for the {@code profile} field in a JSON payload must
 * still be deserialized correctly via the {@code UriStringDeserializer}.
 *
 * @author Spring Data REST team
 * @author Steve Rutherford
 * @see Profile
 * @see MemberRepository
 */
@Entity
public class Member {

	@Id
	@GeneratedValue
	private Long id;

	private String username;

	@ManyToOne(fetch = FetchType.LAZY)
	private Profile profile;

	protected Member() {}

	public Member(String username) {
		this.username = username;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}
}
