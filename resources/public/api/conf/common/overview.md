Use this API to create test users for [testing in our sandbox](/api-documentation/docs/testing) with [user-restricted endpoints](/api-documentation/docs/authorisation/user-restricted-endpoints).

### What is a test user?
This is a temporary user ID you can use for testing in our sandbox environment.
To access [user-restricted endpoints](/api-documentation/docs/authorisation/user-restricted-endpoints) your application’s users must complete the OAuth journey, which includes signing in using their HMRC sign in details.

Each test user has a:

* type of HMRC user ID account - individual, organisation or agent
* HMRC user ID and password - for sign in during the OAuth journey
* set of service enrolments and related tax identifiers - as different APIs require different enrolments

### What types of test user can I create?
With this API you can create a wide range of test users, including individuals, organisations and agents together with a wide variety of service enrolments and corresponding tax identifiers. You can also create test users for use during automated testing.

You can also use our [create a test user service](/api-test-user) to create test individuals or organisations with a simpler default set of enrolments.

### How long does a test user last?
You can create multiple test users which will be generated in a default test state.

Test users can be reused, so we recommend checking if you have any unused test users before creating a new one.

Test users that have not been used in testing for 90 days will be deleted.