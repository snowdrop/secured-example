#!/usr/bin/env bash
SOURCE_REPOSITORY_URL=${1:-https://github.com/snowdrop/secured-example}
SOURCE_REPOSITORY_REF=${2:-sb-2.4.x}

source scripts/waitFor.sh

# Deploy SSO
oc apply -f .openshiftio/sso.yaml
if [[ $(waitFor "sso" "application") -eq 1 ]] ; then
  echo "SSO failed to deploy. Aborting"
  exit 1
fi
SSO_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth')

# Deploy app
oc create -f .openshiftio/application.yaml
oc new-app --template=rest-secured -p SOURCE_REPOSITORY_URL=$SOURCE_REPOSITORY_URL -p SOURCE_REPOSITORY_REF=$SOURCE_REPOSITORY_REF -p SSO_AUTH_SERVER_URL=$SSO_URL
if [[ $(waitFor "rest-secured" "app") -eq 1 ]] ; then
  echo "Application failed to deploy. Aborting"
  exit 1
fi

# Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it -Dunmanaged-test=true -DSSO_AUTH_SERVER_URL=$SSO_URL
