/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import lombok.Data;

/**
 * Configuration object for DoaminObjectReader
 * <p>
 *
 * @author Joe Valerio
 */
@Data
public class DomainObjectReaderConfiguration {

	/**
	 * Causes all values in arrays to be replaced
	 *
	 * Default behavior is to merge the array entities.
	 */
	private boolean replaceAllArrayValues;

}
