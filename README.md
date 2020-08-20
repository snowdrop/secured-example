[![CircleCI](https://circleci.com/gh/snowdrop/secured-example.svg?style=shield)](https://circleci.com/gh/snowdrop/secured-example)

# How to play with the SSO Example locally

NOTE: `service.sso.yaml` and `.openshiftio/service.sso.yaml` must be always kept in sync.

- Deploy Keycloak on Openshift
```
oc new-project sso
oc create -f service.sso.yaml
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
