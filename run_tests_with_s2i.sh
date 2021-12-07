#!/usr/bin/env bash
SOURCE_REPOSITORY_URL=${1:-https://github.com/snowdrop/secured-example}
SOURCE_REPOSITORY_REF=${2:-sb-2.4.x}

# Deploy SSO
oc apply -f .openshiftio/sso.yaml
sleep 30 # needed in order to bypass the 'Pending' state
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"deploy\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'
oc logs $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"sso\"))  | .metadata  " | jq -rs "sort_by(.startTme) | last | .name")

# Deploy app
oc create -f .openshiftio/application.yaml
oc new-app --template=rest-secured -p SOURCE_REPOSITORY_URL=$SOURCE_REPOSITORY_URL -p SOURCE_REPOSITORY_REF=$SOURCE_REPOSITORY_REF
sleep 30 # needed in order to bypass the 'Pending' state
# wait for the app to stand up
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"build\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'

# Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it -Dunmanaged-test=true
