#!/bin/bash
#########
#  ============LICENSE_START====================================================
#  org.onap.aaf
#  ===========================================================================
#  Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
#  ===========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END====================================================
#
# Docker push Script.  Reads all the components generated by install, on per-version basis
#
# Pull in Variables from d.props
. ./d.props
DOCKER=${DOCKER:=docker}

AAF_COMPONENTS="config agent base core cass $(cat components) "

for AAF_COMPONENT in ${AAF_COMPONENTS}; do
        # docker push ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${OLD_VERSION}
        $DOCKER push ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION}
        $DOCKER tag ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION}-latest
        $DOCKER tag ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:latest
        $DOCKER tag ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION%-SNAPSHOT}-STAGING-latest
        $DOCKER push ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION}-latest
	$DOCKER push ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:latest
	$DOCKER push ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION%-SNAPSHOT}-STAGING-latest
done
