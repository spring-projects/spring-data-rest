package org.springframework.data.rest.webmvc.support;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Thibaud Lepretre
 */
@Configuration
@Import(JpaRepositoryConfig.class)
public class ControllerAdviceConfig {
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ControllerAdvice
	public static class CustomGlobalConfiguration {
		@ExceptionHandler
		public ResponseEntity<Void> handle(HttpRequestMethodNotSupportedException o_O) {
			HttpHeaders headers = new HttpHeaders();
			headers.setAllow(o_O.getSupportedHttpMethods());

			return new ResponseEntity<Void>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
