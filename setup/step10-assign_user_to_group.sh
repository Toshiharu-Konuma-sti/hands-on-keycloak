#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh
. $CUR_DIR/pretreatment.sh
. ${CUR_DIR}/.env-keycloak

call_show_start_banner

echo "\n### START: Get an access token ##########"
AC_TOKEN=$(call_fetch_master_access_token ${KC_URL} ${KEYCLOAK_ADMIN} ${KEYCLOAK_ADMIN_PASSWORD})

echo "\n### START: Get groups ##########"
JSON_GRP=$(call_get_groups $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Get users ##########"
JSON_USR=$(call_get_users $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Assign a user to a group ##########"
run_assign_user_to_group \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-staff-grp" "staff.sios"
run_assign_user_to_group \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-manager-grp" "taro.sios"
run_assign_user_to_group \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-admin-grp" "admin.sios"

call_show_finish_banner
