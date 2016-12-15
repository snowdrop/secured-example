#!/usr/bin/env bash

SCRIPT_DIR="$(dirname "$0")"

. $SCRIPT_DIR/../common.sh

auth_result=$(http --verify=no -f $SSO_HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD client_secret=$SECRET grant_type=password client_id=$CLIENT_ID)
access_token=$(echo -e "$auth_result" | awk -F"," '{print $1}' | awk -F":" '{print $2}' | sed s/\"//g | tr -d ' ')

echo ">>> Add Realm"
http --verify=no --verbose POST $SSO_HOST/auth/admin/realms "Authorization: Bearer $access_token" < $SCRIPT_DIR/../realm.json

#echo ">>> Get Realms"
#http --verify=no --verbose GET $SSO_HOST/auth/admin/realms "Authorization: Bearer $access_token"