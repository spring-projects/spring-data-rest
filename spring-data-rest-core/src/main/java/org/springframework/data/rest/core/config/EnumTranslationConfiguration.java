/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.rest.core.config;

/**
 * Configuration options for enum value translation.
 * 
 * @author Oliver Gierke
 * @since 2.4
 * @soundtrack Wallis Bird - Measuring Cities (Yeah! Wallis Bird live 2007-2014)
 */
public interface EnumTranslationConfiguration {

	/**
	 * Configures whether the default translation of enum names shall be applied. Defaults to {@literal true}. This means
	 * the configuration will turn enum names into human friendly {@link String}s and also parse them if - only if - no
	 * explicit translation is available.
	 * 
	 * @param enableDefaultTranslation whether to enable the default translation of enum names.
	 */
	void setEnableDefaultTranslation(boolean enableDefaultTranslation);

	/**
	 * Configures whether to always accept the raw enum name when parsing. This is useful if clients were used to send the
	 * Java enum names shall not be broken even if on the serialization side enum translation is activated.
	 * 
	 * @param parseEnumNameAsFallback whether to parse the raw enum value as fallback, even if an explicit translation is
	 *          available.
	 */
	void setParseEnumNameAsFallback(boolean parseEnumNameAsFallback);
}
