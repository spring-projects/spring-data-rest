/*
 * Copyright 2016-2018 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.jpa.groovy;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Simulates a Groovy domain object by extending {@link GroovyObject}.
 *
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
@Entity
class SimulatedGroovyDomainClass implements GroovyObject {

	private @Id @GeneratedValue Long id;
	private String name;

	public Long getId() {
		return id;
	}

	void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	//
	// The following fields don't actually have to be implemented since the test cases don't
	// make any Groovy calls. This just simulates the structure of a Groovy object to
	// verify proper handling.
	//

	@Override
	public Object invokeMethod(String s, Object o) {
		return null;
	}

	@Override
	public Object getProperty(String s) {
		return null;
	}

	@Override
	public void setProperty(String s, Object o) {}

	@Override
	public MetaClass getMetaClass() {
		return null;
	}

	@Override
	public void setMetaClass(MetaClass metaClass) {}
}
