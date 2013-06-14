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

import org.springframework.util.Assert;


class CollectionResourceMappingBuilder implements InternalMappingBuilder {
	
	private final CollectionResourceMapping mapping;
	
	public CollectionResourceMappingBuilder(CollectionResourceMapping mapping) {
		this.mapping = mapping;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.InternalMappingBuilder#withCollectionRel(java.lang.String)
	 */
	public CollectionResourceMappingBuilder withCollectionRel(String rel) {
		
		SimpleCollectionResourceMapping newMapping = new SimpleCollectionResourceMapping(rel != null ? rel : mapping.getRel(),
				mapping.getSingleResourceRel(), mapping.getPath(), mapping.isExported());

		return new CollectionResourceMappingBuilder(newMapping);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.InternalMappingBuilder#withSingleRel(java.lang.String)
	 */
	@Override
	public CollectionResourceMappingBuilder withSingleRel(String rel) {
		
		SimpleCollectionResourceMapping newMapping = new SimpleCollectionResourceMapping(mapping.getRel(),
				rel != null ? rel : mapping.getSingleResourceRel(), mapping.getPath(), mapping.isExported());
		
		return new CollectionResourceMappingBuilder(newMapping);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.InternalMappingBuilder#withPath(java.lang.String)
	 */
	@Override
	public CollectionResourceMappingBuilder withPath(String path) {
		
		SimpleCollectionResourceMapping newMapping = new SimpleCollectionResourceMapping(mapping.getRel(),
				mapping.getSingleResourceRel(), path != null ? path : mapping.getPath(), mapping.isExported());
		
		return new CollectionResourceMappingBuilder(newMapping);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.InternalMappingBuilder#withExposed(java.lang.Boolean)
	 */
	@Override
	public CollectionResourceMappingBuilder withExposed(Boolean exported) {
		
		SimpleCollectionResourceMapping newMapping = new SimpleCollectionResourceMapping(mapping.getRel(),
				mapping.getSingleResourceRel(), mapping.getPath(), exported != null ? exported : mapping.isExported());
		
		return new CollectionResourceMappingBuilder(newMapping);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.InternalMappingBuilder#merge(org.springframework.data.rest.repository.mapping.CollectionResourceMapping)
	 */
	@Override
	public InternalMappingBuilder merge(CollectionResourceMapping mapping) {
		
		if (mapping == null) {
			return this;
		}
		
		return withCollectionRel(mapping.getRel()). //
				withSingleRel(mapping.getSingleResourceRel()). //
				withPath(mapping.getPath()). //
				withExposed(mapping.isExported());
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.InternalMappingBuilder#getMapping()
	 */
	@Override
	public CollectionResourceMapping getMapping() {
		
		Assert.hasText(mapping.getRel(), "Rel must not be null or empty!");
		
		return mapping;
	}

	/**
	 * @return the exported
	 */
	public Boolean isExported() {
		return mapping.isExported();
	}
}