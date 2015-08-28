/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.rest.webmvc.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * A {@link BeanPostProcessor} that looks for {@link DispatcherServlet}s and turns on OPTIONS
 * dispatching for the application to support {@link org.springframework.data.rest.webmvc.RepositoryEntityController}
 * request mappings for HTTP OPTIONS.
 *
 * @author Greg Turnquist
 * @since 2.4
 */
public class DispatcherServletOptionsEnablingBeanPostProcessor implements BeanPostProcessor {

	/**
	 * Nothing to do during before initialization.
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * Look for any {@link DispatcherServlet} and turn on OPTIONS to be dispatched to the app.
	 *
	 * @param bean
	 * @param beanName
	 * @return
	 * @throws BeansException
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		if (DispatcherServlet.class.isAssignableFrom(bean.getClass())) {
			DispatcherServlet dispatcherServlet = (DispatcherServlet) bean;
			dispatcherServlet.setDispatchOptionsRequest(true);
		}

		return bean;
	}
}
