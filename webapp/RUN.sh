#!/bin/sh

# {{{ show_usage()
show_usage()
{
	cat << EOS
Usage: $(basename $0) [options]

Run the Gradle tasks of this multi-project (relying-party and resource-server)
on this machine. JDK 21 is required. Normally, the containers in
../container/docker-compose.yml build and run them, so this is not needed.

Options:
  rs                    Run the resource-server (http://localhost:8111/).
  rp                    Run the relying-party (http://localhost:8222/).
  assemble              Assemble the outputs of this project.
  doc                   Generate the Javadoc.
  test                  Run the unit tests.

Note: When run on this machine, the hostname 'keycloak' and 'resource-server'
in each application.yaml can not be resolved. Replace them with 'localhost'.

EOS
}
# }}}

S_TIME=$(date +%s)
CUR_DIR=$(cd $(dirname $0); pwd)
. $CUR_DIR/common.sh

cd $CUR_DIR
case "$1" in
	"rs")
		start_banner
		./gradlew :resource-server:bootRun
		;;
	"rp")
		start_banner
		./gradlew :relying-party:bootRun
		;;
	"assemble")
		start_banner
		./gradlew assemble --info
		finish_banner $S_TIME
		;;
	"doc")
		start_banner
		./gradlew javadoc
		command -v tree >/dev/null 2>&1 && tree -L 2 */build/docs/
		finish_banner $S_TIME
		;;
	"test")
		start_banner
		./gradlew test
		finish_banner $S_TIME
		;;
	*)
		show_usage
		;;
esac
