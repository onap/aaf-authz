#!/bin/bash
#
# A Script for use in Pods... Check for status files, and validate before moving on.
#
DIR="/opt/app/aaf/status"
APP=$1
shift
OTHER=$1
shift

function status {
  if [ -d "$DIR" ]; then
     echo "$@" > $DIR/$APP
  fi
}


function check {
  if [ -d "$DIR" ]; then
    if [ -e "$DIR/$OTHER" ]; then
      echo "$(cat $DIR/$OTHER)"
    else 
      echo "$DIR/$OTHER does not exist"
    fi
  else 
    echo "$DIR does not exist"
  fi
}

function wait {
  n=0
  while [ $n -lt 40  ]; do 
     rv="$(check)"
     echo "$rv"
     if [ "$rv" = "ready" ]; then
       echo "$OTHER is $rv"
       n=10000
     else 
       (( ++n )) 
       echo "Sleep 10 (iteration $n)"
       sleep 10
     fi
  done
}

function start {
  n=0
  while [ $n -lt 40  ]; do 
     rv="$(check)"
     echo "$OTHER is $rv"
     if [ "$rv" = "ready" ]; then
       # This is critical.  Until status is literally "ready" in the status directory, no processes will start
       status ready
       echo "Starting $@"
       n=10000
     else 
       (( ++n )) 
       echo "Sleep 10 (iteration $n)"
       sleep 10
     fi
  done
}

case "$OTHER" in
  sleep)
    echo "Sleeping $1"
    status "Sleeping $1"
    sleep $1
    shift
    status "ready"
    echo "Done"
    ;;
  wait)
    OTHER="$1"
    shift    
    wait
    ;;
  *)
    echo "App $APP is waiting to start until $OTHER is ready"
    status "waiting for $OTHER"
  
    start
  ;;
esac  

eval "$@"
