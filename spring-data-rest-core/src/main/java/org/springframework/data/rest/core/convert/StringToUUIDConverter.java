package org.springframework.data.rest.core.convert;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class StringToUUIDConverter implements Converter<String, UUID> {
  @Override public UUID convert(String s) {
    return (null != s ? UUID.fromString(s) : null);
  }
}
