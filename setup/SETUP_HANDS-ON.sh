#!/bin/sh

PATH_BASE_DIR="$(dirname $(pwd)/$0)/"
. $PATH_BASE_DIR/functions.sh
. $PATH_BASE_DIR/subroutines.sh

HST="http://localhost:8080"
ADM_USER="admin"
ADM_PASS="admin"

MY_REALM="handson"

echo "##### START ######################################"

	AC_TOKEN=$(call_fetch_master_access_token $HST $ADM_USER $ADM_PASS)

	call_create_realm $HST $AC_TOKEN $MY_REALM

	call_create_client_scope $HST $AC_TOKEN $MY_REALM "my-scope"
	call_create_client_scope $HST $AC_TOKEN $MY_REALM "baseball.manager"
	call_create_client_scope $HST $AC_TOKEN $MY_REALM "baseball.player"
	call_create_client_scope $HST $AC_TOKEN $MY_REALM "soccer.manager"
	call_create_client_scope $HST $AC_TOKEN $MY_REALM "soccer.player"
	JSON_SCP=$(call_get_client_scopes $HST $AC_TOKEN $MY_REALM)

	call_create_client $HST $AC_TOKEN $MY_REALM "my-web-app" "http://localhost:8222/*" false
	call_create_client $HST $AC_TOKEN $MY_REALM "app-baseball" "http://localhost:8222/*" true
	call_create_client $HST $AC_TOKEN $MY_REALM "app-soccer" "http://localhost:8222/*" true
	JSON_CLT=$(call_get_clients $HST $AC_TOKEN $MY_REALM)

	run_assing_def_client_scope_to_client \
		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "my-web-app" "my-scope"

	run_assing_opt_client_scope_to_client \
		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-baseball" "baseball.manager"
	run_assing_def_client_scope_to_client \
		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-baseball" "baseball.player"
	run_assing_opt_client_scope_to_client \
		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-soccer" "soccer.manager"
	run_assing_def_client_scope_to_client \
		$HST $AC_TOKEN $MY_REALM "$JSON_CLT" "$JSON_SCP" "app-soccer" "soccer.player"

	call_create_group $HST $AC_TOKEN $MY_REALM "my-admin-grp"
	call_create_group $HST $AC_TOKEN $MY_REALM "my-staff-grp"
	JSON_GRP=$(call_get_groups $HST $AC_TOKEN $MY_REALM)

	call_create_realm_role $HST $AC_TOKEN $MY_REALM "my-admin-role"
	call_create_realm_role $HST $AC_TOKEN $MY_REALM "my-staff-role"
	JSON_ROL=$(call_get_realm_roles $HST $AC_TOKEN $MY_REALM)

	run_assing_realm_role_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_ROL" "my-admin-grp" "my-admin-role"
	run_assing_realm_role_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_ROL" "my-staff-grp" "my-staff-role"

##	call_enter_message "password of user to create"
##	PASSWD=$(call_enter_capture)
	USER_NM="admin.sios"
	call_create_user $HST $AC_TOKEN $MY_REALM $USER_NM $USER_NM
	USER_NM="staff.sios"
	call_create_user $HST $AC_TOKEN $MY_REALM $USER_NM $USER_NM
	JSON_USR=$(call_get_users $HST $AC_TOKEN $MY_REALM)
	USER_NM="taro.sios"
	call_create_user $HST $AC_TOKEN $MY_REALM $USER_NM $USER_NM

	run_assing_user_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-admin-grp" "admin.sios"
	run_assing_user_to_group \
		$HST $AC_TOKEN $MY_REALM "$JSON_GRP" "$JSON_USR" "my-staff-grp" "staff.sios"

echo "\n##### FINISH #####################################"
