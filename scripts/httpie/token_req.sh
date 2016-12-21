#!/usr/bin/env bash

SCRIPT_DIR="$(dirname "$0")"

. $SCRIPT_DIR/../common.sh

#echo ">>> HTTP Token query"
#echo "http --verify=no -f $SSO_HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD client_secret=$SECRET grant_type=password client_id=$CLIENT_ID"

auth_result=$(http --verify=no -f $SSO_HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD client_secret=$SECRET grant_type=password client_id=$CLIENT_ID)
access_token=$(echo -e "$auth_result" | awk -F"," '{print $1}' | awk -F":" '{print $2}' | sed s/\"//g | tr -d ' ')

#echo ">>> TOKEN Received"
#echo -e "$auth_result"
echo -e "$access_token"

echo ">>> Greeting"
http --verify=no GET $APP/greeting "Authorization: Bearer $access_token"

echo ">>> Greeting Customized Message"
http --verify=no GET $APP/greeting name==Spring "Authorization:Bearer $access_token"