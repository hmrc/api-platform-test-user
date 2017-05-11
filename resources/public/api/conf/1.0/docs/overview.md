The Create Test User API allows developers and testers to create test users 
in the API Developer Sandbox. These test users can then be used for testing 
[user-restricted endpoints](/api-documentation/docs/authorisation/user-restricted-endpoints#user-restricted).

Each test user is given:

* a unique user ID and password, which can be used to sign the user in as part 
  of the "grant authority" user journey
* a unique set of tax identifiers, which can be used as appropriate when calling
  the various APIs

This API can be used to create test users with a variety of service enrolments and the corresponding tax identifiers for individuals, organisations and agents. 