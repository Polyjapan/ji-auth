JI Auth
=======

A homemade identity provider and SSO portal for Japan Impact!

## Compile

To get a universal zip, run `sbt dist`.

To get a docker container (prefered), run `sbt docker:publishLocal`. The tag for the container depends on the version
set in `build.sbt`.

This build contains three artifacts:
 - The API (`ch.japanimpact.jiauthframework`): a Scala API to use the framework
 - The Tools: a small application to generate test API tokens using a private key
 - The Server: JI Auth itself, the CAS+SAMLv2 IdP. 

Run `sbt boot` to start development environment (in which case the server will start on port 9000). You can provide a specific configuration file using the `-Dconfig.file=path` switch.

## Client

The scala client is located in the API folder. 

## Endpoints

JIAuth supports protocols CAS and SAML. Here are the endpoints to use:

 - SAMLv2:
    - Metadata discovery/entity ID: `https://auth.japan-impact.ch/SAML2/SSO/Metadata`
    - Redirect binding: `https://auth.japan-impact.ch/SAML2/SSO/Redirect`
    - POST binding: `https://auth.japan-impact.ch/SAML2/SSO/POST`
 - CAS:
    - Login page for the user: `https://auth.japan-impact.ch/cas/login?service=<your service url>`
    - V1 validation endpoint: `https://auth.japan-impact.ch/cas/validate?service=<your service>&ticket=<the ticket>`
    - V2/V3 service validation: `https://auth.japan-impact.ch/cas/serviceValidate?...`
    - V2/V3 service+proxy validation: `https://auth.japan-impact.ch/cas/proxyValidate?...`
    - V2/V3 proxy validation: `https://auth.japan-impact.ch/cas/proxy`
    - V3 saml validation: `https://auth.japan-impact.ch/cas/samlValidate`
    
SAMLv1.1 is "supported-ish" via the CAS v3 SAML validation. Use at your own risks.

All v2/v3 endpoints (except SAML validation) take an optional `format` GET parameter, by default set to `XML`. 
If set to `json`, the data returned by the endpoint will be in JSON instead of XML.
    
CAS endpoints are available on multiple places. 
 - V1 endpoints are available under `https://auth.japan-impact.ch/cas/<endpoint>` and `https://auth.japan-impact.ch/cas/v1/<endpoint>`
 - V2-3 endpoints are available under `https://auth.japan-impact.ch/cas/<endpoint>` and `https://auth.japan-impact.ch/cas/v2/<endpoint>`
 - V3 endpoints are available under `https://auth.japan-impact.ch/cas/<endpoint>` and `https://auth.japan-impact.ch/cas/p3/<endpoint>`

## Old informations (probably not up to date)

## API authentication

Any API that accepts API tokens proceeds them in the same way.

To access these APIs MUST pass the token in the `Authorization` header, as follows: `Authorization: Bearer ${ACCESS_TOKEN}`.

The API will reply with the following standard error codes:
 - `401` if the ACCESS_TOKEN is not valid (you may want to refresh it)
 - `403` if the ACCESS_TOKEN is valid but doesn't allow access to the resource.

As a responsible developer, we recommend that all your APIs directly accept `ACCESS_TOKEN` in the way described before.

## Common API datatypes

- `UserProfile`: the profile of an user
    - `id`: (`number`) the unique id of that user
    - `email`: (`string`) the email address of that user
    - `details`: (`UserDetails`)
    - `address`: (`optional UserAddress`)
    
- `UserDetails`: the details about an user
    - `firstName`: (`string`)
    - `lastName`: (`string`)
    - `phoneNumber`: (`optional string`)

- `UserAddress`: the address of an user. Mostly raw data, not sanitized.
    - `address`: (`string`)
    - `addressComplement`: (`optional string`)
    - `postCode`: (`string`) it's a string because of the variety of formats in the world
    - `city`: (`city`)
    - `country`: (`string`)    
    
## Public Backend APIs

The following endpoints don't require your `Client-Secret`. They can therefore be called by any client, even a single page app or android app.

### `GET /api/refresh/:refreshToken`

Get a fresh new access token.

- Parameters:
    - `refreshToken` (string): a valid refresh token
- Return type: (`json` object)
    - `accesToken`: (`string`) the new valid access token
    - `refreshToken`: (`string`) the refresh token (in the future, it may be different from the one you sent. You should always update your refresh token.)
    - `duration`: (`number`) the duration (in second) during which the `accessToken` is valid
- Errors:
    - `401`: the refresh token is not valid
    
### `GET /api/user/`

This endpoind requires an `ACCESS_TOKEN`. See the section on "API Authentication" for more details.
    
- Return type: `UserProfile`, the user profile of the token bearer
- Errors:
    - `401`: the access token is not valid or expired


## Common backend APIs

These APIs are intended to be accessed only by the backend of the apps, and not any user facing script.

The endpoints are protected and require the following header to work:
 - `X-Client-Secret` for the app client secret

To get a client secret, you need to register a new app on the server.
 
If the headers are missing, the endpoints return:

 - `401 UNAUTHENTICATED` with json
    - `errorCode` = `101` (Missing Data)
    
Random idea: an endpoint that takes a list of emails, and returns the ids of the accounts (allows easier migrations)

#### `GET /api/user/:userId`

Gets the profile of an arbitrary user

- Parameters:
    - `userId` (number): the id of the user you want to query
- Return type: `UserProfile`, the user profile you queried
- Errors
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 103: the clientSecret is wrong for this clientID
        - 201: user not found

#### `GET /api/users/:userIds`

Gets the profile of up to 100 users at once.

Note that not found users are removed silently.

- Parameters:
    - `userIds` (CSV numbers): the ids of the users you want to query
- Return type: `Map[UserId (string), UserProfile]`, the found user profiles
- Errors
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 103: the clientSecret is wrong for this clientID

#### `POST /api/groups/<group>/members`

Adds a member to a group. The app owner needs to have a write permission on that group.

Consumes:
 - `userId`: `int`, the user to add to the group
 
Returns:
 - `200 OK` if it worked
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 103: the clientSecret is wrong for this clientID
        - 201: the group was not found (doesn't exist, or can't be accessed by this app)
        - 202: the user was not found 
        - 203: the app cannot add users to the group (app owner doesn't have write permission) 
        

#### `DELETE /api/groups/<group>/members/<userId>`

Removes <userId> from the group <group>. The app owner needs write permission on the group. If the user is an admin of the group,
the app owner needs to be admin of the group as well. This endpoint cannot remove the owner of the group.
 
Returns:
 - `200 OK` if it worked
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 103: the clientSecret is wrong for this clientID
        - 201: the group was not found (doesn't exist, or can't be accessed by this app)
        - 202: the user was not found or not a member of the group
        - 203: the app cannot remove this user (no write permission OR trying to remove an admin without admin permission OR trying to remove owner)
        

 
#### `GET /api/ticket/<ticket>` (deprecated)

Gets the information contained on a ticket and **deletes** it.

Consumes:
 - `ticket`: `string`, the ticket sent by the server
 
Returns:
 - `200 OK` if it worked
    - `type`: `SET(T_LOGIN, T_EXPLICIT_GRANT, T_REGISTER, T_DOUBLE_REGISTER, T_EMAIL_CONFIRM, T_PASSWORD_RESET, T_APP)`
    - `userId`: `int`, the ID of the user on the CAS server
    - `email`: `string`, the email of the user.
    - `groups`: `array(string)`, the visible groups this user is part of. A group is visible if the app owner has a read permission on that group.
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 103: the clientSecret is wrong for this clientID
        - 201: the ticket is invalid, has expired, or is not allowed to be read by this app 


#### `GET /api/app_login/<other client ID>` (deprecated)

Generates a ticket for the app to use against the app identified by the provided "other client ID".

The ticket's content will be filled from the owner of the app calling this endpoint.

This allows easy inter-apps communication, as apps only have to login like regular users.

Returns:
 - `200 OK` if it worked
    - `ticket`: `string`, a ticket of type `T_APP` to use on the app
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the other client id is not found (<=> no such app registered)        
        - 103: the clientSecret is wrong for this clientID

## CAS support

JI Auth supports CAS v1, v2 and v3. The implementation is sparse. Here are the main endpoints:

- `/cas/login?service=service_url` to log in. The domain of `service_url` must be registered as a cas_service
- `/cas/v1/validate`: the standard CAS v1 validation endpoint
- `/cas/v2/proxyValidate` and `/cas/v2/serviceValidate`: the sparsely implemented serviceValidate CAS v2/v3 endpoint.
- `/cas/v2/proxy` to get a proxy ticket

### Proxy authentication

CAS defines *Proxy Authentication*, a way for a backend service to request user data from CAS.

Terms:
 - CAS: the authentication server, here JI Auth (at `https://auth.japan-impact.ch/`)
 - Backend: a CAS service to which no user is ever redirected (at `https://frontend.app/`)
 - Frontend: a CAS service on which users are redirected (at `https://backend.app/`)
 - User: an example user, that hsa an account on our CAS
 
The idea of proxy authentication is to allow Backend to authentify User.

The flow is the following:
 1. User goes to Frontend
 2. Frontend redirects to CAS
 3. User logs in on CAS and is redirected to Frontend callback
 4. Frontend requests a `PGT` by providing its own pgCallback url
    1. CAS makes a request to pgCallback, providing `pgtId` and `pgtIou` as GET parameters
    2. CAS replies to the request by giving back the `pgtIou`
    3. Frontend can then guess the `pgtId`, as it received a request to pgCallback
 5. Frontend requests a `PT` by calling the endpoint and giving the `pgtId` token
 6. Frontend sends the `PT` to backend
 7. Backend requests the user data associated with the `PT` to CAS
 
Details, with endpoints:
 1. _nothing to add_
 2. `https://auth.japan-impact.ch/cas/login?service=https://frontend.app/callback`
 3. Redirected to `https://frontend.app/callback?ticket=<serviceTicket>`
 4. Request to `https://auth.japan-impact.ch/cas/v2/serviceValidate?service=frontend.app&ticket=<serviceTicket>&pgtUrl=https://frontend.app/pgCallback`.
    1. CAS requests `https://frontend.app/pgCallback?pgtId=<pgtId>&pgtIou=<pgtIou>` (pgtId is the PGT: Proxy Granting Ticket). Frontend remembers the mapping `pgtIou -> pgtId` (only required for a short time)
    2. CAS replies a standard XML https://apereo.github.io/cas/6.0.x/protocol/CAS-Protocol-Specification.html#262-response with `proxyGrantingTicket` set to `pgtIou`
    3. Frontend uses the mapping to get the `pgtId` (PGT)
 5. Request to `https://auth.japan-impact.ch/cas/v2/proxy?service=backend.app&pgt=<pgtId>`, reply with standard XML https://apereo.github.io/cas/6.0.x/protocol/CAS-Protocol-Specification.html#272-response containing `proxyTicket` (PT)
 6. Send to backend the PT (for example `https://backend.app/login?ticket=<pt>`, but doesn't even require HTTP(s))
 7. Backend requests `https://auth.japan-impact.ch/cas/v2/proxyValidate?service=backend.app&ticket=<pt>`
 
Notes:
 - the `service` in steps (5) and (7) needs to be the same. i.e. a PT can only be used for the service to which it was issued.
 - the PT is only usable ONCE. If you need to use it multiple times, repeat steps 5-7
 - the serviceTicket is only usable ONCE. 
 - the response at 4.ii also includes proper user data, enough to log the user in
 - JI Auth implements a security: a service A can generate a PT for a service B iff B allows A to proxy access to it.



## Groups

We will also add a group system.
A group has an `id`, a unique `name`, a `display_name` and an `owner_id`.

Users can be members of groups, via a (`user_id`, `group_id`) relationship.

This relationship has several boolean flags:
- `can_manage_members`: if true, this member can add and remove members from the group
- `can_read_members`: if true, this member and its apps can read the members of the group
- `is_admin`: if true, this member can change the flags of all members except the owner

The owner can change the owner, and is always member with all three flags set.

Groups will be returned in the ticket as a list: 

    groups: [group_1_name, group_2_name, ...]
    
This list will only be a subset of all the groups the user is in. This subset will **only** contain the groups for which
the `app_owner` is a member with `can_read_members` flag set on.
    
An endpoint will be added that will allow to get the information on a group

    GET /api/group/<group name>
    
    Returns:
    
    {
        id: number,
        name: string,
        display_name: string
    }
    
This endpoint requires no authentication. It is only provided to allow third party apps to display a pretty name instead
of a raw group name.