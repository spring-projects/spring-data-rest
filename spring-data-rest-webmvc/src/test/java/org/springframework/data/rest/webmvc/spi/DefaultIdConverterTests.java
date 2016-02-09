/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.rest.webmvc.spi;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import static org.springframework.data.rest.webmvc.spi.BackendIdConverter.DefaultIdConverter.INSTANCE;

/**
 * Unit tests for {@link BackendIdConverter.DefaultIdConverter}.
 *
 * @author Andrew Walters
 */
public class DefaultIdConverterTests {
    /**
     * @see DATAREST-763
     */
    @Test
    public void idsForUseWithinAURIShouldEncodeSpaces() {
        assertThat(INSTANCE.toRequestId("I contain spaces", Object.class), is("I+contain+spaces"));
    }

    /**
     * @see DATAREST-763
     */
    @Test
    public void idsForUseWithinAURIShouldEncodeSpacesAtEnd() {
        assertThat(INSTANCE.toRequestId("I contain spaces   ", Object.class), is("I+contain+spaces+++"));
    }

    /**
     * @see DATAREST-763
     *
     * This one is slightly more interesting - the %2F *may* be rejected by web servers as part of a generated URL
     * Apache inparticular denies all URLs with %2F in the path part, for security reasons
     * but we can't allow [raw] slashes to be present in an ID as paths could then be subverted - precisely the reason
     * that Apache rejects the %2F.
     *
     * I'd propose double-encoding/decoding the '/' --> %2f --> %252f for maximum portability
     */
    @Test
    public void idsForUseWithinAURIShouldEncodeToAvoidPathExploiting() {
        assertThat(INSTANCE.toRequestId("I/contain/slashes", Object.class), is("I%252Fcontain%252Fslashes"));
    }

    /**
     * @see DATAREST-763
     */
    @Test
    public void idsForUseWithinAURIShouldEncodeQuestionMarkToAvoidQueryStringExploiting() {
        assertThat(INSTANCE.toRequestId("IContainAQuestionMark?size=1000000", Object.class),
                is("IContainAQuestionMark%3Fsize%3D1000000"));
    }

    /**
     * @see DATAREST-763
     */
    @Test
    public void idsForUseWithinAURIShouldDecodeQuestionMarks() {
        assertThat((String)INSTANCE.fromRequestId("IContainAQuestionMark%3Fsize%3D1000000", Object.class),
                is("IContainAQuestionMark?size=1000000"));
    }

    /**
     * @see DATAREST-763
     */
    @Test
    public void idsForUseWithinAURIShouldDecodeSlashes() {
        assertThat((String)INSTANCE.fromRequestId("I%252Fcontain%252Fslashes", Object.class),
                is("I/contain/slashes"));
    }

    /**
     * @see DATAREST-763
     */
    @Test
    public void idsForUseWithinAURIShouldDecodeSpaces() {
        assertThat((String)INSTANCE.fromRequestId("I%20contain%20spaces", Object.class),
                is("I contain spaces"));
    }
}
