package org.springframework.data.rest.core.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.springframework.util.ClassUtils;

/**
 * A "fluent" bean is one that does not use the JavaBean conventions of "setProperty" and "getProperty" but instead
 * uses just "property" with 0 or 1 arguments to distinguish between getter (0 arg) and setter (1 arg).
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class FluentBeanSerializer extends SerializerBase<Object> {

  @SuppressWarnings({"unchecked", "rawtypes"})
  public FluentBeanSerializer( final Class t ) {
    super( t );

    if ( !FluentBeanUtils.isFluentBean( t ) ) {
      throw new IllegalArgumentException( "Class of type " + t + " is not a FluentBean" );
    }
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public void serialize( final Object value,
                         final JsonGenerator jgen,
                         final SerializerProvider provider )
      throws IOException,
             JsonGenerationException {
    if ( null == value ) {
      provider.defaultSerializeNull( jgen );
    } else {
      Class<?> type = value.getClass();
      if ( ClassUtils.isAssignable( type, Collection.class ) ) {
        jgen.writeStartArray();
        for ( Object o : (Collection) value ) {
          write( o, jgen, provider );
        }
        jgen.writeEndArray();
      } else if ( ClassUtils.isAssignable( type, Map.class ) ) {
        jgen.writeStartObject();
        for ( Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet() ) {
          jgen.writeFieldName( entry.getKey() );
          write( entry.getValue(), jgen, provider );
        }
        jgen.writeEndObject();
      } else {
        write( value, jgen, provider );
      }
    }
  }

  private void write( final Object value,
                      final JsonGenerator jgen,
                      final SerializerProvider provider ) throws IOException {
    Class<?> type = value.getClass();
    if ( ClassUtils.isAssignable( type, _handledType ) ) {
      jgen.writeStartObject();
      for ( String fname : FluentBeanUtils.metadata( type ).fieldNames() ) {
        jgen.writeFieldName( fname );
        write( FluentBeanUtils.get( fname, value ), jgen, provider );
      }
      jgen.writeEndObject();
    } else {
      jgen.writeObject( value );
    }
  }

}
