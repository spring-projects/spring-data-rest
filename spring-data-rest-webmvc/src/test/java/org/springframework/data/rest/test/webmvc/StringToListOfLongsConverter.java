package org.springframework.data.rest.test.webmvc;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin
 */
public class StringToListOfLongsConverter implements Converter<String[], List<Long>> {

  @Override public List<Long> convert(String[] source) {
    List<Long> longs = new ArrayList<Long>();
    String strings;
    if(source.length == 1) {
      strings = source[0];
    } else {
      strings = StringUtils.arrayToCommaDelimitedString(source);
    }
    for(String s : StringUtils.commaDelimitedListToStringArray(strings)) {
      longs.add(Long.parseLong(s));
    }
    return longs;
  }

}
