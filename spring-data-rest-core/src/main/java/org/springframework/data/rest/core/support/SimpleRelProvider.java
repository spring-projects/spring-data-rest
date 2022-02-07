/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.data.rest.core.support;

import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.util.StringUtils;

/**
 * @author Oliver Gierke
 */
public class SimpleRelProvider implements LinkRelationProvider {

	@Override
	public boolean supports(LookupContext context) {
		return true;
	}

	@Override
	public LinkRelation getItemResourceRelFor(Class<?> type) {

		LinkRelation collectionRel = getCollectionResourceRelFor(type);
		return LinkRelation.of(String.format("%s.%s", collectionRel.value(), collectionRel.value()));
	}

	@Override
	public LinkRelation getCollectionResourceRelFor(Class<?> type) {
		return LinkRelation.of(StringUtils.uncapitalize(type.getSimpleName()));
	}
}
