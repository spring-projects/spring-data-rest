/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.rest.webmvc;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.ResourcesProcessor;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Special {@link RequestMappingHandlerAdapter} that tweaks the {@link HandlerMethodReturnValueHandlerComposite} to be
 * proxied by a {@link ResourceProcessingHandlerMethodReturnValueHandler} which will invoke the
 * {@link ResourceProcessor}s/{@link ResourcesProcessor}s found in the application context and eventually delegate to
 * the originally configured {@link HandlerMethodReturnValueHandler}.
 * <p>
 * This is a separate component as it might make sense to deploy it in a standalone SpringMVC application to enable post
 * processing. It would actually make most sense in Spring HATEOAS project.
 * 
 * @author Oliver Gierke
 */
public class ResourcePostProcessorInvokingHandlerAdapter extends RequestMappingHandlerAdapter {

	@Autowired(required = false)
	private List<ResourceProcessor<? extends Resource<?>>> resourceProcessors = new ArrayList<ResourceProcessor<? extends Resource<?>>>();

	@Autowired(required = false)
	private List<ResourcesProcessor<? extends Resources<?>>> resourcesProcessors = new ArrayList<ResourcesProcessor<? extends Resources<?>>>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		super.afterPropertiesSet();

		// Retrieve actual handlers to use as delegate
		HandlerMethodReturnValueHandlerComposite oldHandlers = getReturnValueHandlers();

		// Set up ResourceProcessingHandlerMethodResolver to delegate to originally configured ones
		List<HandlerMethodReturnValueHandler> newHandlers = new ArrayList<HandlerMethodReturnValueHandler>();
		newHandlers.add(new ResourceProcessingHandlerMethodReturnValueHandler(oldHandlers, resourceProcessors,
				resourcesProcessors));

		// Configure the new handler to be used
		this.setReturnValueHandlers(newHandlers);
	}
}
