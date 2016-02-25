package org.springframework.data.rest.core.domain;

import lombok.Value;

import java.util.UUID;

import org.springframework.data.annotation.Id;

/**
 * @author Jon Brisbin
 */
@Value
public class Profile {

	@Id UUID id = UUID.randomUUID();
	String name, type;
}
