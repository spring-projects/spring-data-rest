/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.core.event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ValidatingRepositoryEventListener}
 *
 * @author Elefhterios Laskaridis
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidatingRepositoryEventListenerUnitTests {

    private ValidatingRepositoryEventListener subject;

    @Mock private ObjectFactory<PersistentEntities> persistentEntitiesObjectFactory;
    @Mock private PersistentEntities persistentEntities;

    @Before
    public void setUp() {
        when(persistentEntitiesObjectFactory.getObject()).thenReturn(persistentEntities);
        this.subject = new ValidatingRepositoryEventListener(this.persistentEntitiesObjectFactory);
    }

    private static final class StubModel { }

    private static final class StubModelValidator implements Validator {

        private Object validationTarget;

        public boolean isInvoked() {
            return this.validationTarget != null;
        }

        public Object getValidationTarget() {
            return validationTarget;
        }

        @Override
        public boolean supports(Class<?> aClass) {
            return StubModel.class.equals(aClass);
        }

        @Override
        public void validate(Object o, Errors errors) {
            this.validationTarget = o;
        }
    }

    @Test // DATAREST-524
    public void invokesAfterCreateValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterCreate", validator);
        this.subject.onAfterCreate(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterDeleteValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterDelete", validator);
        this.subject.onAfterDelete(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterDeleteValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterDeleteStubModelValidator", validator);
        this.subject.onAfterDelete(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterCreateValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterCreateStubModelValidator", validator);
        this.subject.onAfterCreate(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void doesNotInvokeAfterLinkDeleteValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterLinkDelete", validator);
        this.subject.onAfterLinkDelete(validationTarget, null);

        assertFalse(validator.isInvoked());
        assertNull(validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterLinkDeleteValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterLinkDeleteStubModelValidator", validator);
        this.subject.onAfterLinkDelete(validationTarget, null);

        assertFalse(validator.isInvoked());
        assertNull(validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterLinkSaveValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterLinkSave", validator);
        this.subject.onAfterLinkSave(validationTarget, null);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterLinkSaveValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterLinkSaveStubModelValidator", validator);
        this.subject.onAfterLinkSave(validationTarget, null);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterSaveValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterSave", validator);
        this.subject.onAfterSave(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesAfterSaveValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("afterSaveStubModelValidator", validator);
        this.subject.onAfterSave(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeCreateValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeCreate", validator);
        this.subject.onBeforeCreate(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeCreateValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeCreateStubModelValidator", validator);
        this.subject.onBeforeCreate(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeDeleteValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeDelete", validator);
        this.subject.onBeforeDelete(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeDeleteValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeDeleteStubModelValidator", validator);
        this.subject.onBeforeDelete(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeLinkDeleteValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeLinkDelete", validator);
        this.subject.onBeforeLinkDelete(validationTarget, null);

        assertFalse(validator.isInvoked());
        assertNull(validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeLinkDeleteValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeLinkDeleteStubModelValidator", validator);
        this.subject.onBeforeLinkDelete(validationTarget, null);

        assertFalse(validator.isInvoked());
        assertNull(validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeLinkSaveValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeLinkSave", validator);
        this.subject.onBeforeLinkSave(validationTarget, null);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeLinkSaveValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeLinkSaveStubModelValidator", validator);
        this.subject.onBeforeLinkSave(validationTarget, null);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeSaveValidator_whenValidatorIsRegisteredByEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeSave", validator);
        this.subject.onBeforeSave(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }

    @Test // DATAREST-524
    public void invokesBeforeSaveValidator_whenValidatorIsRegisteredByCompositeEventName() {
        StubModelValidator validator = new StubModelValidator();
        StubModel validationTarget = new StubModel();

        this.subject.addValidator("beforeSaveStubModelValidator", validator);
        this.subject.onBeforeSave(validationTarget);

        assertTrue(validator.isInvoked());
        assertEquals(validationTarget, validator.getValidationTarget());
    }
}
