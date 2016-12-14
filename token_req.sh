#!/usr/bin/env bash

REALM=master
USER=admin
PASSWORD=admin
CLIENT_ID=demoapp
HOST=https://secure-sso-sso.e8ca.engint.openshiftapps.com
CLIENT=http://springboot-rest-sso.e8ca.engint.openshiftapps.com

auth_result=$(http --verify=no --verbose -f $HOST/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD grant_type=password client_id=$CLIENT_ID)
#auth_result=$(curl -sk -X POST $HOST/auth/realms/$REALM/protocol/openid-connect/token -d grant_type=password -d username=$USER -d password=$PASSWORD -d client_id=$CLIENT_ID)

echo ">>> HTTP Token query"
#echo "http --verify=no -f http://$HOST:$PORT_HTTP/auth/realms/$REALM/protocol/openid-connect/token username=$USER password=$PASSWORD grant_type=password client_id=$CLIENT_ID"
#echo "curl -X POST $HOST/auth/realms/$REALM/protocol/openid-connect/token -d grant_type=password -d username=$USER -d password=$PASSWORD -d client_id=$CLIENT_ID"

access_token=$(echo -e "$auth_result" | awk -F"," '{print $1}' | awk -F":" '{print $2}' | sed s/\"//g | tr -d ' ')

echo ">>> TOKEN Received"
echo $access_token

echo ">>> Gateway Service URL"
echo "$URL"

echo ">>> GET Blog article : 1"
#http --verify=no GET $CLIENT/greeting "Authorization: Bearer $access_token"
curl -k $CLIENT/greeting -H "Authorization:Bearer $access_token"