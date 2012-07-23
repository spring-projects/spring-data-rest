package org.springframework.data.rest.webmvc;

import java.io.IOException;
import java.nio.charset.Charset;
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
 * @author Jon Brisbin
 */
public abstract class JacksonUtil {

  public static final Charset DEFAULT_CHARSET = Charset.forName( "UTF-8" );
  public static final MediaType COMPACT_JSON = new MediaType( "application",
                                                              "x-spring-data-compact+json",
                                                              DEFAULT_CHARSET );
  public static final MediaType VERBOSE_JSON = new MediaType( "application",
                                                              "x-spring-data-verbose+json",
                                                              DEFAULT_CHARSET );
  public static final MediaType APPLICATION_JAVASCRIPT = new MediaType( "application",
                                                                        "javascript",
                                                                        DEFAULT_CHARSET );

  private JacksonUtil() {
  }

  public static MappingJacksonHttpMessageConverter createJacksonHttpMessageConverter( final ObjectMapper objectMapper ) {
    CustomSerializerFactory customSerializerFactory = new CustomSerializerFactory();
    customSerializerFactory.addSpecificMapping( SimpleLink.class, new FluentBeanSerializer( SimpleLink.class ) );
    objectMapper.setSerializerFactory( customSerializerFactory );
    MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter() {
      {
        setSupportedMediaTypes( Arrays.asList( MediaType.APPLICATION_JSON, COMPACT_JSON, VERBOSE_JSON ) );
      }

      @Override
      protected void writeInternal( Object object, HttpOutputMessage outputMessage )
          throws IOException,
                 HttpMessageNotWritableException {
        JsonEncoding encoding = getJsonEncoding( outputMessage.getHeaders().getContentType() );
        // Believe it or not, this is the only way to get pretty-printing from Jackson in this configuration
        JsonGenerator jsonGenerator = objectMapper
            .getJsonFactory()
            .createJsonGenerator( outputMessage.getBody(), encoding )
            .useDefaultPrettyPrinter();
        try {
          objectMapper.writeValue( jsonGenerator, object );
        } catch ( IOException ex ) {
          throw new HttpMessageNotWritableException( "Could not write JSON: " + ex.getMessage(), ex );
        }
      }
    };
    jsonConverter.setObjectMapper( objectMapper );

    return jsonConverter;
  }

}
