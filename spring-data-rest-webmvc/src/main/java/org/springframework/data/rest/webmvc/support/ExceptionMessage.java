package org.springframework.data.rest.webmvc.support;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A helper that renders an {@link Exception} JSON-friendly.
 * 
 * @author Jon Brisbin
 */
public class ExceptionMessage {

	private final Throwable exception;

	public ExceptionMessage(Throwable exception) {
		this.exception = exception;
	}

	@JsonProperty("message")
	public String getMessage() {
		return exception.getMessage();
	}

	@JsonProperty("cause")
	public ExceptionMessage getCause() {
		if (null != exception.getCause()) {
			return new ExceptionMessage(exception.getCause());
		}
		return null;
	}

}
