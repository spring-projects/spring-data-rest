/*
 * Copyright 2020-2021 the original author or authors.
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

import java.util.List;

import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;

/**
 * A {@link HandlerMapping} that exposes its delegates.
 *
 * @author Oliver Drotbohm
 * @deprecated since 3.4. Prefer handling {@link HandlerMapping}s that implement {@link Iterable} of
 *             {@link HandlerMapping} and consume the delegates that way.
 */
@Deprecated
public interface DelegatingHandlerMapping extends MatchableHandlerMapping, Iterable<HandlerMapping> {

	/**
	 * Returns all delegate {@link HandlerMapping}s. Prefer simply iterating over this instance itself.
	 *
	 * @return will never be {@literal null}.
	 */
	List<HandlerMapping> getDelegates();
}
