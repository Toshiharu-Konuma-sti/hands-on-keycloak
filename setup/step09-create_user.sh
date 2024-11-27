#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh

call_show_start_banner

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token $KC_URL $ADM_USER $ADM_PASS)

echo "\n### START: Create users ##########"
##	call_enter_message "password of user to create"
##	PASSWD=$(call_enter_capture)
USER_NM="admin.sios"
PASSWD=$USER_NM
call_create_user $KC_URL $AC_TOKEN $MY_REALM $USER_NM $PASSWD
USER_NM="staff.sios"
call_create_user $KC_URL $AC_TOKEN $MY_REALM $USER_NM $PASSWD
USER_NM="taro.sios"
call_create_user $KC_URL $AC_TOKEN $MY_REALM $USER_NM $PASSWD

echo "\n### START: Get users ##########"
call_get_users $KC_URL $AC_TOKEN $MY_REALM

call_show_finish_banner
