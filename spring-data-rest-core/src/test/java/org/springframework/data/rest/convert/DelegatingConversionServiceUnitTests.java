/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.convert;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Tests to ensure the {@link DelegatingConversionService} properly delegates conversions to the
 * {@link org.springframework.core.convert.ConversionService} that is appropriate for the given source and return types.
 * 
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingConversionServiceUnitTests {

	private static final UUID RANDOM_UUID = UUID.fromString("9deccfd7-f892-4e26-a4d5-c92893392e78");

	@Mock ConversionService conversionService;
	DelegatingConversionService delegatingConversionService;

	@Before
	public void setup() {

		DefaultFormattingConversionService cs = new DefaultFormattingConversionService(false);
		cs.addConverter(UUIDConverter.INSTANCE);

		delegatingConversionService = new DelegatingConversionService(conversionService, cs);

		when(conversionService.canConvert(String.class, UUID.class)).thenReturn(false);
		when(conversionService.canConvert(UUID.class, String.class)).thenReturn(false);
	}

	@Test
	public void shouldDelegateToProperConversionService() {

		assertThat(delegatingConversionService.canConvert(String.class, UUID.class), is(true));
		assertThat(delegatingConversionService.convert(RANDOM_UUID.toString(), UUID.class), is(RANDOM_UUID));

		verifyConversionService();
	}

	@Test
	public void shouldConvertUUIDToString() {

		assertThat(delegatingConversionService.canConvert(UUID.class, String.class), is(true));
		assertThat(delegatingConversionService.convert(RANDOM_UUID, String.class), is(RANDOM_UUID.toString()));

		verifyConversionService();
	}

	private void verifyConversionService() {

		verify(conversionService, times(0)).convert(Matchers.any(String.class), eq(UUID.class));
		verify(conversionService, times(0)).convert(Matchers.any(UUID.class), eq(String.class));
	}
}
