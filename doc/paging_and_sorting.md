# Paging and Sorting

_This documents Spring Data REST's usage of the Spring Data Repository paging and sorting abstractions. To familiarize yourself with those features, please see the Spring Data documentation for the Repository implementation you're using._

## Paging

Rather than return everything from a large result set, Spring Data REST recognizes some URL parameters that will influence the page size and starting page number.

To add paging support to your Repositories, you need to extend the `PagingAndSortingRepository<T,ID>` interface rather than the basic `CrudRepository<T,ID>` interface. This adds methods that accept a `Pageable` to control the number and page of results returned.

    public Page findAll(Pageable pageable);

If you extend `PagingAndSortingRepository<T,ID>` and access the list of all entities, you'll get links to the first 20 entities. To set the page size to any other number, add a `limit` parameter:

    http://localhost:8080/people/?limit=50

This will set the page size to 50.

To use paging in your own query methods, you need to change the method signature to accept an additional `Pageable` parameter and return a `Page` rather than a `List`. For example, the following query method will be exported to `/people/search/nameStartsWith` and will support paging:

    @RestResource(path = "nameStartsWith", rel = "nameStartsWith")
    public Page findByNameStartsWith(@Param("name") String name, Pageable p);

The Spring Data REST exporter will recognize the returned `Page` and give you the results in the body of the response, just as it would with a non-paged response, but additional links will be added to the resource to represent the "previous" and "next" pages of data.

### Previous and Next Links

Each paged response will return links to the previous and next pages of results based on the current page. If you are currently at the first page of results, however, no "previous" link will be rendered. The same is true for the last page of results: no "next" link will be rendered if you are on the last page of results. The "rel" value of the link will end with ".next" for next links and ".prev" for previous links.

    {
      "rel" : "people.next",
      "href" : "http://localhost:8080/people?page=2&limit=20"
    }

### Header metadata when paging

As a convenience to the user agent, Spring Data REST sets a few special HTTP headers when doing paging. To help the UA understand where it is within the entire set of available pages, three headers are set when returning paged responses:

    x-springdata-meta-total-count: 125
    x-springdata-meta-current-page: 1
    x-springdata-meta-total-pages: 7

The UA can use these values to keep track of where the paging stands in relation to the entire result set. This information is useful if you are providing a Javascript slider, for instance. You would be able to easily set the number of "notches" in the slider to the total number of pages and easily indicate to the user exactly where the current page of data falls in the context of the whole.

## Sorting

Spring Data REST also recognizes sorting parameters that will use the Repository sorting support.

To have your results sorted on a particular property, add a `sort` URL parameter with the name of the property you want to sort the results on. You can control the direction of the sort by specifying a URL parameter composed of the property name plus `.dir` and setting that value to either `asc` or `desc`. The following would use the `findByNameStartsWith` query method defined on the `PersonRepository` for all `Person` entities with names starting with the letter "K" and add sort data that orders the results on the `name` property in descending order:

    curl -v http://localhost:8080/people/search/nameStartsWith?name=K&sort=name&name.dir=desc

To sort the results by more than one property, keep adding as many `sort=PROPERTY` parameters as you need. They will be added to the `Pageable` in the order they appear in the query string.