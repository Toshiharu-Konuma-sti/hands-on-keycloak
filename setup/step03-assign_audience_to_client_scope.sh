#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh
. $CUR_DIR/pretreatment.sh
. ${CUR_DIR}/.env-keycloak

call_show_start_banner

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token ${KC_URL} ${KEYCLOAK_ADMIN} ${KEYCLOAK_ADMIN_PASSWORD})

echo "\n### START: Get client scopes ##########"
JSON_SCP=$(call_get_client_scopes $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Map an audience to a client scopes ##########"
run_assign_audience_to_client_scope $KC_URL $AC_TOKEN $MY_REALM "$JSON_SCP" "read:my-scope" "my-web-app"
run_assign_audience_to_client_scope $KC_URL $AC_TOKEN $MY_REALM "$JSON_SCP" "write:my-scope" "my-web-app"
run_assign_audience_to_client_scope $KC_URL $AC_TOKEN $MY_REALM "$JSON_SCP" "read:baseball.manager" "app-baseball"
run_assign_audience_to_client_scope $KC_URL $AC_TOKEN $MY_REALM "$JSON_SCP" "read:baseball.player" "app-baseball"
run_assign_audience_to_client_scope $KC_URL $AC_TOKEN $MY_REALM "$JSON_SCP" "read:soccer.manager" "app-soccer"
run_assign_audience_to_client_scope $KC_URL $AC_TOKEN $MY_REALM "$JSON_SCP" "read:soccer.player" "app-soccer"

call_show_finish_banner
