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
	call_get_realms $HST $AC_TOKEN
