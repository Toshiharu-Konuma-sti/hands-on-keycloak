#!/bin/sh

CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/functions.sh
. $CUR_DIR/variables.sh
. $CUR_DIR/req_kc_apis.sh
. $CUR_DIR/pretreatment.sh

call_show_start_banner

echo "\n### START: Get Access Token ##########"
AC_TOKEN=$(call_fetch_master_access_token $KC_URL $ADM_USER $ADM_PASS)

echo "\n### START: Get clients ##########"
JSON_CLT=$(call_get_clients $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Get client scopes ##########"
JSON_SCP=$(call_get_client_scopes $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Assign client scope to client(my-web-app) ##########"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:my-scope"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "write:my-scope"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:baseball.manager"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:baseball.player"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:soccer.manager"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:soccer.player"

echo "\n### START: Assign client scope to client of demo app ##########"
run_assign_opt_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-baseball" "read:baseball.manager"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-baseball" "read:baseball.player"
run_assign_opt_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-soccer" "read:soccer.manager"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-soccer" "read:soccer.player"

call_show_finish_banner
