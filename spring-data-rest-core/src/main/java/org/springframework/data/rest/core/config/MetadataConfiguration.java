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
package org.springframework.data.rest.core.config;

/**
 * Configuration for metadata exposure.
 * 
 * @author Oliver Gierke
 */
public class MetadataConfiguration {

	private boolean omitUnresolvableDescriptionKeys = true;
	private boolean alpsEnabled = true;

	/**
	 * Configures whether to omit documentation attributes for unresolvable resource bundle keys. Defaults to
	 * {@literal true}, which means that an unsuccessful attempt to resolve the message will cause no documentation entry
	 * to be rendered for the metadata resources.
	 * 
	 * @param omitUnresolvableDescriptionKeys whether to omit documentation attributes for unresolvable resource bundle
	 *          keys.
	 */
	public void setOmitUnresolvableDescriptionKeys(boolean omitUnresolvableDescriptionKeys) {
		this.omitUnresolvableDescriptionKeys = omitUnresolvableDescriptionKeys;
	}

	/**
	 * Returns whether to omit documentation attributes for unresolvable resource bundle keys.
	 * 
	 * @return the omitUnresolvableDescriptionKeys
	 */
	public boolean omitUnresolvableDescriptionKeys() {
		return omitUnresolvableDescriptionKeys;
	}

	/**
	 * Configures whether to expose the ALPS resources.
	 * 
	 * @param alpsEnabled the alpsEnabled to set
	 */
	public void setAlpsEnabled(boolean enableAlps) {
		this.alpsEnabled = enableAlps;
	}

	/**
	 * Returns whether the ALPS resources are exposed.
	 * 
	 * @return the alpsEnabled
	 */
	public boolean alpsEnabled() {
		return alpsEnabled;
	}
}
