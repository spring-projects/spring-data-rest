package org.springframework.data.rest.core.convert;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class UUIDToStringConverter implements Converter<UUID, String> {
  @Override public String convert(UUID uuid) {
    return (null != uuid ? uuid.toString() : null);
  }
}
