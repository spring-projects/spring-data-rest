package org.springframework.data.rest.webmvc;

import static org.springframework.data.util.ClassTypeInformation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.rest.webmvc.support.JsonpResponse;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 */
public class PersistentEntityResourceProcessorReturnValueHandler implements HandlerMethodReturnValueHandler {

  private final HandlerMethodReturnValueHandler delegate;
  private final List<Wrapper> processors = new ArrayList<Wrapper>();

  @SuppressWarnings({"unchecked"})
  public PersistentEntityResourceProcessorReturnValueHandler(HandlerMethodReturnValueHandler delegate,
                                                             List<ResourceProcessor<?>> processors) {
    this.delegate = delegate;
    for(ResourceProcessor<?> rp : processors) {
      TypeInformation<?> componentType = from(rp.getClass())
          .getSuperTypeInformation(ResourceProcessor.class)
          .getComponentType();
      if(Resources.class.isAssignableFrom(componentType.getType())) {
        this.processors.add(new ResourcesProcessorWrapper(componentType.getComponentType().getType(),
                                                          (ResourceProcessor<Resources<?>>)rp));
      } else if(Resource.class.isAssignableFrom(componentType.getType())) {
        this.processors.add(new ResourceProcessorWrapper(componentType.getComponentType().getType(),
                                                         (ResourceProcessor<Resource<?>>)rp));
      }
    }
  }

  @Override public boolean supportsReturnType(MethodParameter returnType) {
    Class<?> controller = returnType.getMethod().getDeclaringClass();
    return RepositoryController.class.isAssignableFrom(controller)
        || RepositoryEntityController.class.isAssignableFrom(controller)
        || RepositoryPropertyReferenceController.class.isAssignableFrom(controller)
        || RepositorySearchController.class.isAssignableFrom(controller);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public void handleReturnValue(Object returnValue,
                                MethodParameter methodParam,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest nativeRequest) throws Exception {
    Class<?> returnValueType = returnValue.getClass();
    Class<?> entityType = null;

    if(JsonpResponse.class.isAssignableFrom(returnValueType)) {
      entityType = ((JsonpResponse)returnValue).getResponseEntity().getBody().getClass();
    } else if(ResponseEntity.class.isAssignableFrom(returnValueType)) {
      entityType = ((ResponseEntity)returnValue).getBody().getClass();
    } else if(Resources.class.isAssignableFrom(returnValueType)) {
      Collection c = ((Resources)returnValue).getContent();
      Object o;
      if(null != c && !c.isEmpty() && null != (o = c.iterator().next())) {
        entityType = o.getClass();
      } else {
        if(delegate.supportsReturnType(methodParam)) {
          delegate.handleReturnValue(returnValue,
                                     methodParam,
                                     mavContainer,
                                     nativeRequest);
        }
        return;
      }
    } else if(Resource.class.isAssignableFrom(returnValueType)) {
      entityType = ((Resource)returnValue).getContent().getClass();
    }

    for(Wrapper w : processors) {
      if(w.type.isAssignableFrom(entityType)) {
        if(ResourcesProcessorWrapper.class.isAssignableFrom(w.getClass())
            && Resources.class.isAssignableFrom(returnValueType)) {
          ((ResourcesProcessorWrapper)w).processor.process((Resources<?>)returnValue);
        } else if(ResourceProcessorWrapper.class.isAssignableFrom(w.getClass())
            && Resource.class.isAssignableFrom(returnValueType)) {
          ((ResourceProcessorWrapper)w).processor.process((Resource<?>)returnValue);
        }
      }
    }

    if(delegate.supportsReturnType(methodParam)) {
      delegate.handleReturnValue(returnValue,
                                 methodParam,
                                 mavContainer,
                                 nativeRequest);
    }
  }

  static class Wrapper {
    Class<?>           type;
    TypeInformation<?> typeInfo;

    Wrapper(Class<?> type) {
      this.type = type;
      this.typeInfo = from(type);
    }
  }

  static class ResourcesProcessorWrapper extends Wrapper {
    ResourceProcessor<Resources<?>> processor;

    ResourcesProcessorWrapper(Class<?> type, ResourceProcessor<Resources<?>> processor) {
      super(type);
      this.processor = processor;
    }
  }

  static class ResourceProcessorWrapper extends Wrapper {
    ResourceProcessor<Resource<?>> processor;

    ResourceProcessorWrapper(Class<?> type, ResourceProcessor<Resource<?>> processor) {
      super(type);
      this.processor = processor;
    }
  }

}
