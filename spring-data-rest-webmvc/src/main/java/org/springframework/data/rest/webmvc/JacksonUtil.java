package org.springframework.data.rest.webmvc;

import java.io.IOException;
import java.util.Arrays;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.springframework.data.rest.core.SimpleLink;
import org.springframework.data.rest.core.util.FluentBeanSerializer;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

/**
 * Utility class for creating a custom-configured {@see MappingJacksonHttpMessageConverter} that has our own
 * serializers and {@see MediaType} mappings on it.
 *
 * @author Jon Brisbin
 */
public abstract class JacksonUtil {

  private JacksonUtil() {
  }

  public static MappingJacksonHttpMessageConverter createJacksonHttpMessageConverter(final ObjectMapper objectMapper) {
    // We need a custom serializer for handling beans that don't conform to the JavaBeans standard 'get' and 'set'
    CustomSerializerFactory customSerializerFactory = new CustomSerializerFactory();
    customSerializerFactory.addSpecificMapping(SimpleLink.class, new FluentBeanSerializer(SimpleLink.class));
    objectMapper.setSerializerFactory(customSerializerFactory);
    // We want to support all our custom types of JSON and also the catch-all
    MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter() {
      {
        setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaTypes.COMPACT_JSON,
            MediaTypes.VERBOSE_JSON,
            MediaType.ALL
        ));
      }

      @Override
      protected void writeInternal(Object object, HttpOutputMessage outputMessage)
          throws IOException,
                 HttpMessageNotWritableException {
        JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
        // Believe it or not, this is the only way to get pretty-printing from Jackson in this configuration
        JsonGenerator jsonGenerator = objectMapper
            .getJsonFactory()
            .createJsonGenerator(outputMessage.getBody(), encoding)
            .useDefaultPrettyPrinter();
        try {
          objectMapper.writeValue(jsonGenerator, object);
        } catch(IOException ex) {
          throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
        }
      }
    };
    jsonConverter.setObjectMapper(objectMapper);

    return jsonConverter;
  }

}
