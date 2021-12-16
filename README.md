# Secured Spring Boot Example

## Table of Contents

* [Secured Spring Boot Example](#secured-spring-boot-example)
    * [How to play with the SSO Example locally](#how-to-play-with-the-sso-example-locally)
    * [How to play with the SSO Example on OpenShift](#how-to-play-with-the-sso-example-on-openshift)
    * [Running Tests on OpenShift using Dekorate](#running-tests-on-openshift-using-dekorate)
    * [Running Tests on OpenShift using S2i from Source](#running-tests-on-openshift-using-s2i-from-source)

## How to play with the SSO Example locally

- Deploy Keycloak on Openshift
```
oc new-project sso
oc create -f .openshiftio/sso.yaml
```

- Get Keycloak Auth Endpoint
```
SSO_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth')
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

https://appdev.openshift.io/docs/spring-boot-runtime.html#mission-secured-spring-boot

## How to run the SSO Example on OpenShift

- Deploy Keycloak on Openshift.
```
oc new-project sso
oc create -f .openshiftio/sso.yaml
```

Obtain the `SSO_URL`

```shell
SSO_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth')
```

- Build and deploy the Spring Boot application using Dekorate.
```
mvn clean verify -Popenshift -Ddekorate.deploy=true -DSSO_AUTH_SERVER_URL=${SSO_URL}
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
