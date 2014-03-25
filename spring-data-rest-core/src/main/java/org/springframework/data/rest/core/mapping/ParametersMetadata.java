/*
 * Copyright 2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Value object for a list of {@link ParameterMetadata} instances.
 * 
 * @author Oliver Gierke
 */
public class ParametersMetadata implements Iterable<ParameterMetadata> {

	private final List<ParameterMetadata> parameterMetadata;

	/**
	 * Creates a new {@link ParametersMetadata} instance for the given {@link ParameterMetadata} instances.
	 * 
	 * @param parameterMetadata must not be {@literal null}.
	 */
	ParametersMetadata(List<ParameterMetadata> parameterMetadata) {

		Assert.notNull(parameterMetadata, "Parameter metadata must not be null!");

		this.parameterMetadata = parameterMetadata;
	}

	/**
	 * Returns all parameter names.
	 * 
	 * @return
	 */
	public List<String> getParameterNames() {

		List<String> names = new ArrayList<String>(parameterMetadata.size());

		for (ParameterMetadata metadata : parameterMetadata) {
			names.add(metadata.getName());
		}

		return names;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ParameterMetadata> iterator() {
		return parameterMetadata.iterator();
	}
}
