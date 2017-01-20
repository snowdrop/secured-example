#!/usr/bin/env bash

SCRIPT_DIR="$(dirname "$0")"

. $SCRIPT_DIR/../common.sh

auth_result=$(curl -sk -X POST $SSO_HOST/auth/realms/$REALM/protocol/openid-connect/token -d grant_type=password -d username=$USER -d client_secret=$SECRET -d password=$PASSWORD -d client_id=$CLIENT_ID)
access_token=$(echo -e "$auth_result" | awk -F"," '{print $1}' | awk -F":" '{print $2}' | sed s/\"//g | tr -d ' ')

echo ">>> Add user"
curl -sk -x POST $SSO_HOST/auth/admin/realms/$REALM/users -H "Authorization:Bearer $access_token" --data $SCRIPT_DIR/../user.json

echo ">>> Get User id"
userId=$(curl -sk $SSO_HOST/auth/admin/realms/$REALM/users -d username=bburke "Authorization: Bearer $access_token" | jsonValue id)
echo "User id : $userId"

echo ">>> Reset Password for the user"
curl -skx PUT $SSO_HOST//auth/admin/realms/$REALM/users/$userId/reset-password -d id=$userId -d realm=$REALM "Authorization: Bearer $access_token" --data $SCRIPT_DIR/../password.json