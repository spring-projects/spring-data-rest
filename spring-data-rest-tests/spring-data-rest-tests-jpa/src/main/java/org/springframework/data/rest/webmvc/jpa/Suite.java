/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.rest.webmvc.jpa;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Alex Leigh
 */
@Entity
@DiscriminatorValue("S")
public class Suite extends Room {

	static final String TYPE = "suite";

	private String suiteCode;

	@Override
	public String getType() {
		return TYPE;
	}

	public String getSuiteCode() {
		return this.suiteCode;
	}

	public void setSuiteCode(String suiteCode) {
		this.suiteCode = suiteCode;
	}
}
