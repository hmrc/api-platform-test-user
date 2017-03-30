The Create Test User API allows developers and testers to create test users 
in the API Developer Sandbox. These test users can then be used for testing 
[user-restricted endpoints](/api-documentation/docs/authorisation#user-restricted).

Each test user is given:

* a unique user ID and password, which can be used to sign the user in as part 
  of the "grant authority" user journey
* a unique set of tax identifiers, which can be used as appropriate when calling
  the various APIs

Currently, this API can be used to create individual and organisation test users 
with a default set of service enrolments and associated tax identifiers, as per 
the [Create Test User online service](/api-test-user). In the future it will be 
possible to create test users with specific enrolments. It will also be possible
to create agent test users.