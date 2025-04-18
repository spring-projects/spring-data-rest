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
@DiscriminatorValue("D")
public class Dinner extends Meal {

	public static final String TYPE = "dinner";

	private String dinnerCode;

	@Override
	public String getType() {
		return TYPE;
	}

	public String getDinnerCode() {
		return this.dinnerCode;
	}

	public void setDinnerCode(String dinnerCode) {
		this.dinnerCode = dinnerCode;
	}
}
