package org.springframework.data.rest.repository.invoke;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.convert.ISO8601DateConverter;
import org.springframework.data.rest.repository.domain.jpa.PersonRepository;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * @author Jon Brisbin
 */
public class MethodParameterConversionServiceUnitTests {

  static final SimpleDateFormat ISO8601_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  static final String[]         DATE_S      = new String[]{"2010-01-01T12:00:00-0600"};
  static final Date DATE_D;

  static {
    try {
      DATE_D = ISO8601_FMT.parse(DATE_S[0]);
    } catch(ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  MethodParameter findByCreatedGreaterThan;
  MethodParameter findByCreatedUsingISO8601Date;

  @Before
  public void setup() throws NoSuchMethodException {
    findByCreatedGreaterThan = new MethodParameter(PersonRepository.class.getMethod("findByCreatedGreaterThan",
                                                                                    Date.class,
                                                                                    Pageable.class), 0);
    findByCreatedUsingISO8601Date = new MethodParameter(PersonRepository.class.getMethod("findByCreatedUsingISO8601Date",
                                                                                         Date.class,
                                                                                         Pageable.class), 0);
  }

  @SuppressWarnings({"deprecation"})
  @Test
  public void shouldConvertDateParameterUsingDefaultConverter() throws Exception {
    ConfigurableConversionService cs = new DefaultFormattingConversionService();
    MethodParameterConversionService conversionService = new MethodParameterConversionService(cs);

    String dateStr = "01/01/2010";
    assertThat(conversionService.canConvert(String.class, findByCreatedGreaterThan), is(true));
    assertThat((Date)conversionService.convert(dateStr, findByCreatedGreaterThan), is(new Date(dateStr)));
  }

  @Test
  public void shouldConvertDateParameterUsingSpecificConverter() throws Exception {
    ConfigurableConversionService cs = new DefaultFormattingConversionService();
    cs.addConverter(ISO8601DateConverter.INSTANCE);
    MethodParameterConversionService conversionService = new MethodParameterConversionService(cs);

    assertThat(conversionService.canConvert(String.class, findByCreatedUsingISO8601Date), is(true));
    assertThat((Date)conversionService.convert(DATE_S, findByCreatedUsingISO8601Date), is(DATE_D));
  }

}
