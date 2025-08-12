package org.springframework.data.rest.webmvc.support;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A helper that renders an {@link Exception} JSON-friendly.
 *
 * @author Jon Brisbin
 */
public class ExceptionMessage {

	private final Throwable throwable;

	public ExceptionMessage(Throwable throwable) {
		this.throwable = throwable;
	}

	@JsonProperty("message")
	public @Nullable String getMessage() {
		return throwable.getMessage();
	}

	@JsonProperty("cause")
	public @Nullable ExceptionMessage getCause() {
		return throwable.getCause() != null ? new ExceptionMessage(throwable.getCause()) : null;
	}
}
