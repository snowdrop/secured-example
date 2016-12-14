#!/usr/bin/env bash

REALM=master
USER=admin
PASSWORD=admin
CLIENT_ID=demoapp
HOST=https://secure-sso-sso.e8ca.engint.openshiftapps.com
CLIENT=http://springboot-rest-sso.e8ca.engint.openshiftapps.com

echo ">>> HTTP Token query"
echo "http --verify=no -f $HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD grant_type=password client_id=$CLIENT_ID"

auth_result=$(http --verify=no -f $HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD grant_type=password client_id=$CLIENT_ID)
access_token=$(echo -e "$auth_result" | awk -F"," '{print $1}' | awk -F":" '{print $2}' | sed s/\"//g | tr -d ' ')

echo ">>> TOKEN Received"
echo $access_token

echo ">>> Greeting Service"
http --verify=no GET $CLIENT/greeting "Authorization: Bearer $access_token"
