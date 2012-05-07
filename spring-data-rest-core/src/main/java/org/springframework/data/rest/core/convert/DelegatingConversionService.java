package org.springframework.data.rest.core.convert;

import java.util.Stack;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class DelegatingConversionService implements ConversionService {

  private Stack<ConversionService> conversionServices = new Stack<ConversionService>();

  public DelegatingConversionService() {
  }

  public DelegatingConversionService(ConversionService... svcs) {
    addConversionServices(svcs);
  }

  public DelegatingConversionService addConversionServices(ConversionService... svcs) {
    for (ConversionService svc : svcs) {
      conversionServices.add(svc);
    }
    return this;
  }

  public DelegatingConversionService addConversionService(int atIndex, ConversionService svc) {
    conversionServices.add(atIndex, svc);
    return this;
  }

  @Override public boolean canConvert(Class<?> from, Class<?> to) {
    for (ConversionService svc : conversionServices) {
      if (svc.canConvert(from, to)) {
        return true;
      }
    }
    return false;
  }

  @Override public boolean canConvert(TypeDescriptor from, TypeDescriptor to) {
    for (ConversionService svc : conversionServices) {
      if (svc.canConvert(from, to)) {
        return true;
      }
    }
    return false;
  }

  @Override public <T> T convert(Object o, Class<T> type) {
    for (ConversionService svc : conversionServices) {
      if (svc.canConvert(o.getClass(), type)) {
        return svc.convert(o, type);
      }
    }
    throw new ConverterNotFoundException(TypeDescriptor.forObject(o), TypeDescriptor.valueOf(type));
  }

  @Override public Object convert(Object o, TypeDescriptor from, TypeDescriptor to) {
    for (ConversionService svc : conversionServices) {
      if (svc.canConvert(from, to)) {
        return svc.convert(o, from, to);
      }
    }
    throw new ConverterNotFoundException(from, to);
  }

}
