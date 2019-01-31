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
# Docker Building Script.  Reads all the components generated by install, on per-version basis
#
# Pull in Variables from d.props
if [ ! -e ./d.props ]; then
    cp d.props.init d.props
fi

. ./d.props

DOCKER=${DOCKER:=docker}

echo "Building Containers for aaf components, version $VERSION"

# AAF_cass now needs a version...
cd ../auth-cass/docker
bash ./dbuild.sh
cd -

# AAF Base version - set the core image, etc
sed -e 's/${AAF_VERSION}/'${VERSION}'/g' \
    Dockerfile.base > Dockerfile
$DOCKER build -t ${ORG}/${PROJECT}/aaf_base:${VERSION} .
$DOCKER tag ${ORG}/${PROJECT}/aaf_base:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_base:${VERSION}
$DOCKER tag ${ORG}/${PROJECT}/aaf_base:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_base:latest
rm Dockerfile

# Create the AAF Config (Security) Images
cd ..
cp auth-cmd/target/aaf-auth-cmd-$VERSION-full.jar sample/bin
cp -Rf ../conf/CA sample


# AAF Config image (for AAF itself)
sed -e 's/${AAF_VERSION}/'${VERSION}'/g' \
    -e 's/${AAF_COMPONENT}/'${AAF_COMPONENT}'/g' \
    -e 's/${DOCKER_REPOSITORY}/'${DOCKER_REPOSITORY}'/g' \
    docker/Dockerfile.config > sample/Dockerfile
$DOCKER build -t ${ORG}/${PROJECT}/aaf_config:${VERSION} sample
$DOCKER tag ${ORG}/${PROJECT}/aaf_config:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_config:${VERSION}
$DOCKER tag ${ORG}/${PROJECT}/aaf_config:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_config:latest

cp ../cadi/servlet-sample/target/aaf-cadi-servlet-sample-${VERSION}-sample.jar sample/bin
# AAF Agent Image (for Clients)
sed -e 's/${AAF_VERSION}/'${VERSION}'/g' \
    -e 's/${AAF_COMPONENT}/'${AAF_COMPONENT}'/g' \
    -e 's/${DOCKER_REPOSITORY}/'${DOCKER_REPOSITORY}'/g' \
    docker/Dockerfile.client > sample/Dockerfile
$DOCKER build -t ${ORG}/${PROJECT}/aaf_agent:${VERSION} sample
$DOCKER tag ${ORG}/${PROJECT}/aaf_agent:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_agent:${VERSION}
$DOCKER tag ${ORG}/${PROJECT}/aaf_agent:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_agent:latest

# Clean up 
rm sample/Dockerfile sample/bin/aaf-auth-cmd-${VERSION}-full.jar sample/bin/aaf-cadi-servlet-sample-${VERSION}-sample.jar 
rm -Rf sample/CA
cd -

########
# Second, build a core Docker Image
echo Building aaf_$AAF_COMPONENT...
# Apply currrent Properties to Docker file, and put in place.
sed -e 's/${AAF_VERSION}/'${VERSION}'/g' \
    -e 's/${AAF_COMPONENT}/'${AAF_COMPONENT}'/g' \
    -e 's/${DOCKER_REPOSITORY}/'${DOCKER_REPOSITORY}'/g' \
    Dockerfile.core >../aaf_${VERSION}/Dockerfile
cd ..
$DOCKER build -t ${ORG}/${PROJECT}/aaf_core:${VERSION} aaf_${VERSION}
$DOCKER tag ${ORG}/${PROJECT}/aaf_core:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_core:${VERSION}
$DOCKER tag ${ORG}/${PROJECT}/aaf_core:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_core:latest
rm aaf_${VERSION}/Dockerfile
cd -

#######
# Do all the Containers related to AAF Services
#######
if ["$1" == ""]; then
    AAF_COMPONENTS=$(ls ../aaf_${VERSION}/bin | grep -v '\.')
else
    AAF_COMPONENTS=$1
fi

cp ../sample/bin/pod_wait.sh  ../aaf_${VERSION}/bin
for AAF_COMPONENT in ${AAF_COMPONENTS}; do
    echo Building aaf_$AAF_COMPONENT...
    sed -e 's/${AAF_VERSION}/'${VERSION}'/g' \
        -e 's/${AAF_COMPONENT}/'${AAF_COMPONENT}'/g' \
        -e 's/${DOCKER_REPOSITORY}/'${DOCKER_REPOSITORY}'/g' \
        Dockerfile.ms >../aaf_${VERSION}/Dockerfile
    cd ..
    $DOCKER build -t ${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION} aaf_${VERSION}
    $DOCKER tag ${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION}
    $DOCKER tag ${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION} ${DOCKER_REPOSITORY}/${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:latest
    rm aaf_${VERSION}/Dockerfile
    cd -

done
rm ../aaf_${VERSION}/bin/pod_wait.sh
