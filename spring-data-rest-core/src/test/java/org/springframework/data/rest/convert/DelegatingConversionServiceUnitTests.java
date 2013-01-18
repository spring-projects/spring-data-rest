package org.springframework.data.rest.convert;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.UUID;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.rest.AbstractJMockTests;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Tests to ensure the {@link DelegatingConversionService} properly delegates conversions to the {@link
 * org.springframework.core.convert.ConversionService} that is appropriate for the given source and return types.
 *
 * @author Jon Brisbin
 */
public class DelegatingConversionServiceUnitTests extends AbstractJMockTests {

  private static final UUID RANDOM_UUID = UUID.fromString("9deccfd7-f892-4e26-a4d5-c92893392e78");

  private ConversionService           conversionService;
  private DelegatingConversionService delegatingConversionService;

  @Before
  public void setup() {
    conversionService = context.mock(ConversionService.class);

    DefaultFormattingConversionService cs = new DefaultFormattingConversionService(false);
    cs.addConverter(UUIDConverter.INSTANCE);

    delegatingConversionService = new DelegatingConversionService(
        conversionService,
        cs
    );

    context.checking(new Expectations() {{
      allowing(conversionService).canConvert(String.class, UUID.class);
      will(returnValue(false));
      allowing(conversionService).canConvert(UUID.class, String.class);
      will(returnValue(false));

      // Ensure the first ConversionService is never asked to convert this String into a UUID
      never(conversionService).convert(with(any(String.class)), with(UUID.class));
      never(conversionService).convert(with(any(UUID.class)), with(String.class));
    }});
  }

  @Test
  public void shouldDelegateToProperConversionService() throws Exception {
    assertThat(delegatingConversionService.canConvert(String.class, UUID.class), is(true));
    assertThat(delegatingConversionService.convert(RANDOM_UUID.toString(), UUID.class), is(RANDOM_UUID));
  }

  @Test
  public void shouldConvertUUIDToString() throws Exception {
    assertThat(delegatingConversionService.canConvert(UUID.class, String.class), is(true));
    assertThat(delegatingConversionService.convert(RANDOM_UUID, String.class), is(RANDOM_UUID.toString()));
  }

}
