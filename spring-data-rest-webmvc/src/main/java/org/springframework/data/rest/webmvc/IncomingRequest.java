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
package org.springframework.data.rest.webmvc;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * Value object to wrap a {@link ServerHttpRequest} to provide a slightly more abstract API to find out about the
 * request method.
 *
 * @author Oliver Gierke
 */
public class IncomingRequest {

	private final ServerHttpRequest request;
	private final MediaType contentType;

	/**
	 * Creates a new {@link IncomingRequest} from {@link ServerHttpRequest}.
	 * 
	 * @param request must not be {@literal null}.
	 */
	public IncomingRequest(ServerHttpRequest request) {

		Assert.notNull(request, "ServerHttpRequest must not be null!");

		this.request = request;
		this.contentType = request.getHeaders().getContentType();
	}

	/**
	 * Returns whether the request is a PATCH request.
	 * 
	 * @return
	 */
	public boolean isPatchRequest() {
		return request.getMethod().equals(HttpMethod.PATCH);
	}

	/**
	 * Returns whether the request is a PATCH request with a payload of type {@link RestMediaTypes#JSON_PATCH_JSON}.
	 * 
	 * @return
	 */
	public boolean isJsonPatchRequest() {
		return isPatchRequest() && RestMediaTypes.JSON_PATCH_JSON.equals(contentType);
	}

	/**
	 * Returns whether the request is a PATCH request with a payload of type {@link RestMediaTypes#MERGE_PATCH_JSON}.
	 * 
	 * @return
	 */
	public boolean isJsonMergePatchRequest() {
		return isPatchRequest() && RestMediaTypes.MERGE_PATCH_JSON.equals(contentType);
	}

	/**
	 * Returns the body of the request.
	 * 
	 * @return will never be {@literal null}.
	 * @throws IOException
	 */
	public InputStream getBody() throws IOException {
		return request.getBody();
	}

	/**
	 * Returns the underlying {@link ServerHttpRequest}.
	 * 
	 * @return will never be {@literal null}.
	 */
	public ServerHttpRequest getServerHttpRequest() {
		return request;
	}
}
