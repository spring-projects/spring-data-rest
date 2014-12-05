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
package org.springframework.data.rest.webmvc.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.rest.webmvc.AbstractWebIntegrationTests;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Integration tests for customization of Spring Data REST's exception handling.
 * 
 * @author Thibaud Lepretre
 * @author Oliver Gierke
 */
@ContextConfiguration
public class ExceptionHandlingCustomizationIntegrationTests extends AbstractWebIntegrationTests {

	@Configuration
	@Import(JpaRepositoryConfig.class)
	static class ControllerAdviceConfig {

		@ControllerAdvice
		@Order(Ordered.HIGHEST_PRECEDENCE)
		static class CustomGlobalConfiguration {

			@ExceptionHandler
			ResponseEntity<Void> handle(HttpRequestMethodNotSupportedException o_O) {

				HttpHeaders headers = new HttpHeaders();
				headers.setAllow(o_O.getSupportedHttpMethods());

				return new ResponseEntity<Void>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}

	@Test
	public void httpRequestMethodNotSupportedExceptionShouldNowReturnHttpStatus500Over405() throws Exception {

		Link link = client.discoverUnique("addresses");

		mvc.perform(get(link.getHref())).//
				andExpect(status().isInternalServerError());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.rest.webmvc.AbstractWebIntegrationTests#expectedRootLinkRels()
	 */
	@Override
	protected Iterable<String> expectedRootLinkRels() {
		return Collections.emptySet();
	}
}
