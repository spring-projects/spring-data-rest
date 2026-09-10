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

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Repository for {@link Member} entities. Annotated with {@code @RepositoryRestResource} so that it is exported as
 * an HTTP endpoint even when the {@code ANNOTATED} repository detection strategy is active. This contrasts with
 * {@link ProfileRepository}, which is intentionally <em>not</em> annotated and therefore not HTTP-exported.
 *
 * @author Spring Data REST team
 * @author Steve Rutherford
 * @see Member
 * @see ProfileRepository
 */
@RepositoryRestResource
public interface MemberRepository extends CrudRepository<Member, Long> {
}
