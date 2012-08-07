package org.springframework.data.rest.test.webmvc;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Jon Brisbin
 */
public class StringToListOfLongsConverter implements Converter<String[], List<Long>> {

  @Override public List<Long> convert(String[] source) {
    List<Long> longs = new ArrayList<Long>();
    for(String s : source) {
      longs.add(Long.parseLong(s));
    }
    return longs;
  }

}
