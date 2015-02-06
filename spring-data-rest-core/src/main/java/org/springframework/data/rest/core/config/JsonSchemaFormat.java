package org.springframework.data.rest.core.config;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonSchemaFormat {

	EMAIL, DATE_TIME, HOSTNAME, IPV4, IPV6, URI;

	@JsonValue
	public String toString() {
		return name().toLowerCase(Locale.US).replaceAll("_", "-");
	}
}
