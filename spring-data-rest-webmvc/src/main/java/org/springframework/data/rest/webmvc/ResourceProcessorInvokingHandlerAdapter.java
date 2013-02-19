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
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Special {@link RequestMappingHandlerAdapter} that tweaks the {@link HandlerMethodReturnValueHandlerComposite} to be
 * proxied by a {@link ResourceProcessorHandlerMethodReturnValueHandler} which will invoke the {@link
 * ResourceProcessor}s
 * found in the application context and eventually delegate to the originally configured
 * {@link HandlerMethodReturnValueHandler}.
 * <p/>
 * This is a separate component as it might make sense to deploy it in a standalone SpringMVC application to enable
 * post
 * processing. It would actually make most sense in Spring HATEOAS project.
 *
 * @author Oliver Gierke
 */
public class ResourceProcessorInvokingHandlerAdapter extends RequestMappingHandlerAdapter {

  @Autowired(required = false)
  private List<ResourceProcessor<?>> resourcesProcessors = new ArrayList<ResourceProcessor<?>>();

  /**
   * Empty constructor to setup a {@link ResourceProcessorInvokingHandlerAdapter}.
   */
  public ResourceProcessorInvokingHandlerAdapter() {

  }

  /**
   * Copy constructor to copy configuration of {@link HttpMessageConverter}s, {@link WebBindingInitializer}, custom
   * {@link HandlerMethodArgumentResolver}s and custom {@link HandlerMethodReturnValueHandler}s.
   *
   * @param original
   *     must not be {@literal null}.
   */
  public ResourceProcessorInvokingHandlerAdapter(RequestMappingHandlerAdapter original) {

    Assert.notNull(original);

    setMessageConverters(original.getMessageConverters());
    setWebBindingInitializer(original.getWebBindingInitializer());
    setCustomArgumentResolvers(original.getCustomArgumentResolvers());
    setCustomReturnValueHandlers(original.getCustomReturnValueHandlers());
  }

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
    newHandlers.add(new ResourceProcessorHandlerMethodReturnValueHandler(oldHandlers, resourcesProcessors));

    // Configure the new handler to be used
    this.setReturnValueHandlers(newHandlers);
  }
}
