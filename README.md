# Introduction

This project exposes a simple REST endpoint where the service `greeting` is available, but properly secured, at this address http://hostname:port/greeting
and returns a json Greeting message after the application issuing the call to the REST endpoint has been granted to access the service.

```json
{
    "content": "Hello, World!",
    "id": 1
}
```

The id of the message is incremented for each request. To customize the message, you can pass as parameter the name of the person that you want to send your greeting.

To manage the security, roles & permissions to access the service, a [Red Hat SSO](https://access.redhat.com/documentation/en/red-hat-single-sign-on/7.0/securing-applications-and-services-guide/securing-applications-and-services-guide) backend will be installed and configured for this project.
It relies on the Keycloak project which implements the OpenId connect specification which is an extension of the Oauth2 protocol.

After a successful login, the application will receive an `identity token` and an `access token`.
The identity token contains information about the user such as username, email, and other profile information.
The access token is digitally signed by the realm and contains access information (like user role mappings).

This `access token` is typically formatted as a JSON Token that the Spring Boot application will use with its Keycloak adapter to determine what resources it is allowed to access on the application.
The configuration of the adapter is defined within the `app/src/main/resources/application.properties` file using these properties:

```
keycloak.realm=REALM
keycloak.realm-key=PUBLIC_KEY
keycloak.auth-server-url=SSO_HOST
keycloak.ssl-required=external
keycloak.resource=CLIENT_APP
keycloak.credentials.secret=CLIENT_SECRET
keycloak.use-resource-role-mappings=false
```

The security context is managed by Red Hat SSO using a realm (defined using the keycloak.realm property) where the adapter to establish a trusted TLS connection will use the Realm Public key defined using the `keycloak.realm-key` property.
To access the server, the parameter `auth-server-url` is defined using the TLS address of the host followed with `/auth`.
To manage different clients or applications, a resource has been created for the realm using the property `keycloak.resource`.
This parameter, combined with the `keycloak.credentials.secret` property, will be used during the authentication phase to log in the application.
If, it has been successfully granted, then a token will be issued that the application will use for the subsequent calls.

The request that is issued to authenticate the application is:

```
https://<SSO_HOST>/auth/realms/<REALM>/protocol/openid-connect/token?client_secret=<SECRET>&grant_type=password&client_id=CLIENT_APP
```

And the HTTP requests accessing the endpoint/Service will include the following Bearer Token:

```
http://<SpringBoot_App>/greeting -H "Authorization:Bearer <ACCESS_TOKEN>"
```

The project is split into two Apache Maven modules - `app` and `sso`.
The `App` module exposes the REST Service using Spring Boot bundled with the Apache Tomcat 8.0.36 artifacts.
The `sso` module contains the OpenShift objects required to deploy the Red Hat SSO Server 7.0 along with the `app` module.

The goal of this project is to deploy the quickstart in an OpenShift environment (online, dedicated, ...).

# Prerequisites

To get started with these quickstarts you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.2.x 
[oc][3] | OpenShift Client | v3.3.x
[git][4] | Git version management | 2.x 

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

In order to build and deploy this project, you must have an account on an OpenShift Online (OSO): https://console.dev-preview-int.openshift.com/ instance.

# OpenShift Online

1. Using OpenShift Online or Dedicated, log on to the OpenShift Server.

    ```bash
    oc login https://<OPENSHIFT_ADDRESS> --token=MYTOKEN` when you use OpenShift Online or Dedicated.
    ```

1. Create a new project on OpenShift.

    ```bash
    oc new-project <some_project_name>` and next build the quickstart
    ```

1. Build the quickstart.

    ```
    mvn clean install -Popenshift
    ```

# Deploy the Application

1. To deploy the whole secured app, move to `sso` folder, and then use the Fabric8 Maven Plugin with the goals deploy and start:

    ```bash
    cd sso
    mvn fabric8:deploy -Popenshift
    ```

1. Open the OpenShift web console to see the status of the app and the exact routes used to access the app's greeting endpoint, or to access the Red Hat SSO's admin console.

    Note: until [CLOUD-1166](https://issues.jboss.org/browse/CLOUD-1166) is fixed,
    we need to fix the redirect-uri in RH-SSO admin console, to point to our app's route.

1. To specify the Red Hat SSO URL to be used by the Spring Boot application,
you must change the SSO_URL env variable assigned to the DeploymentConfig object.

    Note: You can retrieve the address of the SSO Server by issuing this command `oc get route/secure-sso` in a terminal and get the HOST/PORT name

    ```
    oc env dc/secured-springboot-rest SSO_URL=https://secure-sso-sso.e8ca.engint.openshiftapps.com
    ```

# Access the service

If the pod of the Secured Spring Boot application is running like the Red Hat SSO Server,
you can use one of the bash scripts proposed within the root of the project to access the service.

Depending which tool you prefer to use (curl or httpie),
use one of bash files available and pass as parameters the address of the Red Hat Secured SSO Server and the Secured Spring Boot application.

```
./scripts/httpie/token_req.sh https://secure-sso-sso.e8ca.engint.openshiftapps.com http://springboot-rest-sso.e8ca.engint.openshiftapps.com
./scripts/curl/token_req.sh https://secure-sso-sso.e8ca.engint.openshiftapps.com http://springboot-rest-sso.e8ca.engint.openshiftapps.com
```

The URLs of the Red Hat SSO and Spring Boot application are created according to this convention:

* Red Hat Secured SSO : <secured_sso_route>.<namespace>.<host_machine>
* Secured Spring Boot Application : <secured_springboot_route>.<namespace>.<host_machine>

You can find such routes using this oc client command `oc get routes` or the Openshift Console.

# Access the service using a user without admin role

1. To secure the Spring Boot REST endpoint, define the following properties in the `app/src/main/resources/application.properties` file, which contains the Keycloak parameters.

    ```
    keycloak.securityConstraints[0].securityCollections[0].name=admin stuff
    keycloak.securityConstraints[0].securityCollections[0].authRoles[0]=admin
    keycloak.securityConstraints[0].securityCollections[0].patterns[0]=/greeting
    ```
    The pattern’s property defines as pattern, the `/greeting` endpoint which means that this endpoint is protected by Keycloak.
    Every other endpoint that is not explicitly listed is NOT secured by Keycloak and is publicly available.
    The authRoles property defines which Keycloak roles are allowed to access the defined endpoints.
    Typically, the default admin user which is used as the admin role and will be able to access the service.

1. To verify that a user without the `admin` role cannot access the service, create a new user using the following bash script:

    ```
    ./scripts/curl/add_user.sh <SSO_HOST> <SpringBoot_HOST>
    ./scripts/httpie/add_user.sh <SSO_HOST> <SpringBoot_HOST>
    ```

1. Call the greeting endpoint by issuing a HTTP request, where the username is `bburke` and the password password.

    ```
    ./scripts/curl/token_user_req.sh <SSO_HOST> <SpringBoot_HOST>
    ./scripts/httpie/token_user_req.sh <SSO_HOST> <SpringBoot_HOST>
    ```

    You will be notified that you cannot access the service.

# Test the Quickstart Locally

(atm, we exclude / ignore tests, as they hit RH-SSO which we don't have running as part of the tests)

Install and start a Red Hat SSO server, using a parameter to access the SSO server.

```
mvn test -Dsso.url=http://localhost:8080 -Drealm=<realm> -Drealm.public.key=<public key> -Dclient.id=<client id> -Dsecret=<secret>
```