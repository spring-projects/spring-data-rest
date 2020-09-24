/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.rest.webmvc.RestControllerConfiguration;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportSelector} choosing additional controller configuration (eg. for the {@litearl Hal Explorer}) when
 * present.
 *
 * @author Christoph Strobl
 * @since 3.4
 */
class RestControllerImportSelector implements ImportSelector {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ImportSelector#selectImports(org.springframework.core.type.AnnotationMetadata)
	 */
	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {

		if (ClassUtils.isPresent("org.springframework.data.rest.webmvc.halexplorer.HalExplorerConfiguration",
				importingClassMetadata.getClass().getClassLoader())) {

			return new String[] { RestControllerConfiguration.class.getName(),
					"org.springframework.data.rest.webmvc.halexplorer.HalExplorerConfiguration" };
		}

		return new String[] { RestControllerConfiguration.class.getName() };
	}
}
