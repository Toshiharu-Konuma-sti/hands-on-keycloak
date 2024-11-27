
# {{{ call_own_fname()
call_own_fname()
{
	OFNM=$(basename $0)
	echo "$OFNM"
}
# }}}

# {{{ call_path_of_container()
# $1: the current directory
call_path_of_container()
{
	TARGET=$(realpath $1/../container)
	echo "$TARGET"
}
# }}}

# {{{ call_path_of_setup()
# $1: the current directory
call_path_of_setup()
{
	TARGET=$(realpath $1/../setup)
	echo "$TARGET"
}
# }}}

# {{{ call_path_of_development()
# $1: the current directory
call_path_of_development()
{
	TARGET=$(realpath $1/../development)
	echo "$TARGET"
}
# }}}

# {{{ call_path_of_experience()
# $1: the current directory
call_path_of_experience()
{
	TARGET=$(realpath $1/../try-my-hand)
	echo "$TARGET"
}
# }}}

# {{{ call_show_start_banner()
# $0: the name of the script being executed 
call_show_start_banner()
{
	echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n> START: Script = [$(call_own_fname)]\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
}
# }}}

# {{{ call_show_finish_banner()
# $0: the name of the script being executed 
call_show_finish_banner()
{
	echo "\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n< FINISH: Script = [$(call_own_fname)]\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
}
# }}}

