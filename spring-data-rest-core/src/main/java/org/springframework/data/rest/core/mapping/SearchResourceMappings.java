/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.rest.core.mapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.data.rest.core.Path;
import org.springframework.util.Assert;

/**
 * {@link ResourceMapping} for all search resources.
 * 
 * @author Oliver Gierke
 */
public class SearchResourceMappings implements Iterable<MethodResourceMapping>, ResourceMapping {

	private static final String AMBIGUOUS_MAPPING = "Ambiguous search mapping detected. Both %s and "
			+ "%s are mapped to %s! Tweak configuration to get to unambiguous paths!";

	private static final Path PATH = new Path("/search");
	private static final String REL = "search";

	private final Map<Path, MethodResourceMapping> mappings;

	/**
	 * Creates a new {@link SearchResourceMappings} from the given
	 * 
	 * @param mappings
	 */
	public SearchResourceMappings(List<MethodResourceMapping> mappings) {

		Assert.notNull(mappings, "MethodResourceMappings must not be null!");

		this.mappings = new HashMap<Path, MethodResourceMapping>(mappings.size());

		for (MethodResourceMapping mapping : mappings) {

			MethodResourceMapping existing = this.mappings.get(mapping.getPath());

			if (existing != null) {
				throw new IllegalStateException(String.format(AMBIGUOUS_MAPPING, existing.getMethod(), mapping.getMethod(),
						existing.getPath()));
			}

			this.mappings.put(mapping.getPath(), mapping);
		}
	}

	/**
	 * Returns the method mapped to the given path.
	 * 
	 * @param path must not be {@literal null} or empty.
	 * @return
	 */
	public Method getMappedMethod(String path) {

		Assert.hasText(path, "Path must not be null or empty!");

		MethodResourceMapping mapping = mappings.get(new Path(path));
		return mapping == null ? null : mapping.getMethod();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getPath()
	 */
	@Override
	public Path getPath() {
		return PATH;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getRel()
	 */
	@Override
	public String getRel() {
		return REL;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#isExported()
	 */
	@Override
	public boolean isExported() {
		return !mappings.isEmpty();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#isPagingResource()
	 */
	@Override
	public boolean isPagingResource() {
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.core.mapping.ResourceMapping#getDescription()
	 */
	@Override
	public ResourceDescription getDescription() {
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<MethodResourceMapping> iterator() {
		return mappings.values().iterator();
	}
}
