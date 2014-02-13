/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.rest.core.invoke;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * @author Oliver Gierke
 */
public interface RepositoryInvoker extends RepositoryInvocationInformation {

	<T> T invokeSave(T object);

	<T> T invokeFindOne(Serializable id);

	Iterable<Object> invokeFindAll(Pageable pageable);

	Iterable<Object> invokeFindAll(Sort sort);

	void invokeDelete(Serializable serializable);

	Object invokeQueryMethod(Method method, Map<String, String[]> parameters, Pageable pageable, Sort sort);
}
