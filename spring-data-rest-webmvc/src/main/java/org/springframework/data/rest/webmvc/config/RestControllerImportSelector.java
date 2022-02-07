/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.rest.webmvc.RestControllerConfiguration;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportSelector} choosing additional controller configuration (eg. for the {@litearl Hal Explorer}) when
 * present.
 *
 * @author Christoph Strobl
 * @author Oliver Drotbohm
 * @since 3.4
 */
class RestControllerImportSelector implements ImportSelector {

	private static final String HAL_EXPLORER_CONFIGURATION = "org.springframework.data.rest.webmvc.halexplorer.HalExplorerConfiguration";
	private static final String JMOLECULES_SPRING = "org.jmolecules.spring.IdentifierToPrimitivesConverter";

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {

		List<String> configurations = new ArrayList<>();
		configurations.add(RestControllerConfiguration.class.getName());

		ClassLoader classLoader = importingClassMetadata.getClass().getClassLoader();

		if (ClassUtils.isPresent(HAL_EXPLORER_CONFIGURATION, classLoader)) {
			configurations.add(HAL_EXPLORER_CONFIGURATION);
		}

		if (ClassUtils.isPresent(JMOLECULES_SPRING, classLoader)) {
			configurations.add(JMoleculesConfigurer.class.getName());
		}

		return configurations.toArray(new String[configurations.size()]);
	}
}
