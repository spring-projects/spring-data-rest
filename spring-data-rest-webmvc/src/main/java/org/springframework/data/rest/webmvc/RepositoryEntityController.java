package org.springframework.data.rest.webmvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.config.ResourceMapping;
import org.springframework.data.rest.repository.PagingAndSorting;
import org.springframework.data.rest.repository.PersistentEntityResource;
import org.springframework.data.rest.repository.context.*;
import org.springframework.data.rest.repository.invoke.RepositoryMethodInvoker;
import org.springframework.data.rest.repository.json.JsonSchema;
import org.springframework.data.rest.repository.json.PersistentEntityToJsonSchemaConverter;
import org.springframework.data.rest.repository.support.DomainObjectMerger;
import org.springframework.hateoas.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jon Brisbin
 */
@RequestMapping("/{repository}")
@SuppressWarnings({"rawtypes"})
public class RepositoryEntityController extends AbstractRepositoryRestController {

	@Autowired
	private DomainObjectMerger                    domainObjectMerger;
	@Autowired
	private PersistentEntityToJsonSchemaConverter jsonSchemaConverter;

	public RepositoryEntityController(Repositories repositories,
																		RepositoryRestConfiguration config,
																		DomainClassConverter domainClassConverter,
																		ConversionService conversionService,
																		EntityLinks entityLinks) {
		super(repositories,
					config,
					domainClassConverter,
					conversionService,
					entityLinks);
	}

	@RequestMapping(
			value = "/schema",
			method = RequestMethod.GET,
			produces = {
					"application/schema+json"
			}
	)
	@ResponseBody
	public JsonSchema schema(RepositoryRestRequest repoRequest) {
		return jsonSchemaConverter.convert(repoRequest.getPersistentEntity().getType());
	}

	@RequestMapping(
			method = RequestMethod.GET,
			produces = {
					"application/json",
					"application/x-spring-data-verbose+json"
			}
	)
	@ResponseBody
	public Resources listEntities(final RepositoryRestRequest repoRequest)
			throws ResourceNotFoundException {
		List<Link> links = new ArrayList<Link>();

		Iterable<?> results;
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker) {
			throw new ResourceNotFoundException();
		}
		boolean hasSortParams = (null != repoRequest.getRequest().getParameter(config.getSortParamName()));
		if (repoMethodInvoker.hasFindAllPageable()) {
			PagingAndSorting pageSort = repoRequest.getPagingAndSorting();
			results = repoMethodInvoker.findAll(new PageRequest(pageSort.getPageNumber(),
																													pageSort.getPageSize(),
																													pageSort.getSort()));
		} else if (repoMethodInvoker.hasFindAllSorted() && hasSortParams) {
			results = repoMethodInvoker.findAll(repoRequest.getPagingAndSorting().getSort());
		} else if (repoMethodInvoker.hasFindAll()) {
			results = repoMethodInvoker.findAll();
		} else {
			throw new ResourceNotFoundException();
		}

		ResourceMapping repoMapping = repoRequest.getRepositoryResourceMapping();
		if (!repoMethodInvoker.getQueryMethods().isEmpty()) {
			links.add(entityLinks.linkForSingleResource(repoRequest.getPersistentEntity().getType(), "search")
													 .withRel(repoMapping.getRel() + ".search"));
		}

		PagingAndSorting pageSort = repoRequest.getPagingAndSorting();
		Link prevLink = null;
		Link nextLink = null;
		if (results instanceof Page) {
			if (((Page) results).hasPreviousPage() && pageSort.getPageNumber() > 0) {
				prevLink = entitiesPageLink(repoRequest, 0, "page.previous");
			}
			if (((Page) results).hasNextPage()) {
				nextLink = entitiesPageLink(repoRequest, 1, "page.next");
			}
		}

		return resultToResources(repoRequest, results, links, prevLink, nextLink);
	}

	@SuppressWarnings({"unchecked"})
	@RequestMapping(
			method = RequestMethod.GET,
			produces = {
					"application/x-spring-data-compact+json",
					"text/uri-list"
			}
	)
	@ResponseBody
	public Resources listEntitiesCompact(final RepositoryRestRequest repoRequest)
			throws ResourceNotFoundException {
		Resources resources = listEntities(repoRequest);
		List<Link> links = new ArrayList<Link>(resources.getLinks());

		for (Resource<?> resource : ((Resources<Resource<?>>) resources).getContent()) {
			PersistentEntityResource<?> persistentEntityResource = (PersistentEntityResource<?>) resource;
			links.add(resourceLink(repoRequest, persistentEntityResource));
		}
		if (resources instanceof PagedResources) {
			return new PagedResources(Collections.emptyList(), ((PagedResources) resources).getMetadata(), links);
		} else {
			return new Resources(Collections.emptyList(), links);
		}
	}

	@SuppressWarnings({"unchecked"})
	@RequestMapping(
			method = RequestMethod.POST,
			consumes = {
					"application/json"
			},
			produces = {
					"application/json",
					"text/uri-list"
			}
	)
	@ResponseBody
	public ResponseEntity<Resource<?>> createNewEntity(RepositoryRestRequest repoRequest,
																										 PersistentEntityResource<?> incoming) {
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || !repoMethodInvoker.hasSaveOne()) {
			throw new NoSuchMethodError();
		}

		applicationContext.publishEvent(new BeforeCreateEvent(incoming.getContent()));
		Object obj = repoMethodInvoker.save(incoming.getContent());
		applicationContext.publishEvent(new AfterCreateEvent(obj));

		BeanWrapper wrapper = BeanWrapper.create(obj, conversionService);
		Link selfLink = entityLinks.linkForSingleResource(
				repoRequest.getPersistentEntity().getType(),
				wrapper.getProperty(repoRequest.getPersistentEntity().getIdProperty())
		).withSelfRel();
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create(selfLink.getHref()));

		if (config.isReturnBodyOnCreate()) {
			return resourceResponse(
					headers,
					new PersistentEntityResource<Object>(
							repoRequest.getPersistentEntity(),
							obj,
							selfLink
					).setBaseUri(repoRequest.getBaseUri()),
					HttpStatus.CREATED
			);
		} else {
			return resourceResponse(headers, null, HttpStatus.CREATED);
		}
	}

	@SuppressWarnings({"unchecked"})
	@RequestMapping(
			value = "/{id}",
			method = RequestMethod.GET,
			produces = {
					"application/json",
					"application/x-spring-data-compact+json",
					"text/uri-list"
			}
	)
	@ResponseBody
	public Resource<?> getSingleEntity(RepositoryRestRequest repoRequest,
																		 @PathVariable String id) throws ResourceNotFoundException {
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || !repoMethodInvoker.hasFindOne()) {
			throw new ResourceNotFoundException();
		}

		Object domainObj = domainClassConverter.convert(
				id,
				STRING_TYPE,
				TypeDescriptor.valueOf(repoRequest.getPersistentEntity().getType())
		);
		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		PersistentEntityResource per = PersistentEntityResource.wrap(repoRequest.getPersistentEntity(),
																																 domainObj,
																																 repoRequest.getBaseUri());
		BeanWrapper wrapper = BeanWrapper.create(domainObj, conversionService);
		Link selfLink = entityLinks.linkForSingleResource(
				repoRequest.getPersistentEntity().getType(),
				wrapper.getProperty(repoRequest.getPersistentEntity().getIdProperty())
		).withSelfRel();
		per.add(selfLink);
		return per;
	}

	@SuppressWarnings({"unchecked"})
	@RequestMapping(
			value = "/{id}",
			method = RequestMethod.PUT,
			consumes = {
					"application/json"
			},
			produces = {
					"application/json",
					"text/uri-list"
			}
	)
	@ResponseBody
	public ResponseEntity<Resource<?>> updateEntity(RepositoryRestRequest repoRequest,
																									PersistentEntityResource<?> incoming,
																									@PathVariable String id) throws ResourceNotFoundException {
		RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || !repoMethodInvoker.hasSaveOne() || !repoMethodInvoker.hasFindOne()) {
			throw new NoSuchMethodError();
		}

		Object domainObj = domainClassConverter.convert(
				id,
				STRING_TYPE,
				TypeDescriptor.valueOf(repoRequest.getPersistentEntity().getType())
		);
		if (null == domainObj) {
			BeanWrapper incomingWrapper = BeanWrapper.create(incoming.getContent(), conversionService);
			PersistentProperty idProp = incoming.getPersistentEntity().getIdProperty();
			incomingWrapper.setProperty(idProp, conversionService.convert(id, idProp.getType()));
			return createNewEntity(repoRequest, incoming);
		}

		domainObjectMerger.merge(incoming.getContent(), domainObj);

		applicationContext.publishEvent(new BeforeSaveEvent(incoming.getContent()));
		Object obj = repoMethodInvoker.save(domainObj);
		applicationContext.publishEvent(new AfterSaveEvent(obj));

		if (config.isReturnBodyOnUpdate()) {
			PersistentEntityResource per = PersistentEntityResource.wrap(repoRequest.getPersistentEntity(),
																																	 obj,
																																	 repoRequest.getBaseUri());
			BeanWrapper wrapper = BeanWrapper.create(obj, conversionService);
			Link selfLink = entityLinks.linkForSingleResource(repoRequest.getPersistentEntity().getType(),
																												wrapper.getProperty(repoRequest.getPersistentEntity()
																																											 .getIdProperty()))
																 .withSelfRel();
			per.add(selfLink);
			return resourceResponse(null,
															per,
															HttpStatus.OK);
		} else {
			return resourceResponse(null,
															null,
															HttpStatus.NO_CONTENT);
		}
	}

	@SuppressWarnings({"unchecked"})
	@RequestMapping(
			value = "/{id}",
			method = RequestMethod.DELETE
	)
	@ResponseBody
	public ResponseEntity<?> deleteEntity(final RepositoryRestRequest repoRequest,
																				@PathVariable final String id)
			throws ResourceNotFoundException, HttpRequestMethodNotSupportedException {
		final RepositoryMethodInvoker repoMethodInvoker = repoRequest.getRepositoryMethodInvoker();
		if (null == repoMethodInvoker || (!repoMethodInvoker.hasFindOne()
				&& !(repoMethodInvoker.hasDeleteOne() || repoMethodInvoker.hasDeleteOneById()))) {
			throw new HttpRequestMethodNotSupportedException("DELETE");
		}
		ResourceMapping methodMapping = repoRequest.getRepositoryResourceMapping().getResourceMappingFor("delete");
		if (null != methodMapping && !methodMapping.isExported()) {
			throw new HttpRequestMethodNotSupportedException("DELETE");
		}

		final Object domainObj = domainClassConverter.convert(id,
																													STRING_TYPE,
																													TypeDescriptor.valueOf(repoRequest.getPersistentEntity()
																																														.getType()));
		if (null == domainObj) {
			throw new ResourceNotFoundException();
		}

		applicationContext.publishEvent(new BeforeDeleteEvent(domainObj));
		TransactionCallbackWithoutResult callback = new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				if (repoMethodInvoker.hasDeleteOneById()) {
					Class<? extends Serializable> idType = (Class<? extends Serializable>) repoRequest.getPersistentEntity()
																																														.getIdProperty()
																																														.getType();
					final Serializable idVal = conversionService.convert(id, idType);
					repoMethodInvoker.delete(idVal);
				} else if (repoMethodInvoker.hasDeleteOne()) {
					repoMethodInvoker.delete(domainObj);
				}
			}
		};
		if (null != txTmpl) {
			txTmpl.execute(callback);
		} else {
			callback.doInTransaction(null);
		}
		applicationContext.publishEvent(new AfterDeleteEvent(domainObj));

		return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
	}

}
