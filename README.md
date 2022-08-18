# Secured Spring Boot Example

https://appdev.openshift.io/docs/spring-boot-runtime.html#mission-secured-spring-boot

## Table of Contents

* [Secured Spring Boot Example](#secured-spring-boot-example)
    * [Prerequisites](#prerequisites)
    * [How to play with the SSO Example locally](#how-to-play-with-the-sso-example-locally)
    * [How to run the SSO Example on OpenShift](#how-to-run-the-sso-example-on-openshift)
    * [Deploying application on OpenShift using Helm](#deploying-application-on-openshift-using-helm)
    * [Running Tests on OpenShift using Dekorate](#running-tests-on-openshift-using-dekorate)
    * [Running Tests on OpenShift using S2i from Source](#running-tests-on-openshift-using-s2i-from-source)
    * [Running Tests on OpenShift using Helm](#running-tests-on-openshift-using-helm)

## Prerequisites

- JDK 11+ installed with JAVA_HOME configured appropriately

## How to play with the SSO Example locally

- Deploy Keycloak on Openshift
```
oc new-project sso
oc create -f .openshiftio/sso.yaml
```

- Get Keycloak Auth Endpoint
```
SSO_URL=$(oc get route sso -o jsonpath='http://{.spec.host}/auth')
```

- Start the application. Provide the Keycloak URL in a `SSO_AUTH_SERVER_URL` parameter. 
```
mvn spring-boot:run -DSSO_AUTH_SERVER_URL=${SSO_URL}
```

- Use curl to access the endpoint without authentication (you will receive an unauthorized error)
```
curl -v http://localhost:8080/api/greeting
```

- Open the application webpage in your browser `http://localhost:8080` and log in with one of the user credentials.

| Username | Password | Expected status code |
| -------- | -------- | -------------------- |
| alice    | password | 200                  |
| admin    | admin    | 403                  |

User `alice` is recognised by the system and has permission to access the greeting service.
`admin` on the other hand, is recognised by the system but cannot access the greeting service.

- Try accessing the greeting service by using the form or the `curl` command displayed in the webpage.

## How to run the SSO Example on OpenShift

- Deploy Keycloak on Openshift.
```
oc new-project sso
oc create -f .openshiftio/sso.yaml
```

Obtain the `SSO_URL`

```shell
SSO_URL=$(oc get route sso -o jsonpath='http://{.spec.host}/auth')
```

- Build and deploy the Spring Boot application using Dekorate.
```
mvn clean verify -Popenshift -Ddekorate.deploy=true -DSSO_AUTH_SERVER_URL=${SSO_URL}
```

## Deploying application on OpenShift using Helm

First, make sure you have installed [the Helm command line](https://helm.sh/docs/intro/install/) and connected/logged to a kubernetes cluster.

Now, deploy Keycloak on Openshift.
```
oc create -f .openshiftio/sso.yaml
```

And obtain the `SSO_URL`:

```shell
SSO_URL=$(oc get route sso -o jsonpath='http://{.spec.host}/auth')
```

Then, you need to install the example by doing:

```
helm install secured ./helm --set spring-boot-example-app.s2i.source.repo=https://github.com/snowdrop/secured-example --set spring-boot-example-app.s2i.source.ref=<branch-to-use> --set spring-boot-example-app.s2i.env[0].name="MAVEN_ARGS_APPEND" --set spring-boot-example-app.s2i.env[0].value="-DSSO_AUTH_SERVER_URL=${SSO_URL}"
```

**note**: Replace `<branch-to-use>` with one branch from `https://github.com/snowdrop/secured-example/branches/all`.

And to uninstall the chart, execute:

```
helm uninstall secured
```

## Running Tests on OpenShift using Dekorate

```
./run_tests_with_dekorate.sh
```

## Running Tests on OpenShift using S2i from Source

```
./run_tests_with_s2i.sh
```

This script can take 2 parameters referring to the repository and the branch to use to source the images from.

```bash
./run_tests_with_s2i.sh "https://github.com/snowdrop/secured-example" branch-to-test
```

## Running Tests on OpenShift using Helm

```
./run_tests_with_helm.sh
```

This script can take 2 parameters referring to the repository and the branch to use to source the images from.

```bash
./run_tests_with_helm.sh "https://github.com/snowdrop/secured-example" branch-to-test
```
