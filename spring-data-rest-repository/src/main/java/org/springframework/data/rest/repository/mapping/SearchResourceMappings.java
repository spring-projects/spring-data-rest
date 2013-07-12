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
package org.springframework.data.rest.repository.mapping;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.rest.core.Path;
import org.springframework.util.Assert;

/**
 * {@link ResourceMapping} for all search resources.
 * 
 * @author Oliver Gierke
 */
public class SearchResourceMappings implements Iterable<MethodResourceMapping>, ResourceMapping {

	private static final Path PATH = new Path("/search");
	private static final String REL = "search";

	private final List<MethodResourceMapping> mappings;

	/**
	 * Creates a new {@link SearchResourceMappings} from the given
	 * 
	 * @param mappings
	 */
	public SearchResourceMappings(List<MethodResourceMapping> mappings) {

		Assert.notNull(mappings, "MethodResourceMappings must not be null!");
		this.mappings = mappings;
	}

	/**
	 * Returns the method mapped to the given path.
	 * 
	 * @param path must not be {@literal null} or empty.
	 * @return
	 */
	public Method getMappedMethod(String path) {

		Assert.hasText(path, "Path must not be null or empty!");

		for (MethodResourceMapping mapping : mappings) {
			if (mapping.getPath().matches(path)) {
				return mapping.getMethod();
			}
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {
		return PATH;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#getRel()
	 */
	@Override
	public String getRel() {
		return REL;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.ResourceMapping#isExported()
	 */
	@Override
	public Boolean isExported() {
		return !mappings.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<MethodResourceMapping> iterator() {
		return mappings.iterator();
	}
}
