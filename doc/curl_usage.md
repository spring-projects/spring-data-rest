# Example API usage with curl

Here is some example usage of the REST API with `curl`. First we'll add a `Family`:

    $ curl -v -d '{"surname" : "Doe"}' -H "Content-Type: application/json" http://localhost:8080/family

    HTTP/1.1 201 Created
    Location: http://localhost:8080/family/1
    Content-Length: 0

Now we'll add a `Person`:

    $ curl -v -d '{"name" : "John Doe"}' -H "Content-Type: application/json" http://localhost:8080/people

    HTTP/1.1 201 Created
    Location: http://localhost:8080/people/1
    Content-Length: 0

Now we'll add this person to the "Doe" family we added above:

    $ curl -v -d 'http://localhost:8080/people/1' -H "Content-Type: text/uri-list" http://localhost:8080/family/1/members

    HTTP/1.1 201 Created
    Content-Length: 0

Notice that we don't return a `Location` when we add items to a referenced collection because we can add N numbers of items (here we're just adding one) so the `Location` header wouldn't be very meaningful as you couldn't match which URL you POSTed with the corresponding URL in the header.

Now that we have some links created, let's query them so our user agent can keep track of them:

    $ curl -v http://localhost:8080/family/1/members

    HTTP/1.1 200 OK
    Content-Type: application/json;charset=ISO-8859-1
    Content-Length: 118

    {
      "_links" : [ {
        "rel" : "family.Family.Person.1",
        "href" : "http://localhost:8080/family/1/members/1"
      } ]
    }

We can continue adding other top-level entities by sending JSON data and can add links to referenced entities by sending `text/uri-list` data with the URIs to those other top-level entities.
