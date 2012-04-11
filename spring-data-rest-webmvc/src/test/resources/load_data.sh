#!/bin/sh
curl -d '{"name" : "John Doe"}' -H "Content-Type: application/json" http://localhost:8080/person
curl -d '{"name" : "Jane Doe"}' -H "Content-Type: application/json" http://localhost:8080/person
curl -d '{"postalCode":"12345","province":"MO","lines":["1 W 1st St."],"city":"Univille"}' -H "Content-Type: application/json" http://localhost:8080/address
curl -d "http://localhost:8080/address/1" -H "Content-Type: text/uri-list" http://localhost:8080/person/1/addresses
curl -d '{"postalCode":"54321","province":"MO","lines":["2 W 1st St."],"city":"Univille"}' -H "Content-Type: application/json" http://localhost:8080/address
curl -d "http://localhost:8080/address/2" -H "Content-Type: text/uri-list" http://localhost:8080/person/2/addresses
curl -d '{"type" : "twitter", "url": "#!/johndoe"}' -H "Content-Type: application/json" http://localhost:8080/profile
curl -d "http://localhost:8080/profile/1" -H "Content-Type: text/uri-list" http://localhost:8080/person/1/profiles
curl -d '{"type" : "facebook", "url": "/janedoe"}' -H "Content-Type: application/json" http://localhost:8080/profile
curl -d '{"_links": [{"rel":"facebook", "href": "http://localhost:8080/profile/2"}]}' -H "Content-Type: application/json" http://localhost:8080/person/2/profiles
