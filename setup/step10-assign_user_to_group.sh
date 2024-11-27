#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/call_kc_apis.sh
. $CUR_DIR/comb_kc_apis.sh

call_show_start_banner

echo "\n### START: Get an access token ##########"
AC_TOKEN=$(call_fetch_master_access_token $KC_URL $ADM_USER $ADM_PASS)

echo "\n### START: Get groups ##########"
JSON_GRP=$(call_get_groups $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Get users ##########"
JSON_USR=$(call_get_users $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Assign a user to a group ##########"
run_assing_user_to_group \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-admin-grp" "admin.sios"
run_assing_user_to_group \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-staff-grp" "staff.sios"
run_assing_user_to_group \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-flat-grp" "taro.sios"

call_show_finish_banner
