JIAuthFramework
===============

A light API to use the JI Auth service.

## How to use?

You need to compile and distribute it locally to be able to use it. It's a simple sbt command:

    sbt publishLocal
   
The methods are pretty easy to use.

## Use in a play application

### CAS Authentication

The CAS bindings are in `ch.japanimpact.auch.api.cas.CASService`. 

You need to add the following section in your configuration:
    
    cas {
        hostname = "auth.japan-impact.ch" // the hostname (and port) of the CAS server
        uri = "/cas/v2" // the base path of CAS on the given hostname
        service = <define here> // the hostname of the current service
        secure = true // set to false to disable use of HTTPS
    }
    
If you wish to use **proxy CAS**, you also need to define a route for proxy callback. 

In `routes` add:

    GET   /pgCallback   ch.japanimpact.auch.api.cas.ProxyController.proxyCallback(pgtId: String, pgtIou: String)

(the `pgCallback` path is provided as an example, you can use the path you like)

### Auth API

AuthAPI provides binding to the Authentication API (to get user info, for example).

First, you'll need a method to provide the authClient in your application module. Similar for other modules.
    
    @Provides
    def provideAuthClient(ws: WSClient)(implicit ec: ExecutionContext, config: Configuration): AuthApi = AuthApi(ws)
   
    
This version reads values from the config file

    jiauth {
      clientId = ""
      clientSecret = ""
      baseUrl = ""
    }

You can also provide these values directly
    
    @Provides
    def provideAuthClient(ws: WSClient)(implicit ec: ExecutionContext, config: Configuration): AuthApi = {
        new AuthApi(ws, appBaseUrl, clientId, clientSecret)
    }
    
You can then inject the `AuthApi` in your components or service to use it. Refer to the documentation of the class for more info.

The usage is pretty straightforward and well documented, see `AuthApi.scala`.


## Use in a PHP application

Just require the class and create an instance of it.

    require 'JIAuth.class.php';
    
    $baseUrl = "https://auth.japan-impact.ch/";
    $clientId = "client_id";
    $clientSecret = "client_secret";
    
    $auth = new JIAuth($baseUrl, $clientId, $clientSecret);
    