#!/usr/bin/env bash

# http://www.keycloak.org/docs/rest-api/index.html#_set_up_a_temporary_password_for_the_user

SCRIPT_DIR="$(dirname "$0")"
echo $SCRIPT_DIR

. $SCRIPT_DIR/common.sh

function jsonValue() {
  KEY=$1
  num=$2
  awk -F"[,:}]" '{for(i=1;i<=NF;i++){if($i~/'$KEY'\042/){print $(i+1)}}}' | tr -d '"' | sed -n ${num}p
}

echo ">>> HTTP Token query"
echo "http --verify=no -f $SSO_HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD client_secret=$SECRET grant_type=password client_id=$CLIENT_ID"

auth_result=$(http --verify=no -f $SSO_HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD client_secret=$SECRET grant_type=password client_id=$CLIENT_ID)
access_token=$(echo -e "$auth_result" | awk -F"," '{print $1}' | awk -F":" '{print $2}' | sed s/\"//g | tr -d ' ')

#echo ">>> Add user"
# http --verify=no --verbose POST $SSO_HOST/auth/admin/realms/$REALM/users "Authorization: Bearer $access_token" < $SCRIPT_DIR/user.json

echo ">>> Get User id"
userId=$(http --verify=no --verbose GET $SSO_HOST/auth/admin/realms/$REALM/users username==bburke "Authorization: Bearer $access_token" | jsonValue id)
echo "User id : $userId"

echo ">>> Reset Password for the user"
http --verify=no --verbose PUT $SSO_HOST/auth/admin/realms/$REALM/users/$userId/reset-password id==$userId realm==$REALM "Authorization: Bearer $access_token" < $SCRIPT_DIR/password.json