/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.rest.webmvc.aot;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.rest.webmvc.BasePathAwareController;

/**
 * @author Christoph Strobl
 * @since 4.0
 */
public class BasePathAwareControllerBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {

		if(!AnnotatedElementUtils.isAnnotated(registeredBean.getBeanClass(), BasePathAwareController.class)) {
			return null;
		}

		return new BasePathAwareControllerContribution(registeredBean.getBeanClass());
	}


	static class BasePathAwareControllerContribution implements BeanRegistrationAotContribution {

		private final Class<?> controllerType;

		public BasePathAwareControllerContribution(Class<?> controllerType) {
			this.controllerType = controllerType;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			// TODO: invoke proxy factory and call getTargetClass() to obtain bytecode
		}
	}
}
