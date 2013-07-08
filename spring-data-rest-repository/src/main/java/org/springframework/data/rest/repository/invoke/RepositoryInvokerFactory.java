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
package org.springframework.data.rest.repository.invoke;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.support.Repositories;

/**
 * @author Oliver Gierke
 */
public class RepositoryInvokerFactory {

	private final Repositories repositories;
	private final Map<Class<?>, RepositoryInvoker> invokers;

	/**
	 * @param repositories
	 * @param invokers
	 */
	public RepositoryInvokerFactory(Repositories repositories) {

		this.repositories = repositories;
		this.invokers = new HashMap<Class<?>, RepositoryInvoker>();
		prepareInvokers(repositories);
	}

	@SuppressWarnings("unchecked")
	private final void prepareInvokers(Repositories repositories) {

		for (Class<?> domainType : repositories) {

			Object repository = repositories.getRepositoryFor(domainType);
			RepositoryInvoker invoker = null;

			if (repository instanceof PagingAndSortingRepository) {
				invoker = new PagingAndSortingRepositoryInvoker((PagingAndSortingRepository<Object, Serializable>) repository);
			} else if (repository instanceof CrudRepository) {
				invoker = new CrudRepositoryInvoker((CrudRepository<Object, Serializable>) repository);
			} else {
				invoker = new RepositoryMethodInvoker(repository, null, null);
			}

			invokers.put(domainType, invoker);
		}
	}

	public RepositoryInvoker getInvokerFor(Class<?> domainType) {
		return invokers.get(domainType);
	}
}
