#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh
. ${CUR_DIR}/.env-keycloak

call_show_start_banner

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token ${KC_URL} ${KEYCLOAK_ADMIN} ${KEYCLOAK_ADMIN_PASSWORD})

echo "\n### START: Create client scopes ##########"
call_create_client_scope $KC_URL $AC_TOKEN $MY_REALM "read:my-scope"
call_create_client_scope $KC_URL $AC_TOKEN $MY_REALM "write:my-scope"
call_create_client_scope $KC_URL $AC_TOKEN $MY_REALM "read:baseball.manager"
call_create_client_scope $KC_URL $AC_TOKEN $MY_REALM "read:baseball.player"
call_create_client_scope $KC_URL $AC_TOKEN $MY_REALM "read:soccer.manager"
call_create_client_scope $KC_URL $AC_TOKEN $MY_REALM "read:soccer.player"

echo "\n### START: Get client scopes ##########"
call_get_client_scopes $KC_URL $AC_TOKEN $MY_REALM

call_show_finish_banner
