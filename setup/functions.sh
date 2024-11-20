
call_test()
{
	echo "hello $1"
}

# {{{ call_fetch_master_access_token()
# $1: Admin REST API Host
# $2: administrator's username
# $3: administrator's password
call_fetch_master_access_token()
{
	AC_TOKEN=$(curl -v POST \
		"$1/realms/master/protocol/openid-connect/token" \
		-d "grant_type=password" \
		-d "client_id=admin-cli" \
		-d "username=$2" \
		-d "password=$3" | \
		tee /dev/tty | \
		jq ".access_token" | \
		sed -e 's/"//g')

	echo "$AC_TOKEN"
}
# }}}

# {{{ call_create_realm()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name to create newlly
call_create_realm()
{
	curl -v POST \
		"$1/admin/realms" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "{\"realm\": \"$3\", \"enabled\": true, \"accessTokenLifespan\": 1800, \"internationalizationEnabled\": true, \"registrationAllowed\": true, \"rememberMe\": true, \"resetPasswordAllowed\": true, \"supportedLocales\": [\"ja\", \"en\"]}"
}
# }}}

# {{{ call_get_realms()
# $1: Admin REST API Host
# $2: access token
call_get_realms()
{
	JSON=$(curl -v \
		"$1/admin/realms" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_create_client_scope()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: scope's name to create newlly
call_create_client_scope()
{
	curl -v POST \
		"$1/admin/realms/$3/client-scopes" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "{\"name\": \"$4\", \"protocol\": \"openid-connect\", \"attributes\": {\"include.in.token.scope\": true}}"
}
# }}}

# {{{ call_get_client_scopes()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
call_get_client_scopes()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/client-scopes" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_create_client()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: client id to create newlly
# $5: redirect URL
# $6: flag of 'consentRequired' (This is a flag whether to display the consent screen or not.)
call_create_client()
{
	REDIRCT=$(echo $5 | sed -e 's/,/","/g')
	WEB_ORG=$(echo $5 | sed -E 's/^.*(http|https)(:\/\/)([^/]+).*/\1\2\3/g')
	JSON="{\"clientId\": \"$4\", \"enabled\": true, \"secret\": \"my-secret\", \"redirectUris\": [\"$REDIRCT\"], \"webOrigins\": [\"$WEB_ORG\"], \"publicClient\": false, \"consentRequired\": $6, \"fullScopeAllowed\": true}"

	curl -v POST \
		"$1/admin/realms/$3/clients" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "$JSON"
}
# }}}

# {{{ call_get_clients()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
call_get_clients()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/clients" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_map_client_scope_audience()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: client scope's id to map an audience
# $5: client's name as an audience
call_map_client_scope_audience()
{
	curl -v -X POST \
		"$1/admin/realms/$3/client-scopes/$4/protocol-mappers/add-models" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "[{\"name\": \"mapper-$5\", \"protocol\": \"openid-connect\", \"protocolMapper\": \"oidc-audience-mapper\", \"config\": {\"included.client.audience\": \"$5\", \"access.token.claim\": \"true\"}}]"
}
# }}}

# {{{ call_get_client_scope_audience()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: client scope's id to show an audience
call_get_client_scope_audience()
{
	curl -v \
		"$1/admin/realms/$3/client-scopes/$4/protocol-mappers/models" \
		-H "Authorization: Bearer $2"
}
# }}}

# {{{ call_assing_def_client_scope_to_client()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: client's id
# $5: client scope's id
call_assing_def_client_scope_to_client()
{
	curl -v -X PUT \
		"$1/admin/realms/$3/clients/$4/default-client-scopes/$5" \
		-H "Authorization: Bearer $2"
}
# }}}

# {{{ call_assing_opt_client_scope_to_client()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: client's id
# $5: client scope's id
call_assing_opt_client_scope_to_client()
{
	curl -v -X PUT \
		"$1/admin/realms/$3/clients/$4/optional-client-scopes/$5" \
		-H "Authorization: Bearer $2"
}
# }}}

# {{{ call_get_def_client_scopes_in_client()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: client's id
call_get_def_client_scopes_in_client()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/clients/$4/default-client-scopes" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_get_opt_client_scopes_in_client()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: client's id
call_get_opt_client_scopes_in_client()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/clients/$4/optional-client-scopes" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_create_group()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: group's name to create newlly
call_create_group()
{
	curl -v POST \
		"$1/admin/realms/$3/groups" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "{\"name\": \"$4\"}"
}
# }}}

# {{{ call_get_groups()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
call_get_groups()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/groups" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_create_realm_role()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: role's name to create newlly
call_create_realm_role()
{
	curl -v POST \
		"$1/admin/realms/$3/roles" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "{\"name\": \"$4\"}"
}
# }}}

# {{{ call_get_realm_roles()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
call_get_realm_roles()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/roles" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_assign_realm_role_to_group()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: group's id to assing role
# $5: rold's id to assing to group
# $6: rold's name to assing to group
call_assign_realm_role_to_group()
{
	JSON="[{\"id\": \"$5\", \"name\": \"$6\"}]"
	curl -v POST \
		"$1/admin/realms/$3/groups/$4/role-mappings/realm" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "$JSON"
}
# }}}

# {{{ call_get_realm_roles_in_group()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: group's id
call_get_realm_roles_in_group()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/groups/$4/role-mappings" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_create_user()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: user's name to create newlly
# $5: user's password to create newlly
call_create_user()
{
	NAME_F=$(echo "$4" | sed -e "s/\..*//")
	NAME_L=$(echo "$4" | sed -e "s/.*\.//")

	curl -v POST \
		"$1/admin/realms/$3/users" \
		-H "Authorization: Bearer $2" \
		-H "Content-Type: application/json" \
		-d "{\"username\": \"$4\", \"firstName\": \"$NAME_F\", \"lastName\": \"$NAME_L\", \"email\": \"$4@test.com\", \"enabled\": true, \"credentials\": [{\"type\": \"password\", \"value\": \"$5\", \"temporary\": false}]}"
}
# }}}

# {{{ call_get_users()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
call_get_users()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/users" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# {{{ call_assign_user_to_group()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: user id
# $5: group id
call_assign_user_to_group()
{
	curl -v -X PUT \
		"$1/admin/realms/$3/users/$4/groups/$5" \
		-H "Authorization: Bearer $2"
}
# }}}

# {{{ call_get_members_in_group()
# $1: Admin REST API Host
# $2: access token
# $3: realm's name
# $4: group's id
call_get_members_in_group()
{
	JSON=$(curl -v \
		"$1/admin/realms/$3/groups/$4/members" \
		-H "Authorization: Bearer $2" | \
		tee /dev/tty | \
		jq)

	echo "$JSON"
}
# }}}

# utility functions

# {{{ call_enter_message() & call_enter_capture()
# $1: the name of value to enter
call_enter_message()
{
	echo "Enter '$1' and press [ENTER]:"
}
call_enter_capture()
{
	read VALUE
	echo "$VALUE"
}
# }}}

# {{{ call_extract_id_from_json()
# $1: JSON
# $2: key field to extract 'id' from JSON
# $3: key value to extract 'id' from JSON
call_extract_id_from_json()
{
	ID=$(echo "$1" | jq ".[] | select(.$2 == \"$3\") | .id" | sed -z 's/\n//g' | sed -e 's/"//g')
	echo "$ID"
}
# }}}

