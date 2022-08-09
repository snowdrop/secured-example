#!/usr/bin/env bash
SOURCE_REPOSITORY_URL=${1:-https://github.com/snowdrop/secured-example}
SOURCE_REPOSITORY_REF=${2:-sb-2.5.x}
S2I_BUILDER_IMAGE_REPO=registry.access.redhat.com/ubi8/openjdk-11
S2I_BUILDER_IMAGE_TAG=1.14

source scripts/waitFor.sh

# Deploy SSO
oc apply -f .openshiftio/sso.yaml
if [[ $(waitFor "sso" "application") -eq 1 ]] ; then
  echo "SSO failed to deploy. Aborting"
  exit 1
fi
SSO_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth')

helm install secured ./helm --set spring-boot-example-app.s2i.source.repo=$SOURCE_REPOSITORY_URL --set spring-boot-example-app.s2i.source.ref=$SOURCE_REPOSITORY_REF --set spring-boot-example-app.s2i.env[0].name="MAVEN_ARGS_APPEND" --set spring-boot-example-app.s2i.env[0].value="-DSSO_AUTH_SERVER_URL=${SSO_URL}" --set spring-boot-example-app.s2i.builderImage.repo=$S2I_BUILDER_IMAGE_REPO --set spring-boot-example-app.s2i.builderImage.tag=$S2I_BUILDER_IMAGE_TAG
if [[ $(waitFor "rest-secured" "app") -eq 1 ]] ; then
  echo "Application failed to deploy. Aborting"
  exit 1
fi

# Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it -Dunmanaged-test=true -DSSO_AUTH_SERVER_URL=$SSO_URL
