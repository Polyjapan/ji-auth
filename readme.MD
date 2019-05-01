JI Auth
=======

It's just a slightly tweaked CAS-like protocol, really.

## Protocol

Because the CAS will be used on both public-facing SPA and internal standard/SP applications, we need to support two authentication modes.

An accepted drawback of this system will be that a user logging in from the "public" mode will not be able to access the "private" mode without logging in again (and vice versa). 

Note: apps are registered against the JI Auth Server with:
 - `appID`: an internal ID for the app
 - `clientID`: the public identification for the app (sent to clients) (could be the same as `appID`)
 - `clientSecret`: the private identification for the app. Kept by it, sent with some requests but **NEVER** to clients.
 - `emailCallback`: the URL where the user should be redirected when receiving the email confirmation (in public hidden mode)
 - `callback`: the URL where the user should be redirected when successfully logging in
 - `reCaptchaPkey`: the reCaptcha private key
 
### Common backend APIs

These APIs are intended to be accessed only by the backend of the apps, and not any user facing script.

##### `POST /api/app_ticket` 

Gets the information contained on a ticket and **deletes** it.

Consumes:
 - `ticket`: `string`, the ticket sent by the server
 - `clientId`: `string`, the `appID` of the app
 - `clientSecret`: `string`, the `appSecret` of the app
 
Returns:
 - `200 OK` if it worked
    - `type`: `SET(T_LOGIN, T_REGISTER, T_DOUBLE_REGISTER, T_EMAIL_CONFIRM, T_PASSWORD_RESET)`
    - `userId`: `int`, the ID of the user on the CAS server
    - `email`: `string`, the email of the user.
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 103: the clientSecret is wrong for this clientID
        - 201: the ticket is invalid, has expired, or is not allowed to be read by this app 
 
### "public" Hidden Mode (implemented and deployed)

This mode is supposed to be hidden from the user. All the endpoints accept and return only JSON. They are grouped under `/hidden`.
All these endpoints are called **by the browser of the client** (so that the apps never see the credentials of the user)
but from a form hosted on the app directly (for example, a single page app).

#### Demo

User wants to register against the public SPA (and, behind the scenes, on the CAS, without knoweledge of doing so) 

```
    Public SPA                 JI Auth Server               Public SPA backend
       
    (1) #--- POST /hidden/register ># (username, password, clientId)
                                    | Server registers users, stores him in DB
                                    | Server sends confirm email using the callback URL of the app
                                    | Server generates [app_ticket] (user, T_REGISTER) and stores it with the appID and 
                                    | an expiration date, set 24h in the future
    (2) #<-- OK, app_ticket --------#
        |                           
    (3) #--- POST /hidden/register --------------------------------># app_ticket, additional info required by the SPA
                                                                    | 
    (4)                             #<-- GET /api/get_user ---------# (app_ticket, clientSecret)
         Server checks app_ticket   |                  
         and clientSecret           |
    (5)                             #--- OK -----------------------># userId + email + T_REGISTER 
                                                                    | Checks that the type is T_REGISTER
                                                                    | Saves the acquired data in database
        #<---------------- Ok (no session) -------------------------#
          
```

User wants to login against the public SPA (and, behind the scenes, on the CAS, without knoweledge of doing so) 
```    
    Public SPA                 JI Auth Server               Public SPA backend
       
    (1) #--- POST /hidden/login ---># (username, password, clientId)
                                    | Server logs in user
                                    | Server does NOT create a session for the user
                                    | Server generates [app_ticket] (user, T_LOGIN) and stores it with the appID and an 
                                    | expiration date, set 24h in the future
    (2) #<-- OK, app_ticket --------#
        |                           
    (3) #--- POST /hidden/login {app_ticket}-----------------------># app_ticket
                                                                    | 
    (4)                             #<-- GET /api/get_user ---------# (app_ticket, clientSecret)
         Server checks app_ticket   |                  
         and clientSecret           |
    (5)                             #--- OK -----------------------># userId + email + T_LOGIN
                                                                    | Check ticket type is T_LOGIN 
                                                                    | Checks that the user is saved in local database
                                                                    
                                                                    If not :
                                                                    The user probably registered in an other service, 
                                                                    ask him the missing info
                                                                    
    6A  #<-- ERR_MISSING_INFO --------------------------------------# With signed temporary session {userId, email} 
        |
        | Users enters missing info
        |                                                                            
    7A  #--- POST /api/complete_profile ---------------------------># Saves the acquired data in database
                                                                    | 
                                                                 (to 8) 
                                                                        
                                                                    If yes: 
                                                                    Great, go to 8 immediately
                                                                    
                                                                    |
    (8) #<-- OK, signed session {user id and data} -----------------#

    Done, send signed session to all subsequent requests.
```

 User clicks on the email confirm link.
 
 
```      
      Client                        JI Auth
      
    (1) #- GET /hidden/email_confirm --># (confirmation key, client_id)
                                        | Server validates email
                                        | Server logs in user
                                        | Server does NOT create a session for the user
                                        | Server generates [app_ticket] (user, T_LOGIN) and stores it with the appID and an 
                                        | expiration date, set 24h in the future
    (2) #<-- OK, app_ticket ------------#

    (see login procedure, starting at 2)
```        

#### Endpoints

##### `POST /hidden/register` 

Registers the callee on the server.

Consumes:
 - `email`: `string`
 - `password`: `string`
 - `clientId`: the `appID` of the app
 - `captcha`: the re-captcha value (will be checked using the app's `reCaptchaPkey`)
 
Returns:
 - `200 OK` if it worked
    - `ticket`: the app ticket, a string
 - `400 BAD REQUEST` if not:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)
        - 104: the captcha is not correct

Notes:
 - If the user already exists:
    - If password is correct `ticket` will be a `T_LOGIN` ticket
    - If password is incorrect, `ticket` will be a `T_DOUBLE_REGISTER` ticket (usually ignored by recipient)

##### `POST /hidden/login` 

Logs in the callee on the system.

Consumes:
 - `email`: `string`
 - `password`: `string`
 - `clientId`: the `appID` of the app
 
Returns:
 - `200 OK` if it worked
    - `ticket`: the app ticket, a string
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)
        - 201: user or password incorrect
        - 202: email is not confirmed

##### `POST /hidden/email_confirm` 

Validates an email address.

Consumes:
 - `email`: `string`, the email of the user
 - `code`: `string`, the email confirmation code
 - `clientId`: `string`, the `appID` of the app
 
Returns:
 - `200 OK` if it worked
    - `ticket`: the app ticket, a string (type `T_EMAIL_CONFIRM`)
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 201: the email confirm code is invalid
        
##### `POST /hidden/forgot_password` 

Asks for a password reset for a client.

Consumes:
 - `email`: `string`
 - `clientId`: the `appID` of the app
 - `captcha`: the re-captcha value (will be checked using the app's `reCaptchaPkey`)
 
Returns:
 - `200 OK` if it worked
 - `400 BAD REQUEST` if not:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)
        - 104: the captcha is not correct

##### `POST /hidden/reset_password` 

Changes a clients' password, using a previously email sent code.

Consumes:
 - `email`: `string`, the email of the user
 - `code`: `string`, the password reset code
 - `clientId`: `string`, the `appID` of the app
 - `password`: `string`, the new password to set
 
Returns:
 - `200 OK` if it worked
    - `ticket`: the app ticket, a string (type `T_PASSWORD_RESET`)
 - `400 BAD REQUEST` if not, with additional info:
    - `errorCode`: int, in following set
        - 101: missing data in the request, or invalid request
        - 102: the clientID is not found (<=> no such app registered)        
        - 201: the user or the code is not found

### "private" Explicit Mode

This mode is closer to a standard CAS protocol. When accessing a protected app for the first time, the user logs in
against the CAS. Then, the CAS creates a session for the user, as well as a ticket (as before), and redirects him to the
client app. When accessing other apps, the CAS session will be used to redirect him immediately.

#### Demo


User wants to login against the app
```    
    Browser                  JI Auth Server                        App
       
    (1) #--- GET Access to protected resource --------------------->#
                                                                    | 
    (2) #<-- Redirect to authServer/login?app=app_id ---------------#
        |
        #--- GET login?app=app_id ->#
                                    | GET with no session, return form
        #<-- Login Form ------------# 
        |
        | User fills form
        |                            
    (3) #--- POST login?app=app_id >#
                                    | Login the user
                                    | Generate a CAS session
                                    | Generate [app_ticket] (user, T_EXPLICIT_GRANT) and stores it with the appID and an 
                                    | expiration date, set 24h in the future
    (4) #<-- Redirect* -------------#
        | * The redirect contains a CAS session
        |   User is redirected to app.callback_url?ticket={app_ticket}
        |                           
    (5) #--- GET callback_url?ticket={app_ticket}------------------># app_ticket
                                                                    | 
    (6)                             #<-- GET /api/get_user ---------# (app_ticket, clientSecret)
         Server checks app_ticket   |                  
         and clientSecret           |
    (7)                             #--- OK -----------------------># userId + email + T_EXPLICIT_GRANT
                                                                    | Check ticket type is T_EXPLICIT_GRANT 
                                                                    | Create session for user
    (8) #<-- Web page with session ---------------------------------# 
```

For the next apps, user will already hold a CAS session.


```    
    Browser                  JI Auth Server                        App
       
    (1) #--- GET Access to protected resource --------------------->#
                                                                    | 
    (2) #<-- Redirect to authServer/login?app=app_id ---------------#
        |
        #--- GET login?app=app_id ->#
                                    | GET with session, fast login
                                    | Generate [app_ticket] (user, T_EXPLICIT_GRANT) and stores it with the appID and an 
                                    | expiration date, set 24h in the future
    (4) #<-- Redirect* -------------#
        |   User is redirected to app.callback_url?ticket={app_ticket}
        |                           
    (5) #--- GET callback_url?ticket={app_ticket}------------------># app_ticket
                                                                    | 
    (6)                             #<-- GET /api/get_user ---------# (app_ticket, clientSecret)
         Server checks app_ticket   |                  
         and clientSecret           |
    (7)                             #--- OK -----------------------># userId + email + T_EXPLICIT_GRANT
                                                                    | Check ticket type is T_EXPLICIT_GRANT 
                                                                    | Create session for user
    (8) #<-- Web page with session ---------------------------------# 
```


#### Endpoints

No real endpoint here. Send the user to `auth_server_url/login?app={app client id}`. Once registered and logged in, the
user will be redirected to `your_app_callback_url?ticket={ticket}`. You can verify the ticket using
the endpoint described before.

You can also log the user out of the CAS by redirecting him to `auth_server_url/logout?app={app client id}`. He will
then be redirected to `your_app_callback_url?logout`.

### Manager mode

We will also have to provide a small management console, to allow some authorized persons to register their own app.

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