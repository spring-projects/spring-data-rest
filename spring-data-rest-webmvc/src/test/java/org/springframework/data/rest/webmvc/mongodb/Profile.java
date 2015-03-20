/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.mongodb;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@Document
public class Profile {

	@Id private String id;
	private Long person;
	private String type;
	private Map<String, String> metadata = new HashMap<String, String>();

	public String getId() {
		return id;
	}

	public Profile setId(String id) {
		this.id = id;
		return this;
	}

	public Long getPerson() {
		return person;
	}

	public Profile setPerson(Long person) {
		this.person = person;
		return this;
	}

	public String getType() {
		return type;
	}

	public Profile setType(String type) {
		this.type = type;
		return this;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
}
