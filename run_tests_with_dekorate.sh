#!/usr/bin/env bash

# Deploy SSO
oc apply -f .openshiftio/sso.yaml
sleep 30 # needed in order to bypass the 'Pending' state
timeout 300s bash -c 'while [[ $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"deploy\"))  | .status  " | jq -rs "sort_by(.startTme) | last | .phase") == "Running" ]]; do sleep 20; done; echo ""'
oc logs $(oc get pod -o json | jq  ".items[] | select(.metadata.name | contains(\"sso\"))  | .metadata  " | jq -rs "sort_by(.startTme) | last | .name")

# Run OpenShift Tests
./mvnw -s .github/mvn-settings.xml clean verify -Popenshift,openshift-it
