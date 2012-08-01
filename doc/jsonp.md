# JSONP Support in Spring Data REST

Spring Data REST supports [JSONP](http://en.wikipedia.org/wiki/JSONP) for doing safe cross-domain Ajax. JSONP support is integrated into the exporter so all you need to do to take advantage of it is pass the appropriate URL parameter. The default parameter is `callback`. So to get query method results wrapped with a call to your Javascript function, add `?callback=my_jsonp_callback` to the URL:

 		curl -v http://localhost:8080/people/search/findByName?name=John+Doe&callback=my_json_callback

Which will result in:

		HTTP/1.1 200 OK
		Content-Type: application/javascript
		Content-Length: ...

		my_jsonp_callback({
			"results": [ ... ],
			"_links": [ ... ]
		})

### Configuring the URL parameter

To configure what URL parameter is used, set the `jsonpParamName` property on your `org.springframework.data.rest.webmvc.RepositoryRestConfiguration` bean definition. In JavaConfig this would look like:

		@Bean public RepositoryRestConfiguration restConfig() {
			return new RepositoryRestConfiguration().
				setJsonpParamName("jsonp");
		}

This would mean the above URL would become:

 		curl -v http://localhost:8080/people/search/findByName?name=John+Doe&jsonp=my_json_callback

## JSONP-E Handling Errors

It's usually not possible to easily handle server errors with JSONP. This is because many JSONP frameworks use a script tag insertion to perform cross-domain Ajax. If the JSONP request results in an HTTP 400 Bad Request, for example, no javascript will be evaluated because the page is considered in error.

To deftly handle server errors using JSONP, you need to set a value on the `jsonpOnErrParamName` REST exporter configuration property (which is defaulted to `null`, which means don't handle errors). If this value is set, the exception handling code will look for a URL query string parameter of that name and use that javascript function to call as the error handler. The way it does this is by changing the HTTP status code from, for example 400, to 200 (OK). It then wraps the error message with a call to your javascript function and sends the original HTTP status code as the first parameter.

For example, if a call to POST a new entity results in a validation error, the server will return a 400 Bad Request. If the `jsonpOnErrParamName` is specified and you send that URL parameter, it will instead return a 200 and call your javascript function. Assuming I have `jsonpOnErrParamName` set to "errback", I would trigger this error handling like this:

		curl -v -d '...bad json data...' http://localhost:8080/people?errback=my_jsonp_error_handler

Which would result in:

		HTTP/1.1 200 OK
		Content-Type: application/javascript
		Content-Length: ...

		my_jsonp_error_handler(400, {
			"message": "Validation failed on property 'name'!",
			"cause": { ... }
		})