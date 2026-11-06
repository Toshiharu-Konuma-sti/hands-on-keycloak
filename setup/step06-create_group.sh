#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh
. ${CUR_DIR}/.env-keycloak

call_show_start_banner

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token ${KC_URL} ${KEYCLOAK_ADMIN} ${KEYCLOAK_ADMIN_PASSWORD})

echo "\n### START: Create groups ##########"
call_create_group $KC_URL $AC_TOKEN $MY_REALM "my-staff-grp"
call_create_group $KC_URL $AC_TOKEN $MY_REALM "my-manager-grp"
call_create_group $KC_URL $AC_TOKEN $MY_REALM "my-admin-grp"

echo "\n### START: Get groups ##########"
call_get_groups $KC_URL $AC_TOKEN $MY_REALM

call_show_finish_banner
