#!/bin/bash dstop.sh
ORG=onap
PROJECT=aaf
DOCKER_REPOSITORY=nexus3.onap.org:10003
VERSION=2.1.0-SNAPSHOT
. ./d.props

if [ "$1" == "" ]; then
  AAF_COMPONENTS=`ls ../aaf_${VERSION}/bin | grep -v '\.'`
else
  AAF_COMPONENTS=$1
fi

for AAF_COMPONENT in ${AAF_COMPONENTS}; do
  docker stop aaf_$AAF_COMPONENT
done
