/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;

/**
 * Extension of {@link LinkedMultiValueMap} that sorts the values List on update.
 *
 * <p>This Map implementation is generally not thread-safe. It is primarily designed
 * for data structures exposed from request objects, for use in a single thread only.
 *
 * @author Joe Valerio
 */
public class SortedLinkedMultiValueMap<K,V extends Comparable<V>> extends LinkedMultiValueMap<K,V> {

	private static final long serialVersionUID = 2075011697927127513L;

	public SortedLinkedMultiValueMap() {
		super();
	}

	public SortedLinkedMultiValueMap(int initialCapacity) {
		super(initialCapacity);
	}

	public SortedLinkedMultiValueMap(Map<K, List<V>> otherMap) {
		super(otherMap);
	}

	@Override
	public void add(K key, V value) {
		super.add(key, value);
		List<V> values = get(key);
		Collections.sort(values);
	}

	@Override
	public void set(K key, V value) {
		super.set(key, value);
		List<V> values = get(key);
		Collections.sort(values);
	}

}
