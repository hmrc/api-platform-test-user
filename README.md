# api-platform-test-user

[ ![Download](https://api.bintray.com/packages/hmrc/releases/api-platform-test-user/images/download.svg) ](https://bintray.com/hmrc/releases/api-platform-test-user/_latestVersion)

This is the backend microservice for the creation of test users in the API Platform for the External Test Environment.
Test users and organisations are stored in mongo.

It exposes endpoints as documented on the [Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user).

### Running tests

Unit and integration tests can be run with the following bash script:

    ./run_all_tests.sh

Note that integration tests require a running version of Mongo 3.2, listening on localhost:27017. A simple way to achieve this
is to run with a docker image:

    docker run -p 27017:27017 --name mongo -d mongo:3.2

### Creating a local test user

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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

