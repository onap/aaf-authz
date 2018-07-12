#!/bin/bash 
#
# Docker Building Script.  Reads all the components generated by install, on per-version basis
#
# Pull in Variables from d.props
if [ ! -e ./d.props ]; then
  cp d.props.init d.props
fi

. ./d.props

# Create the Config (Security) Image
sed -e 's/${AAF_VERSION}/'${VERSION}'/g' -e 's/${AAF_COMPONENT}/'${AAF_COMPONENT}'/g' Dockerfile.config  > ../sample/Dockerfile
cd ..
cp ../cadi/aaf/target/aaf-cadi-aaf-${VERSION}-full.jar sample/bin
docker build -t ${ORG}/${PROJECT}/aaf_config:${VERSION} sample
rm sample/Dockerfile sample/bin/aaf-cadi-aaf-${VERSION}-full.jar
cd -

exit

# Second, build a core Docker Image
echo Building aaf_$AAF_COMPONENT...
# Apply currrent Properties to Docker file, and put in place.
sed -e 's/${AAF_VERSION}/'${VERSION}'/g' -e 's/${AAF_COMPONENT}/'${AAF_COMPONENT}'/g' Dockerfile.core > ../aaf_${VERSION}/Dockerfile
cd ..
docker build -t ${ORG}/${PROJECT}/aaf_core:${VERSION} aaf_${VERSION}
rm aaf_${VERSION}/Dockerfile
cd -

if ["$1" == ""]; then
  AAF_COMPONENTS=`ls ../aaf_*HOT/bin | grep -v '\.'`
else
  AAF_COMPONENTS=$1
fi

for AAF_COMPONENT in ${AAF_COMPONENTS}; do
        echo Building aaf_$AAF_COMPONENT...
        sed -e 's/${AAF_VERSION}/'${VERSION}'/g' -e 's/${AAF_COMPONENT}/'${AAF_COMPONENT}'/g' Dockerfile.ms > ../aaf_${VERSION}/Dockerfile
        cd ..
        docker build -t ${ORG}/${PROJECT}/aaf_${AAF_COMPONENT}:${VERSION}  aaf_${VERSION}
        rm aaf_${VERSION}/Dockerfile
        cd -
done


