package org.springframework.data.rest.repository.invoke;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.rest.repository.annotation.ConvertWith;
import org.springframework.util.Assert;

/**
 * A special conversion service that can convert {@link MethodParameter}s and their values to a target type, taking into
 * account any specific conversion instructions annotated on the parameter with {@link ConvertWith}.
 * 
 * @author Jon Brisbin
 */
public class MethodParameterConversionService {

	private final ConversionService delegateConversionService;

	public MethodParameterConversionService(ConversionService delegateConversionService) {
		Assert.notNull(delegateConversionService, "Delegate ConversionService cannot be null.");
		this.delegateConversionService = delegateConversionService;
	}

	public boolean canConvert(Class<?> sourceType, MethodParameter param) {
		return canConvert(TypeDescriptor.valueOf(sourceType), param);
	}

	public boolean canConvert(TypeDescriptor sourceType, MethodParameter param) {
		return (delegateConversionService.canConvert(sourceType, new TypeDescriptor(param)) || param
				.hasParameterAnnotation(ConvertWith.class));
	}

	public <T> T convert(Object source, MethodParameter param) {
		return convert(source, TypeDescriptor.forObject(source), param);
	}

	@SuppressWarnings({ "unchecked" })
	public <T> T convert(Object source, TypeDescriptor sourceType, MethodParameter param) {
		TypeDescriptor targetType = new TypeDescriptor(param);

		try {
			if (param.hasParameterAnnotation(ConvertWith.class)) {
				Converter<Object, T> converter = (Converter<Object, T>) param.getParameterAnnotation(ConvertWith.class).value()
						.newInstance();
				return converter.convert(source);
			} else {
				return (T) delegateConversionService.convert(source, sourceType, targetType);
			}
		} catch (Exception e) {
			throw new ConversionFailedException(sourceType, targetType, source, e);
		}
	}

}
