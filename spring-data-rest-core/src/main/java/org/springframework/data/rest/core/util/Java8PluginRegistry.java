/*
 * Copyright 2017-2018 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.Plugin;
import org.springframework.plugin.core.PluginRegistry;

/**
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class Java8PluginRegistry<T extends Plugin<S>, S> {

	private final PluginRegistry<T, S> registry;

	public static <T extends Plugin<S>, S> Java8PluginRegistry<T, S> of(List<? extends T> plugins) {
		return Java8PluginRegistry.of(OrderAwarePluginRegistry.create(plugins));
	}

	public static <T extends Plugin<S>, S> Java8PluginRegistry<T, S> of(PluginRegistry<T, S> plugins) {
		return new Java8PluginRegistry<>(plugins);
	}

	public static <T extends Plugin<S>, S> Java8PluginRegistry<T, S> empty() {
		return Java8PluginRegistry.of(Collections.emptyList());
	}

	public Optional<T> getPluginFor(S delimiter) {
		return registry.getPluginFor(delimiter);
	}

	public T getPluginOrDefaultFor(S delimiter, T fallback) {
		return getPluginFor(delimiter).orElse(fallback);
	}
}
