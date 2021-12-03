# How to play with the SSO Example locally

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

# How to play with the SSO Example on OpenShift

- Deploy Keycloak on Openshift.
```
oc new-project sso
oc create -f .openshiftio/sso.yaml
```

- Build and deploy the Spring Boot application using Dekorate.
```
mvn clean verify -Popenshift -Ddekorate.deploy=true
```

- Run Integration Tests.
```
oc delete project sso --ignore-not-found=true
oc new-project sso
oc apply -f .openshiftio/sso.yaml
sh run_tests_with_dekorate.sh
```

# Running Tests on OpenShift using S2i from Source:

```
oc delete project sso --ignore-not-found=true
oc new-project sso
oc create -f .openshiftio/application.yaml
oc new-app --template=rest-http -p SOURCE_REPOSITORY_URL="https://github.com/snowdrop/secured-example" -p SOURCE_REPOSITORY_REF=sb-2.4.x

sleep 30 # needed in order to bypass the 'Pending' state
# wait for the app to stand up
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"build\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'

# launch the tests without deploying the application
sh run_tests_with_s2i.sh
```
