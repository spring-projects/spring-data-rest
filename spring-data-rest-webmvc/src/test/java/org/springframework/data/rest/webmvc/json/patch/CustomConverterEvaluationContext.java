package org.springframework.data.rest.webmvc.json.patch;

import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

public class CustomConverterEvaluationContext {

	public static StandardEvaluationContext stringToDateEvaluationContext(){
		
		StandardEvaluationContext context = new StandardEvaluationContext();
		DefaultConversionService service = new DefaultConversionService();
		service.addConverter(new StringToDateConverter());
		
		StandardTypeConverter converter = new StandardTypeConverter(service);
		context.setTypeConverter(converter);
		
		return context;
		
	}
	
	static class StringToDateConverter implements Converter<String, Date> {
		@Override
		public Date convert(String date) {
			return Date.from(ZonedDateTime.parse(date).toInstant());
		}
	}
}
