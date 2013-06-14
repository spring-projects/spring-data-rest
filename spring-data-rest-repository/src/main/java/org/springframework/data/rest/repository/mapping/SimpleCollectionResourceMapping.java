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


public class SimpleCollectionResourceMapping implements CollectionResourceMapping {
	
	private final String collectionRel;
	private final String singleRel;
	private final String path;
	private final Boolean exported;

	public SimpleCollectionResourceMapping(String relsAndPath) {
		this(relsAndPath, relsAndPath, relsAndPath, true);
	}
	
	public SimpleCollectionResourceMapping(String collectionRel, String singleRel, String path, Boolean exported) {
		
		this.collectionRel = collectionRel;
		this.singleRel = singleRel;
		this.path = path;
		this.exported = exported;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#getCollectionRel()
	 */
	public String getRel() {
		return collectionRel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#getSingleResourceRel()
	 */
	public String getSingleResourceRel() {
		return singleRel;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.repository.mapping.CollectionResourceMapping#getPath()
	 */
	@Override
	public String getPath() {
		return path;
	}

	/**
	 * @return the exported
	 */
	public Boolean isExported() {
		return exported;
	}
}