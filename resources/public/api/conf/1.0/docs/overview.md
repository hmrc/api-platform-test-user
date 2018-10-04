Use this API to create test users for [testing in our sandbox](/api-documentation/docs/testing) with [user-restricted endpoints](/api-documentation/docs/authorisation/user-restricted-endpoints#user-restricted).

###What is a test user?
A test user is a dummy Government Gateway account that you can use for testing in our sandbox.
To access [user-restricted endpoints](/api-documentation/docs/authorisation/user-restricted-endpoints) your application’s users must complete the OAuth journey, which includes signing into their Government Gateway account.

Each test user has a:

* type of Government Gateway account - individual, organisation or agent
* Government Gateway user ID and password - for sign in during the OAuth journey
* set of service enrolments and related tax identifiers - as different APIs require different enrolments

###What types of test user can I create?
With this API you can create a wide range of test users, including individuals, organisations and agents together with a wide variety of service enrolments and corresponding tax identifiers. You can also create test users for use during automated testing.

You can also use our [create a test user service](/api-test-user) to create test individuals or organisations with a simpler default set of enrolments.

###How long does a test user last?
Test users and other test data are [cleared down every two weeks](/api-documentation/docs/testing/data-cleardown). 