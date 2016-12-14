#!/usr/bin/env bash

SCRIPT_DIR="$(dirname "$0")"

. $SCRIPT_DIR/../common.sh

auth_result=$(http --verify=no -f $SSO_HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD client_secret=$SECRET grant_type=password client_id=$CLIENT_ID)
access_token=$(echo -e "$auth_result" | awk -F"," '{print $1}' | awk -F":" '{print $2}' | sed s/\"//g | tr -d ' ')

echo ">>> Add user"
http --verify=no --verbose POST $SSO_HOST/auth/admin/realms/$REALM/users "Authorization: Bearer $access_token" < $SCRIPT_DIR/../user.json

echo ">>> Get User id"
userId=$(http --verify=no --verbose GET $SSO_HOST/auth/admin/realms/$REALM/users username==bburke "Authorization: Bearer $access_token" | jsonValue id)
echo "User id : $userId"

echo ">>> Reset Password for the user"
http --verify=no --verbose PUT $SSO_HOST/auth/admin/realms/$REALM/users/$userId/reset-password id==$userId realm==$REALM "Authorization: Bearer $access_token" < $SCRIPT_DIR/../password.json