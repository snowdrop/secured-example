#!/usr/bin/env bash

# Exit script if you try to use an uninitialized variable.
set -o nounset

# Exit script if a statement returns a non-true return value.
set -o errexit

# Use the error status of the first failure, rather than that of the last item in a pipeline.
set -o pipefail

SCRIPT_ABSOLUTE_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)"
PROJECT_ABSOLUTE_DIR=$(dirname ${SCRIPT_ABSOLUTE_DIR})

pushd ${PROJECT_ABSOLUTE_DIR} > /dev/null

oc apply -f service.sso.yaml
./mvnw clean verify -Popenshift,openshift-it -DSSO_AUTH_SERVER_URL=$(oc get route secure-sso -o jsonpath='https://{.spec.host}/auth') "$@"

popd > /dev/null


