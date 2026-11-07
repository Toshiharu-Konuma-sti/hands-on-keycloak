#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh
. ${CUR_DIR}/.env-keycloak

call_show_start_banner

REDIRECT="http://localhost:8222/*"

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token ${KC_URL} ${KEYCLOAK_ADMIN} ${KEYCLOAK_ADMIN_PASSWORD})

echo "\n### START: Create clients ##########"
call_create_client $KC_URL $AC_TOKEN $MY_REALM "my-web-app" $REDIRECT false
call_create_client $KC_URL $AC_TOKEN $MY_REALM "my-api-gateway" "" false
call_create_client $KC_URL $AC_TOKEN $MY_REALM "expense-api" "" false
# 表示名は、step01で登録したキー(${...})を渡す。同意画面の表示は日英で切り替わる
call_create_client $KC_URL $AC_TOKEN $MY_REALM "app-expense-claim" $REDIRECT true '${client_app_expense_claim}'
call_create_client $KC_URL $AC_TOKEN $MY_REALM "app-expense-approval" $REDIRECT true '${client_app_expense_approval}'

echo "\n### START: Get clients ##########"
call_get_clients $KC_URL $AC_TOKEN $MY_REALM

call_show_finish_banner
