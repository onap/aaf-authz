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
# This script is run when starting aaf_config Container.
#  It needs to cover the cases where the initial data doesn't exist, and when it has already been configured (don't overwrite)
#
JAVA=/usr/bin/java

OSAAF=/opt/app/osaaf
LOCAL=$OSAAF/local
DATA=$OSAAF/data
PUBLIC=$OSAAF/public
CONFIG=/opt/app/aaf_config

JAVA_CADI="$JAVA -cp $CONFIG/bin/aaf-auth-cmd-*-full.jar org.onap.aaf.cadi.CmdLine" 
JAVA_AGENT="$JAVA -cp $CONFIG/bin/aaf-auth-cmd-*-full.jar -Dcadi_prop_files=$LOCAL/org.osaaf.aaf.props org.onap.aaf.cadi.configure.Agent" 
JAVA_AAFCLI="$JAVA -cp $CONFIG/bin/aaf-auth-cmd-*-full.jar -Dcadi_prop_files=$LOCAL/org.osaaf.aaf.props org.onap.aaf.auth.cmd.AAFcli" 

# If doesn't exist... still create
mkdir -p $OSAAF

# If not created by separate PV, create
mkdir -p /opt/app/aaf/status

# Temp use for clarity of code
FILE=

# Setup Bash, first time only
if [ ! -e "$HOME/.bash_aliases" ] || [ -z "$(grep cadi $HOME/.bash_aliases)" ]; then
  echo "alias cadi='$JAVA_CADI \$*'" >>$HOME/.bash_aliases
  echo "alias agent='$CONFIG/bin/agent.sh \$*'" >>$HOME/.bash_aliases
  echo "alias aafcli='$JAVA_AAFCLI \$*'" >>$HOME/.bash_aliases
  chmod a+x $CONFIG/bin/agent.sh
  . $HOME/.bash_aliases
fi

# Only load Identities once
# echo "Check Identities"
FILE="$DATA/identities.dat"
if [ ! -e $FILE ]; then
    mkdir -p $DATA
    cp $CONFIG/data/sample.identities.dat $FILE
    echo "Set Identities"
    INITIALIZED="true"
fi

# Load up Cert/X509 Artifacts
# echo "Check Signer Keyfile"
FILE="$LOCAL/org.osaaf.aaf.signer.p12"
if [ ! -e $FILE ]; then
    mkdir -p $LOCAL
    mkdir -p $PUBLIC
    if [ -e $CONFIG/cert/org.osaaf.aaf.signer.p12 ]; then
        cp $CONFIG/cert/org.osaaf.aaf.signer.p12 $FILE
        echo "Installed Signer P12"
        INITIALIZED="true"
    else
        echo "Decode"
        base64 -d $CONFIG/cert/demoONAPsigner.p12.b64 > $FILE
	base64 -d $CONFIG/cert/truststoreONAP.p12.b64 > $PUBLIC/truststoreONAP.p12 
	base64 -d $CONFIG/cert/truststoreONAPall.jks.b64 > $PUBLIC/truststoreONAPall.jks
	ln -s $PUBLIC/truststoreONAPall.jks $LOCAL
	cp $CONFIG/cert/AAF_RootCA.cer $PUBLIC
	CM_TRUST_CAS="$PUBLIC/AAF_RootCA.cer"
	echo "cadi_keystore_password=something easy" >> $CONFIG/local/aaf.props        
        echo "Setup ONAP Test CAs and Signers"
        INITIALIZED="true"
    fi
fi

# echo "Check keyfile"
FILE="$LOCAL/org.osaaf.aaf.p12"
if [ ! -e $FILE ]; then
    if [ -e $CONFIG/cert/org.osaaf.aaf.p12 ]; then
        cp $CONFIG/cert/org.osaaf.aaf.p12 $FILE
        echo "Installed AAF P12"
        INITIALIZED="true"
    else
        echo "Bootstrap Creation of Keystore from Signer"
        cd $CONFIG/CA
	
        # Remove this after Casablanca
	CADI_X509_ISSUERS="CN=intermediateCA_1, OU=OSAAF, O=ONAP, C=US:CN=intermediateCA_7, OU=OSAAF, O=ONAP, C=US"
	bash bootstrap.sh $LOCAL/org.osaaf.aaf.signer.p12 'something easy'
	cp aaf.bootstrap.p12 $FILE
	if [ -n "$CADI_X509_ISSUERS" ]; then
            CADI_X509_ISSUERS="$CADI_X509_ISSUERS:"
        fi
	BOOT_ISSUER="$(cat aaf.bootstrap.issuer)"
	CADI_X509_ISSUERS="$CADI_X509_ISSUERS$BOOT_ISSUER"

	I=${BOOT_ISSUER##CN=};I=${I%%,*}
        CM_CA_PASS="something easy"
        CM_CA_LOCAL="org.onap.aaf.auth.cm.ca.LocalCA,$LOCAL/org.osaaf.aaf.signer.p12;aaf_intermediate_9;enc:"
	CM_TRUST_CAS="$PUBLIC/AAF_RootCA.cer"
        echo "Generated ONAP Test AAF certs"
        INITIALIZED="true"
    fi
fi

# Only initialize once, automatically...
if [ ! -e $LOCAL/org.osaaf.aaf.props ]; then
    cp  $CONFIG/local/org.osaaf.aaf* $LOCAL
    for D in public etc logs; do
        mkdir -p $OSAAF/$D
        cp $CONFIG/$D/* $OSAAF/$D
    done

    TMP=$(mktemp)
    echo aaf_env=${AAF_ENV} >> ${TMP}
    echo aaf_release=${VERSION} >> ${TMP}
    echo cadi_latitude=${LATITUDE} >> ${TMP}
    echo cadi_longitude=${LONGITUDE} >> ${TMP}
    echo cadi_x509_issuers=${CADI_X509_ISSUERS} >> ${TMP}
    AAF_LOCATE_URL=${aaf_locate_url:="https://${HOSTNAME}:8095"}
    echo aaf_locate_url=${AAF_LOCATE_URL} >> ${TMP}
    for P in `env`; do
      if [[ "$P" == aaf_locator* ]]; then
	echo "$P" >> ${TMP}
        if [[ "$P" == aaf_locator_container=* ]]; then
	    echo aaf_locator_container.hostname=${HOSTNAME} >> ${TMP}
	fi
      fi
    done

    cat $TMP

    $JAVA_AGENT config \
	aaf@aaf.osaaf.org \
        cadi_etc_dir=$LOCAL \
        cadi_prop_files=$CONFIG/local/initialConfig.props:$CONFIG/local/aaf.props:${TMP}
    rm ${TMP}

    # Cassandra Config stuff
    # Default is expect a Cassandra on same Node
    CASS_HOST=${CASS_HOST:="aaf-cass"}
    CASS_PASS=$($JAVA_CADI digest "${CASSANDRA_PASSWORD:-cassandra}" $LOCAL/org.osaaf.aaf.keyfile)
    CASS_NAME=${CASS_HOST/:*/}
    sed -i.backup -e "s/\\(cassandra.clusters=\\).*/\\1${CASSANDRA_CLUSTERS:=$CASS_HOST}/" \
                  -e "s/\\(cassandra.clusters.user=\\).*/\\1${CASSANDRA_USER:=cassandra}/" \
                  -e "s/\\(cassandra.clusters.password=enc:\\).*/\\1$CASS_PASS/" \
                  -e "s/\\(cassandra.clusters.port=\\).*/\\1${CASSANDRA_PORT:=9042}/" \
                  $LOCAL/org.osaaf.aaf.cassandra.props

    if [ -n "$CM_CA_LOCAL" ]; then
      if [ -n "$CM_CA_PASS" ]; then
          CM_CA_LOCAL=$CM_CA_LOCAL$($JAVA_CADI digest "$CM_CA_PASS" $LOCAL/org.osaaf.aaf.keyfile)	
      fi
      # Move and copy method, rather than sed, because of slashes in CM_CA_LOCAL makes too complex
      FILE=$LOCAL/org.osaaf.aaf.cm.ca.props
      mv $FILE $FILE.backup
      grep -v "cm_ca.local=" $FILE.backup > $FILE
      echo "cm_ca.local=$CM_CA_LOCAL" >> $FILE
      echo "cm_trust_cas=$CM_TRUST_CAS" >> $FILE
    fi
    echo "Created AAF Initial Configurations"
    INITIALIZED="true"
fi


# Now run a command
CMD=$1
if [ -z "$CMD"  ]; then
    if [ -n "$INITIALIZED" ]; then
        echo "Initialization Complete"
    else
        echo "No Additional Initialization required"
    fi
else
    shift
    case "$CMD" in
    ls)
        echo ls requested
        find $OSAAF -depth
        ;;
    cat)
        if [ "$1" = "" ]; then
            echo "usage: cat <file... ONLY files ending in .props>"
        else
            if [[ $1 == *.props ]]; then
                echo
                echo "## CONTENTS OF $3"
                echo
                cat "$1"
            else
                echo "### ERROR ####"
                echo "   \"cat\" may only be used with files ending with \".props\""
            fi
        fi
        ;;
    validate)
        echo "## validate requested"
        $JAVA_AAFCLI perm list user aaf@aaf.osaaf.org
        ;;
    onap)
        #echo Initializing ONAP configurations.
	;;
    bash)
        shift
        cd $LOCAL || exit
        exec /bin/bash -c "$@"
        ;;
    setProp)
        cd $LOCAL || exit
        FILES=$(grep -l "$1" ./*.props)
	if [ -z "$FILES" ]; then 
  	    if [ -z "$3" ]; then
               FILES=org.osaaf.aaf.props
            else 
               FILES="$3"
            fi
	    ADD=Y
	fi
        for F in $FILES; do
	    if [ "$ADD" = "Y" ]; then
                echo "Changing $1 for $F"
		echo "$1=$2" >> $F
	    else 
               echo "Changing $1 in $F"
               sed -i.backup -e "s/\\(${1}=\\).*/\\1${2}/" $F
	    fi
            cat $F
        done
        ;;
    encrypt)
        cd $LOCAL || exit
	echo $1
        FILES=$(grep -l "$1" ./*.props)
	if [ "$FILES" = "" ]; then
             FILES=$LOCAL/org.osaaf.aaf.cred.props
	     ADD=Y
        fi
        for F in $FILES; do
            echo "Changing $1 in $F"
            if [ "$2" = "" ]; then
                read -r -p "Password (leave blank to cancel): " -s ORIG_PW
                echo " "
                if [ "$ORIG_PW" = "" ]; then
                    echo canceling...
                    break
                fi
            else
                ORIG_PW="$2"
            fi
            PWD=$($JAVA_CADI digest "$ORIG_PW" $LOCAL/org.osaaf.aaf.keyfile)
            if [ "$ADD" = "Y" ]; then
                  echo "$1=enc:$PWD" >> $F
            else 
            	sed -i.backup -e "s/\\($1.*enc:\\).*/\\1$PWD/" $F
	   fi
            cat $F
        done
        ;;
    taillog) 
	sh $OSAAF/logs/taillog
	;;
    wait)
	bash $CONFIG/bin/pod_wait.sh wait $1
        ;;
    --help | -?)
        case "$1" in
        "")
            echo "--- Agent Container Comands ---"
            echo "  ls                      - Lists all files in Configuration"
            echo "  cat <file.props>>       - Shows the contents (Prop files only)"
            echo "  validate                - Runs a test using Configuration"
            echo "  setProp <tag> [<value>] - set value on 'tag' (if no value, it will be queried from config)"
            echo "  encrypt <tag> [<pass>]  - set passwords on Configuration (if no pass, it will be queried)"
            echo "  bash                    - run bash in Container"
            echo "     Note: the following aliases are preset"
            echo "       cadi               - CADI CmdLine tool"
            echo "       agent              - Agent Java tool (see above help)"
            echo ""
            echo " --help|-? [cadi|agent]   - This help, cadi help or agent help"
            ;;
        cadi)
            echo "--- cadi Tool Comands ---"
            $JAVA_CADI 
            ;;
        agent)
            echo "--- agent Tool Comands ---"
            $JAVA_AGENT 
            ;;
        aafcli)
            echo "--- aafcli Tool Comands ---"
            $JAVA_AAFCLI 
            ;;
        esac
        echo ""
        ;;
    *)
        $JAVA_AGENT "$CMD" "$@" cadi_prop_files=$LOCAL/org.osaaf.aaf.props 
        ;;
    esac
fi
