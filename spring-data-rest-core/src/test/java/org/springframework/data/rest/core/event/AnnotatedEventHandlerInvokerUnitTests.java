/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.rest.core.event;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.core.domain.Person;
import org.springframework.data.rest.core.event.AnnotatedEventHandlerInvoker.EventHandlerMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Unit tests for {@link AnnotatedEventHandlerInvoker}.
 *
 * @author Oliver Gierke
 * @author Fabian Trampusch
 */
public class AnnotatedEventHandlerInvokerUnitTests {

    /**
     * @see DATAREST-582
     */
    @Test
    @SuppressWarnings("unchecked")
    public void doesNotDiscoverMethodsOnProxyClasses() {

        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(new Sample());
        factory.setProxyTargetClass(true);

        AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
        invoker.postProcessAfterInitialization(factory.getProxy(), "proxy");

        MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod> methods = (MultiValueMap<Class<? extends RepositoryEvent>, EventHandlerMethod>) ReflectionTestUtils
                .getField(invoker, "handlerMethods");

        assertThat(methods.get(BeforeCreateEvent.class), hasSize(1));
    }

    /**
     * @see DATAREST-606
     */
    @Test
    public void invokesPrivateEventHandlerMethods() {

        SampleWithPrivateHandler sampleHandler = new SampleWithPrivateHandler();

        AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
        invoker.postProcessAfterInitialization(sampleHandler, "sampleHandler");

        invoker.onApplicationEvent(new BeforeCreateEvent(new Person("Dave", "Matthews")));

        assertThat(sampleHandler.wasCalled, is(true));
    }

    /**
     * @see DATAREST-983
     */
    @Test
    public void invokesEventHandlerOnParentClass() {

        FirstEventHandler firstHandler = new FirstEventHandler();
        SecondEventHandler secondHandler = new SecondEventHandler();

        AnnotatedEventHandlerInvoker invoker = new AnnotatedEventHandlerInvoker();
        invoker.postProcessAfterInitialization(firstHandler, "listHandler");
        invoker.postProcessAfterInitialization(secondHandler, "setHandler");

        invoker.onApplicationEvent(new BeforeCreateEvent(new FirstEntity()));
        invoker.onApplicationEvent(new BeforeCreateEvent(new SecondEntity()));

        assertThat(firstHandler.callCount, is(1));
        assertThat(secondHandler.callCount, is(1));
    }

    @RepositoryEventHandler
    static class Sample {

        @HandleBeforeCreate
        public void method(Sample sample) {
        }
    }

    @RepositoryEventHandler
    static class SampleWithPrivateHandler {

        boolean wasCalled = false;

        @HandleBeforeCreate
        private void method(Person sample) {
            wasCalled = true;
        }
    }

    static class AbstractBaseEntityEventHandler<T extends BaseEntity> {
        int callCount = 0;

        @HandleBeforeCreate
        private void method(T entity) {
            callCount += 1;
        }
    }

    @RepositoryEventHandler
    static class FirstEventHandler extends AbstractBaseEntityEventHandler<FirstEntity> {
    }

    @RepositoryEventHandler
    static class SecondEventHandler extends AbstractBaseEntityEventHandler<SecondEntity> {
    }

    @Data
    static abstract class BaseEntity {
        private String id;
        private String createdBy;
        private LocalDate createdOn;
        private String modifiedBy;
        private LocalDate modifiedOn;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class FirstEntity extends BaseEntity {
        private String foo1;
        private String bar1;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class SecondEntity extends BaseEntity {
        private String foo2;
        private String bar2;
    }

}
