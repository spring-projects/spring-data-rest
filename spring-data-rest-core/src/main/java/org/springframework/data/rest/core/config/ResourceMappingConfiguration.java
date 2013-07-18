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
package org.springframework.data.rest.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the {@link ResourceMapping} configurations for any resources being exported. This includes domain entities
 * and repositories.
 * 
 * @author Jon Brisbin
 */
@SuppressWarnings("deprecation")
public class ResourceMappingConfiguration {

	private final Map<Class<?>, ResourceMapping> resourceMappings = new HashMap<Class<?>, ResourceMapping>();

	public ResourceMapping setResourceMappingFor(Class<?> type) {
		ResourceMapping rm = resourceMappings.get(type);
		if (null == rm) {
			rm = new ResourceMapping(type);
			resourceMappings.put(type, rm);
		}
		return rm;
	}

	public ResourceMapping getResourceMappingFor(Class<?> type) {
		return resourceMappings.get(type);
	}

	public boolean hasResourceMappingFor(Class<?> type) {
		return resourceMappings.containsKey(type);
	}

	public Class<?> findTypeForPath(String path) {
		if (null == path) {
			return null;
		}
		for (Map.Entry<Class<?>, ResourceMapping> entry : resourceMappings.entrySet()) {
			if (path.equals(entry.getValue().getPath())) {
				return entry.getKey();
			}
		}
		return null;
	}

}
