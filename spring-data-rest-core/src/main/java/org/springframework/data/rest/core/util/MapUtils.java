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
package org.springframework.data.rest.core.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Helper methods to work with {@link Map}s.
 * 
 * @author Oliver Gierke
 */
public abstract class MapUtils {

	/**
	 * Turns a {@link MultiValueMap} into its {@link Map} equivalent.
	 * 
	 * @param map must not be {@literal null}.
	 * @return
	 */
	public static <K, V> Map<K, Collection<V>> toMap(MultiValueMap<K, V> map) {

		Assert.notNull(map, "Given map must not be null!");
		Map<K, Collection<V>> result = new LinkedHashMap<K, Collection<V>>(map.size());

		for (Entry<K, List<V>> entry : map.entrySet()) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}
}
