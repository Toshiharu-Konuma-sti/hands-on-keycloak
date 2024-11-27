#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh

call_show_start_banner

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token $KC_URL $ADM_USER $ADM_PASS)

echo "\n### START: Create a realm ##########"
call_create_realm $KC_URL $AC_TOKEN $MY_REALM

call_show_finish_banner
