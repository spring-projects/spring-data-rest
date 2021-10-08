/*
 * Copyright 2013-2021 the original author or authors.
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

import org.hibernate.Version;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;

/**
 * Helper class to register datatype modules based on their presence in the classpath.
 *
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public class Jackson2DatatypeHelper {

	private static final boolean IS_HIBERNATE_AVAILABLE = ClassUtils.isPresent("org.hibernate.Version",
			Jackson2DatatypeHelper.class.getClassLoader());
	private static final boolean IS_HIBERNATE5_MODULE_AVAILABLE = ClassUtils.isPresent(
			"com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule",
			Jackson2DatatypeHelper.class.getClassLoader());

	public static void configureObjectMapper(ObjectMapper mapper) {

		if (IS_HIBERNATE_AVAILABLE && HibernateVersions.isHibernate5() && IS_HIBERNATE5_MODULE_AVAILABLE) {
			new Hibernate5ModuleRegistrar().registerModule(mapper);
		}
	}

	private static class HibernateVersions {

		public static boolean isHibernate5() {
			return Version.getVersionString().startsWith("5");
		}
	}

	private static class Hibernate5ModuleRegistrar {

		public void registerModule(ObjectMapper mapper) {

			Hibernate5JakartaModule module = new Hibernate5JakartaModule();
			module.enable(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING);

			mapper.registerModule(module);
		}
	}
}
