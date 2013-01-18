package org.springframework.data.rest.convert;

import java.util.Stack;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * This {@link ConversionService} implementation delegates the actual conversion to the {@literal ConversionService} it
 * finds in its internal {@link Stack} that claims to be able to convert a given class. It will roll through the
 * {@literal ConversionService}s until it finds one that can convert the given type.
 *
 * @author Jon Brisbin
 */
public class DelegatingConversionService implements ConversionService {

  private Stack<ConversionService> conversionServices = new Stack<ConversionService>();

  public DelegatingConversionService() {
  }

  public DelegatingConversionService(ConversionService... svcs) {
    addConversionServices(svcs);
  }

  /**
   * Add {@link ConversionService}s to the internal list of those to delegate to.
   *
   * @param svcs
   *     The ConversionServices to delegate to (in order).
   *
   * @return @this
   */
  public DelegatingConversionService addConversionServices(ConversionService... svcs) {
    for(ConversionService svc : svcs) {
      conversionServices.add(svc);
    }
    return this;
  }

  /**
   * Add a {@link ConversionService} to the internal list at a specific index for controlling the priority.
   *
   * @param atIndex
   *     Where in the stack to add this ConversionService.
   * @param svc
   *     The ConversionService to add.
   *
   * @return
   */
  public DelegatingConversionService addConversionService(int atIndex, ConversionService svc) {
    conversionServices.add(atIndex, svc);
    return this;
  }

  @Override public boolean canConvert(Class<?> from, Class<?> to) {
    for(ConversionService svc : conversionServices) {
      if(svc.canConvert(from, to)) {
        return true;
      }
    }
    return false;
  }

  @Override public boolean canConvert(TypeDescriptor from, TypeDescriptor to) {
    for(ConversionService svc : conversionServices) {
      if(svc.canConvert(from, to)) {
        return true;
      }
    }
    return false;
  }

  @Override public <T> T convert(Object o, Class<T> type) {
    for(ConversionService svc : conversionServices) {
      if(svc.canConvert(o.getClass(), type)) {
        return svc.convert(o, type);
      }
    }
    throw new ConverterNotFoundException(TypeDescriptor.forObject(o), TypeDescriptor.valueOf(type));
  }

  @Override public Object convert(Object o, TypeDescriptor from, TypeDescriptor to) {
    for(ConversionService svc : conversionServices) {
      if(svc.canConvert(from, to)) {
        return svc.convert(o, from, to);
      }
    }
    throw new ConverterNotFoundException(from, to);
  }

}
