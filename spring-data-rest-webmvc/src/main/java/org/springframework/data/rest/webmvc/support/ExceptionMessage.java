package org.springframework.data.rest.webmvc.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A helper that renders an {@link Exception} JSON-friendly.
 * <p>
 * Only the outermost exception message is serialized into the HTTP response. The cause chain is accessible via
 * {@link #getCause()} for server-side use but is excluded from JSON serialization to prevent leaking internals to
 * clients.
 *
 * @author Jon Brisbin
 * @author Oliver Drotbohm
 */
public class ExceptionMessage {

	private final Throwable throwable;
	private final String message;

	public ExceptionMessage(Throwable throwable) {

		Assert.notNull(throwable, "Throwable must not be null!");

		this.throwable = throwable;
		this.message = throwable.getMessage();
	}

	public ExceptionMessage(String message) {

		Assert.notNull(message, "Message must not be null!");

		this.message = message;
		this.throwable = null;
	}

	@JsonProperty("message")
	public String getMessage() {
		return message;
	}

	@JsonIgnore
	public @Nullable ExceptionMessage getCause() {

		if (throwable == null) {
			return null;
		}

		Throwable cause = throwable.getCause();

		if (cause == null) {
			return null;
		}

		return new ExceptionMessage(cause);
	}
}
