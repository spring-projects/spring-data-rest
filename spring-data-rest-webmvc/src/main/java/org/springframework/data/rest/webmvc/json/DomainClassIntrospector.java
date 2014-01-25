/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import java.util.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * Jackson extension class used to tag all of the domain classes with a specific
 * filter string. This filter string is then used to apply a specific filter to 
 * each domain class during serialization. The actual filter strips out association fields
 * that are already included as links.
 * 
 * @author Nick Weedon
 */
public class DomainClassIntrospector extends JacksonAnnotationIntrospector {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1417867096396135935L;

	public static final String ENTITY_JSON_FILTER = "PersistentEntityJSONFilter";

	public Set<Class<?>> domainClassSet;
	
	public DomainClassIntrospector(List<Class<?>> domainClasses) {

		domainClassSet = new HashSet<Class<?>>();
		domainClassSet.addAll(domainClasses);
	}

	
	@Override
	public Object findFilterId(Annotated ann) {
		// Allow a filter annotation to override the default filter
		Object id = super.findFilterId(ann);

		if(id != null) {
			return id;
		}

		if(domainClassSet.contains(ann.getRawType())) {
    		return ENTITY_JSON_FILTER;
    	}
    	
    	return null;
	}

}

