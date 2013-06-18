package org.springframework.data.rest.webmvc.support;

import static java.lang.String.*;

import java.util.Locale;
import javax.validation.ConstraintViolation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.MessageSource;

/**
 * A helper class to encapsulate {@link ConstraintViolation} errors.
 * 
 * @author Jon Brisbin
 */
public class ConstraintViolationMessage {

	private final ConstraintViolation<?> violation;
	private final String message;

	public ConstraintViolationMessage(ConstraintViolation<?> violation, MessageSource msgSrc, Locale locale) {
		this.violation = violation;
		this.message = msgSrc.getMessage(violation.getMessageTemplate(),
				new Object[] { violation.getLeafBean().getClass().getSimpleName(), violation.getPropertyPath().toString(),
						violation.getInvalidValue() }, violation.getMessage(), locale);
	}

	@JsonProperty("entity")
	public String getEntity() {
		return violation.getRootBean().getClass().getName();
	}

	@JsonProperty("message")
	public String getMessage() {
		return message;
	}

	@JsonProperty("invalidValue")
	public String getInvalidValue() {
		return format("%s", violation.getInvalidValue());
	}

	@JsonProperty("property")
	public String getProperty() {
		return violation.getPropertyPath().toString();
	}

}
