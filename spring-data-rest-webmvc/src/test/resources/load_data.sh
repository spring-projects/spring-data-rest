#!/bin/sh
curl -v -d '{"surname" : "Doe"}' -H "Content-Type: application/json" http://localhost:8080/family
curl -v -d '{"name" : "John Doe"}' -H "Content-Type: application/json" http://localhost:8080/people
curl -v -d '{"name" : "Jane Doe"}' -H "Content-Type: application/json" http://localhost:8080/people
curl -v -d 'http://localhost:8080/people/1
http://localhost:8080/people/2' -H "Content-Type: text/uri-list" http://localhost:8080/family/1/members
curl -v -d '{"postalCode":"12345","province":"MO","lines":["1 W 1st St."],"city":"Univille"}' -H "Content-Type: application/json" http://localhost:8080/address
curl -v -d "http://localhost:8080/address/1" -H "Content-Type: text/uri-list" http://localhost:8080/people/1/addresses
curl -v -d "http://localhost:8080/people/1" -X PUT -H "Content-Type: text/uri-list" http://localhost:8080/address/1/person
curl -v -d '{"postalCode":"54321","province":"MO","lines":["2 W 1st St."],"city":"Univille"}' -H "Content-Type: application/json" http://localhost:8080/address
curl -v -d "http://localhost:8080/address/2" -H "Content-Type: text/uri-list" http://localhost:8080/people/2/addresses
curl -v -d '{"type" : "twitter", "url": "#!/johndoe"}' -H "Content-Type: application/json" http://localhost:8080/profile
curl -v -d "http://localhost:8080/profile/1" -H "Content-Type: text/uri-list" http://localhost:8080/people/1/profiles
curl -v -d '{"type" : "facebook", "url": "/janedoe"}' -H "Content-Type: application/json" http://localhost:8080/profile
curl -v -d '{"links": [{"rel":"facebook", "href": "http://localhost:8080/profile/2"}]}' -H "Content-Type: application/json" http://localhost:8080/people/2/profiles