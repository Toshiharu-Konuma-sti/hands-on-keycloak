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

echo "\n### START: Create realm rols ##########"
# 平社員 < 課長 < 部長 の3段階
call_create_realm_role $KC_URL $AC_TOKEN $MY_REALM "my-staff-role"
call_create_realm_role $KC_URL $AC_TOKEN $MY_REALM "my-manager-role"
call_create_realm_role $KC_URL $AC_TOKEN $MY_REALM "my-admin-role"

echo "\n### START: Get realm roles ##########"
JSON_ROL=$(call_get_realm_roles $KC_URL $AC_TOKEN $MY_REALM)

# 上位のロールが下位のロールを含む(複合ロール)。部長のトークンには、課長と平社員のロールも入る
echo "\n### START: Include a lower role in a higher role (composite role) ##########"
run_assign_composite_role $KC_URL $AC_TOKEN $MY_REALM "$JSON_ROL" "my-manager-role" "my-staff-role"
run_assign_composite_role $KC_URL $AC_TOKEN $MY_REALM "$JSON_ROL" "my-admin-role" "my-manager-role"

call_show_finish_banner
