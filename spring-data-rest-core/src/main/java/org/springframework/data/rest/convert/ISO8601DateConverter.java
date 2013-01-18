package org.springframework.data.rest.convert;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Jon Brisbin
 */
public class ISO8601DateConverter implements ConditionalGenericConverter,
                                             Converter<String[], Date> {

  public static final ConditionalGenericConverter INSTANCE = new ISO8601DateConverter();

  private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS = new HashSet<ConvertiblePair>();

  static {
    CONVERTIBLE_PAIRS.add(new ConvertiblePair(String.class, Date.class));
    CONVERTIBLE_PAIRS.add(new ConvertiblePair(Date.class, String.class));
  }

  @Override public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    if(String.class.isAssignableFrom(sourceType.getType())) {
      return Date.class.isAssignableFrom(targetType.getType());
    }

    return Date.class.isAssignableFrom(sourceType.getType())
        && String.class.isAssignableFrom(targetType.getType());
  }

  @Override public Set<ConvertiblePair> getConvertibleTypes() {
    return CONVERTIBLE_PAIRS;
  }

  @Override public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    DateFormat dateFmt = iso8601DateFormat();
    if(String.class.isAssignableFrom(sourceType.getType())) {
      return dateFmt.format(source);
    } else {
      try {
        return dateFmt.parse(source.toString());
      } catch(ParseException e) {
        throw new ConversionFailedException(sourceType, targetType, source, e);
      }
    }
  }

  @Override public Date convert(String[] source) {
    if(source.length > 0) {
      try {
        return iso8601DateFormat().parse(source[0]);
      } catch(ParseException e) {
        throw new ConversionFailedException(
            TypeDescriptor.valueOf(String[].class),
            TypeDescriptor.valueOf(Date.class),
            source[0],
            new IllegalArgumentException("Source does not conform to ISO8601 date format (YYYY-MM-DDTHH:MM:SS-0000")
        );
      }
    }
    return null;
  }

  private DateFormat iso8601DateFormat() {
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  }

}
