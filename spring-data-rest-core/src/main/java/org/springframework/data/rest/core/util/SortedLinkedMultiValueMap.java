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
 * @author Joseph Valerio
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
