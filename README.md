# api-platform-test-user

[ ![Download](https://api.bintray.com/packages/hmrc/releases/api-platform-test-user/images/download.svg) ](https://bintray.com/hmrc/releases/api-platform-test-user/_latestVersion)

This is a published API on the API Platform.
It is the backend microservice for the creation of test users in the API Platform for the External Test Environment.
Test users and organisations are stored in mongo.

It exposes endpoints as documented on the [Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user).

### Running tests

Unit and integration tests can be run with the following bash script:

    ./run_all_tests.sh

Note that integration tests require a running version of Mongo 4.4, listening on localhost:27017. A simple way to achieve this
is to run with a docker image:

    docker run -p 27017:27017 --name mongo -d mongo:4.4

### Creating a local test user

To create a local test user using a browser frontend, check out [api-platform-test-user-frontend](https://github.com/hmrc/api-platform-test-user-frontend)
and follow its README.

To create a local test user without a frontend, first start the service by running `run_local.sh`.

A test individual can then be created with curl.
```
curl --location --request POST 'http://localhost:9617/individuals' \
--header 'Content-Type: application/json' \
--data-raw '{
  "serviceNames": [
    "national-insurance",
    "self-assessment",
    "mtd-income-tax",
    "customs-services",
    "goods-vehicle-movements",
    "mtd-vat",
    "common-transit-convention-traders"
  ],
  "eoriNumber": "GB123456789012"
}'
```

Test organisations and agents can be created similarly, using the respective endpoints in `conf/app.routes`.

### Adding fields

* In /app/uk/gov/hmrc/testuser/models/TestUser.scala
* update TestUserPropKey to have your new key.
* Use the new key inside of the generators.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

