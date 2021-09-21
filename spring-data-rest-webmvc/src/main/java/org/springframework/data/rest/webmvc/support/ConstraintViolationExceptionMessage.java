package org.springframework.data.rest.webmvc.support;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jon Brisbin
 */
public class ConstraintViolationExceptionMessage {

	private final ConstraintViolationException cve;
	private final List<ConstraintViolationMessage> messages = new ArrayList<ConstraintViolationMessage>();

	public ConstraintViolationExceptionMessage(ConstraintViolationException cve, MessageSource msgSrc, Locale locale) {
		this.cve = cve;
		for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
			messages.add(new ConstraintViolationMessage(cv, msgSrc, locale));
		}
	}

	@JsonProperty("cause")
	public String getCause() {
		return cve.getMessage();
	}

	@JsonProperty("messages")
	public List<ConstraintViolationMessage> getMessages() {
		return messages;
	}

}
