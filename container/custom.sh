
# {{{ create_container()
# $1: the current directory
create_container()
{
	CUR_DIR=$1
	echo "\n### START: Create new containers ##########"
	docker compose \
		-f $CUR_DIR/docker-compose.yml \
		up -d -V --remove-orphans
}
# }}}

# {{{ destory_container()
# $1: the current directory
destory_container()
{
	CUR_DIR=$1
	echo "\n### START: Destory existing containers ##########"
	docker compose \
		-f $CUR_DIR/docker-compose.yml \
		down -v --remove-orphans
}
# }}}

# {{{ rebuild_container()
# $1: the current directory
# $2: the name of container to rebuild
rebuild_container()
{
	CUR_DIR=$1
	CONTAINER_NM=$2
	echo "\n### START: Rebuild a container ##########"
	docker stop $CONTAINER_NM
	IMAGE_NM=$(docker inspect --format='{{.Config.Image}}' $CONTAINER_NM)
	docker rm $CONTAINER_NM
	docker rmi $IMAGE_NM
	docker-compose \
		-f $CUR_DIR/docker-compose.yml \
		up -d -V --build $CONTAINER_NM
}
# }}}

# {{{ show_url()
show_url()
{
	cat << EOS

/************************************************************
 * Information:
 * - Navigate to Web ui tools with the URL below.
 *   - Keycloak:    http://localhost:8080/
 *   - Grafana:     http://localhost:3000/
 *   - Nextcloud:   http://localhost:8081/
 ***********************************************************/

EOS
}
# }}}

# {{{ show_usage()
show_usage()
{
	cat << EOS
Usage: $(basename $0) [options]

Start the containers needed for the hands-on. If there are any containers
already running, stop them and remove resources beforehand.

Options:
  up                    Start the containers.
  down                  Stop the containers and remove resources.
  rebuild {container}   Stop the specified container, removes its image, and
                        restarts it.
  list                  Show the list of containers.
  info                  Show the information such as URLs.

EOS
}
# }}}
