package org.springframework.data.rest.convert;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * For converting a {@link UUID} into a {@link String}.
 *
 * @author Jon Brisbin
 */
public class UUIDConverter implements ConditionalGenericConverter {

  public static final  UUIDConverter        INSTANCE          = new UUIDConverter();
  private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS = new HashSet<ConvertiblePair>();

  static {
    CONVERTIBLE_PAIRS.add(new ConvertiblePair(String.class, UUID.class));
    CONVERTIBLE_PAIRS.add(new ConvertiblePair(UUID.class, String.class));
  }

  @Override public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    if(String.class.isAssignableFrom(sourceType.getType())) {
      return UUID.class.isAssignableFrom(targetType.getType());
    }

    return UUID.class.isAssignableFrom(sourceType.getType())
        && String.class.isAssignableFrom(targetType.getType());
  }

  @Override public Set<ConvertiblePair> getConvertibleTypes() {
    return CONVERTIBLE_PAIRS;
  }

  @Override public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if(String.class.isAssignableFrom(sourceType.getType())) {
      return UUID.fromString(source.toString());
    } else {
      return source.toString();
    }
  }

}
