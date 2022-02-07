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
package org.springframework.data.rest.webmvc.util;

import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.util.Assert;

/**
 * {@link HttpInputMessage} based on a plain {@link InputStream}, i.e. exposing no headers.
 *
 * @author Oliver Drotbohm
 */
public class InputStreamHttpInputMessage implements HttpInputMessage {

	private final InputStream body;

	private InputStreamHttpInputMessage(final InputStream body) {

		Assert.notNull(body, "InputStream must not be null!");

		this.body = body;
	}

	public static InputStreamHttpInputMessage of(final InputStream body) {
		return new InputStreamHttpInputMessage(body);
	}

	public InputStream getBody() {
		return this.body;
	}

	@Override
	public HttpHeaders getHeaders() {
		return HttpHeaders.EMPTY;
	}
}
