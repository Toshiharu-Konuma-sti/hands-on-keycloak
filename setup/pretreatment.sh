
# {{{ run_assign_audience_to_client_scope()
# $1: Admin REST API Host
# $2: access token
# $3: realm
# $4: json of client scope
# $5: client scope's name to map an audience
# $6: client's name as an audience
run_assign_audience_to_client_scope()
{
	SCP_NM=$5
	SCP_ID=$(call_extract_id_from_json "$4" "name" $SCP_NM)
	CLT_NM=$6
	call_assign_audience_to_client_scope $1 $2 $3 $SCP_ID $CLT_NM
	call_get_client_scope_audience $1 $2 $3 $SCP_ID
}
# }}}

# {{{ run_assign_def_client_scope_to_client()
# $1: Admin REST API Host
# $2: access token
# $3: realm
# $4: json of client
# $5: json of client scope
# $6: client's name
# $7: client scope's name
run_assign_def_client_scope_to_client()
{
	CLT_NM=$6
	CLT_ID=$(call_extract_id_from_json "$4" "clientId" $CLT_NM)
	SCP_NM=$7
	SCP_ID=$(call_extract_id_from_json "$5" "name" $SCP_NM)
	call_assign_def_client_scope_to_client $1 $2 $3 $CLT_ID $SCP_ID
	call_get_def_client_scopes_in_client $1 $2 $3 $CLT_ID
}
# }}}

# {{{ run_assign_opt_client_scope_to_client()
# $1: Admin REST API Host
# $2: access token
# $3: realm
# $4: json of client
# $5: json of client scope
# $6: client's name
# $7: client scope's name
run_assign_opt_client_scope_to_client()
{
	CLT_NM=$6
	CLT_ID=$(call_extract_id_from_json "$4" "clientId" $CLT_NM)
	SCP_NM=$7
	SCP_ID=$(call_extract_id_from_json "$5" "name" $SCP_NM)
	call_assign_opt_client_scope_to_client $1 $2 $3 $CLT_ID $SCP_ID
	call_get_opt_client_scopes_in_client $1 $2 $3 $CLT_ID
}
# }}}

# {{{ run_assign_realm_role_to_group()
# $1: Admin REST API Host
# $2: access token
# $3: realm
# $4: json of groups
# $5: json of roles
# $6: group's name
# $7: role's name
run_assign_realm_role_to_group()
{
	GRP_NM=$6
	GRP_ID=$(call_extract_id_from_json "$4" "name" $GRP_NM)
	ROL_NM=$7
	ROL_ID=$(call_extract_id_from_json "$5" "name" $ROL_NM)
	call_assign_realm_role_to_group $1 $2 $3 $GRP_ID $ROL_ID $ROL_NM
	call_get_realm_roles_in_group $1 $2 $3 $GRP_ID
}
# }}}

# {{{ run_assign_user_to_group()
# $1: Admin REST API Host
# $1: access token
# $2: realm
# $3: json of groups
# $4: json of users
run_assign_user_to_group()
{
	GRP_NM=$6
	GRP_ID=$(call_extract_id_from_json "$4" "name" $GRP_NM)
	USR_NM=$7
	USR_ID=$(call_extract_id_from_json "$5" "username" $USR_NM)
	call_assign_user_to_group $1 $2 $3 $USR_ID $GRP_ID
	call_get_members_in_group $1 $2 $3 $GRP_ID
}
# }}}

