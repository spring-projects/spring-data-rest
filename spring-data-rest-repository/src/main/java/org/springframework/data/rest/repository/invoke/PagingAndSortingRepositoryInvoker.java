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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Oliver Gierke
 */
class PagingAndSortingRepositoryInvoker extends CrudRepositoryInvoker {

	private final PagingAndSortingRepository<Object, Serializable> repository;

	/**
	 * @param repository must not be {@literal null}.
	 */
	public PagingAndSortingRepositoryInvoker(PagingAndSortingRepository<Object, Serializable> repository) {
		super(repository);
		this.repository = repository;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.CrudRepositoryInvoker#findAll(org.springframework.data.domain.Pageable)
	 */
	@Override
	public Page<Object> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.invoke.CrudRepositoryInvoker#findAll(org.springframework.data.domain.Sort)
	 */
	@Override
	public Iterable<Object> findAll(Sort sort) {
		return repository.findAll(sort);
	}
}
