/*
 * Copyright 2013-2024 the original author or authors.
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
package org.springframework.data.rest.webmvc.json;

import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

/**
 * Helper class to register datatype modules based on their presence in the classpath.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class Jackson2DatatypeHelper {

	private static final boolean IS_HIBERNATE_AVAILABLE = ClassUtils.isPresent("org.hibernate.Version",
			Jackson2DatatypeHelper.class.getClassLoader());
	private static final boolean IS_HIBERNATE6_MODULE_AVAILABLE = ClassUtils.isPresent(
			"com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module",
			Jackson2DatatypeHelper.class.getClassLoader());

	public static void configureObjectMapper(ObjectMapper mapper) {

		if (IS_HIBERNATE_AVAILABLE && IS_HIBERNATE6_MODULE_AVAILABLE) {
			new Hibernate6ModuleRegistrar().registerModule(mapper);
		}
	}

	private static class Hibernate6ModuleRegistrar {

		public void registerModule(ObjectMapper mapper) {

			var module = new Hibernate6Module();
			module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);

			mapper.registerModule(module);
		}
	}
}
