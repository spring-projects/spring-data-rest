package org.springframework.data.rest.test.webmvc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Jon Brisbin
 */
public class StringToISODateConverter implements Converter<String[], Date> {
  @Override public Date convert(String[] s) {
    if(s.length == 1) {
      try {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(s[0]);
      } catch(ParseException e) {
        throw new IllegalArgumentException(e);
      }
    }

    throw new IllegalArgumentException("Can only parse a single date in the parameter.");
  }
}