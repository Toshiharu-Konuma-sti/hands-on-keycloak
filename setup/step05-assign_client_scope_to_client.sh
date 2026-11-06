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

echo "\n### START: Get clients ##########"
JSON_CLT=$(call_get_clients $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Get client scopes ##########"
JSON_SCP=$(call_get_client_scopes $KC_URL $AC_TOKEN $MY_REALM)

echo "\n### START: Assign client scope to client(my-web-app) ##########"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "read:my-scope"
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "write:my-scope"
echo "\n### START: Assign client scope to client of demo app ##########"
# 経費申請アプリ: 一覧(read)は常に、申請(write)は要求したときだけ。承認(approve)は割り当てない
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-expense-claim" "expenses:read"
run_assign_opt_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-expense-claim" "expenses:write"
# 経費承認アプリ: 一覧(read)は常に、承認(approve)は要求したときだけ
run_assign_def_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-expense-approval" "expenses:read"
run_assign_opt_client_scope_to_client \
	$KC_URL $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-expense-approval" "expenses:approve"

call_show_finish_banner
