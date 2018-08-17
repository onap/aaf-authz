#
# Initialize an Intermediate CA Cert.  
#
  if [ -e intermediate.serial ]; then
    ((SERIAL=`cat intermediate.serial` + 1))
  else
    SERIAL=1
  fi
  echo $SERIAL > intermediate.serial
DIR=intermediate_$SERIAL

mkdir -p $DIR/private $DIR/certs $DIR/newcerts
chmod 700 $DIR/private
chmod 755 $DIR/certs $DIR/newcerts
touch $DIR/index.txt
echo "unique_subject = no" > $DIR/index.txt.attr

if [ ! -e $DIR/serial ]; then
  echo '01' > $DIR/serial
fi
cp manual.sh p12.sh subject.aaf cfg.pkcs11 p11.sh $DIR

if [  "$1" == "" ]; then
  CN=intermediateCA_$SERIAL
else
  CN=$1
fi

SUBJECT="/CN=$CN`cat subject.aaf`"
echo $SUBJECT
  echo "IMPORTANT: If for any reason, you kill this process, type 'stty sane'"
  echo "Enter the PassPhrase for the Key for $CN: "
  `stty -echo`
  read PASSPHRASE
  `stty echo`
 
  # Create a regaular rsa encrypted key
  openssl req -new -newkey rsa:2048 -sha256 -keyout $DIR/private/ca.key \
	   -out $DIR/$CN.csr -outform PEM -subj "$SUBJECT" \
	   -passout stdin  << EOF
$PASSPHRASE
EOF

  chmod 400 $DIR/private/ca.key
  openssl req -verify -text -noout -in $DIR/$CN.csr

  # Sign it
  openssl ca -config openssl.conf -extensions v3_intermediate_ca \
	-days 1826 \
  -cert certs/ca.crt -keyfile private/ca.key -out $DIR/certs/ca.crt \
	-infiles $DIR/$CN.csr

   openssl x509 -text -noout -in $DIR/certs/ca.crt

   openssl verify -CAfile certs/ca.crt $DIR/certs/ca.crt


# Create a Signer p12 script
echo openssl pkcs12 -export -name aaf_$DIR \
               -in certs/ca.crt -inkey private/ca.key \
               -out aaf_$DIR.p12 >> $DIR/signerP12.sh

