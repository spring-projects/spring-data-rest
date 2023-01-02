/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.SpringProxy;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link BeanRegistrationAotProcessor} to register proxy hints for projection interfaces.
 *
 * @author Oliver Drotbohm
 * @since 4.0
 */
class ProjectionProxyAotProcessor implements BeanRegistrationAotProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectionProxyAotProcessor.class);

	private static Class<?>[] ADDITIONAL_INTERFACES = new Class<?>[] { //
			TargetAware.class, //
			SpringProxy.class, //
			DecoratingProxy.class
	};

	private Set<String> packagesSeen = new HashSet<>();

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {

		if (!ClassUtils.isAssignable(RepositoryFactoryBeanSupport.class, registeredBean.getBeanClass())) {
			return null;
		}

		var holder = registeredBean.getMergedBeanDefinition() //
				.getConstructorArgumentValues() //
				.getIndexedArgumentValue(0, String.class);

		var repositoryInterface = (String) holder.getValue();
		var packageToScan = ClassUtils.getPackageName(repositoryInterface);

		LOGGER.debug("Detecting projection interfaces in {}", packageToScan);

		if (packagesSeen.contains(packageToScan)) {
			return null;
		}

		packagesSeen.add(packageToScan);

		return (context, code) -> {

			var classLoader = registeredBean.getBeanFactory().getBeanClassLoader();
			var proxies = context.getRuntimeHints().proxies();

			var scanner = new AnnotatedTypeScanner(Projection.class);
			scanner.setResourceLoader(new DefaultResourceLoader(classLoader));
			scanner.findTypes(packageToScan)
					.forEach(it -> {

						LOGGER.debug("Registering proxy config for projection interface {}.", it.getName());

						proxies.registerJdkProxy(ObjectUtils.addObjectToArray(ADDITIONAL_INTERFACES, it, 0));
					});

		};
	}
}
