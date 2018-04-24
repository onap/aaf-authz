#!/bin/bash drun.sh
# Pull in Variables from d.props
. ./d.props


if [ "$1" == "" ]; then
  AAF_COMPONENTS=`ls -r ../aaf_${VERSION}/bin | grep -v '\.'`
else
  AAF_COMPONENTS=$1
fi
  
for AAF_COMPONENT in ${AAF_COMPONENTS}; do 
	case "$AAF_COMPONENT" in
		"service") 
			PORTMAP="8100:8100"
	  		LINKS="--link aaf_cass:cassandra --add-host=$CASS_HOST" 
			;;
		"locate") 
			PORTMAP="8095:8095"
	  		LINKS="--link aaf_cass:cassandra --add-host=$CASS_HOST" 
			;;
		"oauth") 
			PORTMAP="8140:8140"
	  		LINKS="--link aaf_cass:cassandra --add-host=$CASS_HOST" 
			;;
		"gui") 
			PORTMAP="8200:8200"
			;;
		"cm") 
			PORTMAP="8150:8150"
	  		LINKS="--link aaf_cass:cassandra --add-host=$CASS_HOST" 
			;;
		"hello") 
			PORTMAP="8130:8130"
			;;
		"fs") 
			PORTMAP="80:8096"
			;;
	esac
	
	echo Starting aaf_$AAF_COMPONENT...

	docker run  \
	  -d \
	  --name aaf_$AAF_COMPONENT \
	  --hostname="${AAF_COMPONENT}.aaf.osaaf.org" \
	  --add-host="$HOSTNAME:$HOST_IP" \
	  --add-host="aaf.osaaf.org:$HOST_IP" \
	  ${LINKS} \
	  --publish $PORTMAP \
	  --mount type=bind,source=$CONF_ROOT_DIR,target=/opt/app/osaaf \
	  ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION} 
done
