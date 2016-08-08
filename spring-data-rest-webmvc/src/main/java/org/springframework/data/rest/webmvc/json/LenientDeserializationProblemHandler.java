package org.springframework.data.rest.webmvc.json;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.data.rest.webmvc.json.DeserializationErrors.DeserializationError;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

class LenientDeserializationProblemHandler extends DeserializationProblemHandler {

	private DeserializationErrors errors;

	public LenientDeserializationProblemHandler(String propertyBase) {
		this.errors = DeserializationErrors.of(propertyBase, Collections.<DeserializationError> emptyList());
	}

	public boolean hasErrors() {
		return errors.iterator().hasNext();
	}

	/**
	 * Returns the errors currently captured by the problem handler.
	 * 
	 * @return the errors must not be {@literal null}.
	 */
	public DeserializationErrors getErrors() {
		return errors;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.DeserializationProblemHandler#handleWeirdNumberValue(com.fasterxml.jackson.databind.DeserializationContext, java.lang.Class, java.lang.Number, java.lang.String)
	 */
	@Override
	public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType, Number valueToConvert,
			String failureMsg) throws IOException {
		return super.handleWeirdNumberValue(ctxt, targetType, valueToConvert, failureMsg);
	}

	/* 
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.DeserializationProblemHandler#handleWeirdStringValue(com.fasterxml.jackson.databind.DeserializationContext, java.lang.Class, java.lang.String, java.lang.String)
	 */
	@Override
	public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert,
			String failureMsg) throws IOException {

		JsonParser parser = ctxt.getParser();

		this.errors = errors.reject(getPath(parser), valueToConvert,
				new InvalidFormatException(parser, failureMsg, valueToConvert, targetType));

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.DeserializationProblemHandler#handleInstantiationProblem(com.fasterxml.jackson.databind.DeserializationContext, java.lang.Class, java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> instClass, Object argument,
			Throwable t) throws IOException {

		this.errors = errors.reject(getPath(ctxt.getParser()), argument, (JsonMappingException) t);

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.DeserializationProblemHandler#handleMissingInstantiator(com.fasterxml.jackson.databind.DeserializationContext, java.lang.Class, com.fasterxml.jackson.core.JsonParser, java.lang.String)
	 */
	@Override
	public Object handleMissingInstantiator(DeserializationContext ctxt, Class<?> instClass, JsonParser p, String msg)
			throws IOException {

		this.errors = errors.reject(getPath(p), p.getCurrentValue(),
				JsonMappingException.from(ctxt, String.format("Couldn't create instance of type %s!", instClass)));
		return null;
	}

	/**
	 * Registers a {@link DeserializationError} for the given {@link JsonMappingException}.
	 * 
	 * @param exception must not be {@literal null}.
	 * @return
	 */
	public LenientDeserializationProblemHandler register(JsonMappingException exception) {

		Assert.notNull(exception, "JsonMappingException must not be null!");

		this.errors = errors.reject(getPath(exception.getPath()), getValue(exception), exception);
		return this;
	}

	private static Object getValue(JsonMappingException exception) {

		if (exception instanceof InvalidFormatException) {
			return ((InvalidFormatException) exception).getValue();
		}

		JsonParser parser = (JsonParser) exception.getProcessor();

		return parser.getCurrentValue();
	}

	private static String getPath(List<Reference> references) {

		String head = references.get(0).getFieldName();

		return references.size() == 1 ? head : head.concat(".").concat(getPath(references.subList(1, references.size())));
	}

	private static String getPath(JsonParser parser) {
		return getPath(parser.getParsingContext());
	}

	private static String getPath(JsonStreamContext context) {

		if (context == null) {
			return "";
		}

		JsonStreamContext parent = context.getParent();

		return parent == null || parent.inRoot() ? context.getCurrentName() : concat(parent, context);
	}

	private static String concat(JsonStreamContext parent, JsonStreamContext context) {

		String path = getPath(parent);

		if (context.inArray()) {
			return path.concat("[").concat(String.valueOf(context.getCurrentIndex())).concat("]");
		}

		return path.concat(".").concat(context.getCurrentName());
	}
}
