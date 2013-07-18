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

import static org.springframework.data.rest.core.support.ResourceMappingUtils.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jon Brisbin
 */
@Deprecated
public class ResourceMapping {

	private String rel;
	private String path;
	private boolean exported = true;
	private final Map<String, ResourceMapping> resourceMappings = new HashMap<String, ResourceMapping>();

	public ResourceMapping() {}

	public ResourceMapping(Class<?> type) {
		rel = findRel(type);
		path = findPath(type);
		exported = findExported(type);
	}

	public ResourceMapping(String rel, String path) {
		this.rel = rel;
		this.path = path;
	}

	public ResourceMapping(String rel, String path, boolean exported) {
		this.rel = rel;
		this.path = path;
		this.exported = exported;
	}

	public String getRel() {
		return rel;
	}

	public ResourceMapping setRel(String rel) {
		this.rel = rel;
		return this;
	}

	public String getPath() {
		return path;
	}

	public ResourceMapping setPath(String path) {
		this.path = path;
		return this;
	}

	public boolean isExported() {
		return exported;
	}

	public ResourceMapping setExported(boolean exported) {
		this.exported = exported;
		return this;
	}

	public ResourceMapping addResourceMappings(Map<String, ResourceMapping> mappings) {
		if (null == mappings) {
			return this;
		}

		resourceMappings.putAll(mappings);
		return this;
	}

	public ResourceMapping addResourceMappingFor(String name) {
		ResourceMapping rm = new ResourceMapping();
		resourceMappings.put(name, rm);
		return rm;
	}

	public ResourceMapping getResourceMappingFor(String name) {
		return resourceMappings.get(name);
	}

	public boolean hasResourceMappingFor(String name) {
		return resourceMappings.containsKey(name);
	}

	public Map<String, ResourceMapping> getResourceMappings() {
		return resourceMappings;
	}

	public String getNameForPath(String path) {
		for (Map.Entry<String, ResourceMapping> mapping : resourceMappings.entrySet()) {
			if (mapping.getValue().getPath().equals(path)) {
				return mapping.getKey();
			}
		}
		return path;
	}

	@Override
	public String toString() {
		return "ResourceMapping{" + "rel='" + rel + '\'' + ", path='" + path + '\'' + ", exported=" + exported
				+ ", resourceMappings=" + resourceMappings + '}';
	}

}
