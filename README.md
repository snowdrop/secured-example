# Introduction

This project exposes a simple REST endpoint where the service `greeting` is available, but properly secured, at this address `http://hostname:port/greeting` and returns a json Greeting message after authentication

```
{
    "content": "Hello, World!",
    "id": 1
}

```

The id of the message is incremented for each request. To customize the message, you can pass as parameter the name of the person that you want to send your greeting.

# Build

The project is spilt into two modules - app and build.
App module exposes simple SpringBoot REST endpoint. For this it bundles the Apache Tomcat 8.0.36 artifacts with SpringBoot 1.4.1.RELEASE.
Where build module contains OpenShift resources required to deploy RH-SSO along with the "app" module.

To build the project, use this maven command.

```
mvn fabric8:resource fabric8:build package -DskipTests
```
(atm, we exclude / ignore tests, as they hit RH-SSO which we don't have running as part of the tests)

# Test

To test locally the quickstart, install & start a Red Hat SSO server. Next, pass as parameter the URL to access the SSO Server 

```
mvn test -Dsso.url=http://localhost:8080
```

# Launch / deploy

The goal is to launch this quickstart against a running OpenShift environment.
The easiest way to do this is to create an account on OpenShift Online (OSO): https://console.dev-preview-stg.openshift.com/
(or you're welcome to setup your own OpenShift env; via minishift, etc)
Once you have this, along with OpenShift CLI tools, you're ready to go.

Create a new project on OpenShift: oc new-project <some_project_name>.

To deploy the whole secured app, first move to build/ dir, and then simply use Fabric8 run:

```
cd build
mvn fabric8:run
```

Open OpenShift console in the browser to see the status of the app,
and the exact routes, to be used to access the app's greeting endpoint.
(or to access RH-SSO's admin console)

Note: until https://issues.jboss.org/browse/CLOUD-1166 is fixed,
we need to fix the redirect-uri in RH-SSO admin console, to point to our app's route.

Ctrl-C is to exit the deploy (it sets repl. controllers to zero).
Where you do a full cleanup with "mvn fabric8:undeploy".


If the pod of the Secured Spring Boot Application is running like also the Red Hat SSO Server, you 
can use one of the bash scripts proposed within the QuickStart to access the service.

Depending which tool you prefer to use (curl or httpie), use one of them and pass as parameters
the address of the Red Hat Secured SSO Server and the Secured Spring Boot Application. 

```
./httpie_token_req.sh https://secure-sso-sso.e8ca.engint.openshiftapps.com http://springboot-rest-sso.e8ca.engint.openshiftapps.com
```

The URLs of the Red Hat SSO & Spring Boot Application are created according to this convention:

* Red Hat Secured SSO : <secured_sso_route>.<namespace>.<host_machine>
* Secured Spring Boot Application : <secured_springboot_route>.<namespace>.<host_machine>

You can find such routes using a oc client command `oc get routes` or the Openshift Console.