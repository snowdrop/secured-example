#!/usr/bin/env bash

source scripts/waitFor.sh

# Deploy SSO
oc apply -f .openshiftio/sso.yaml
if [[ $(waitFor "sso" "application") -eq 1 ]] ; then
  echo "SSO failed to deploy. Aborting"
  exit 1
fi
SSO_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth')

# Run Tests
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it -DSSO_AUTH_SERVER_URL=$SSO_URL
