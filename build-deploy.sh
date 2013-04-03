#!/bin/bash

if [ -d "../lispnyc-appserver" ]; then
  echo "found lispnyc-appserver project"
else
  echo "download the lispnyc-appserver project from github"
  exit 1
fi

LEIN_SNAPSHOTS_IN_RELEASE=true lein ring uberwar

cp target/org.lispnyc.webapp.homebase-*.war ../lispnyc-appserver/webapps/home.war

# deploy by hand in webapps/home
rm -rf ../lispnyc-appserver/webapps/home 2>/dev/null
mkdir ../lispnyc-appserver/webapps/home
pushd ../lispnyc-appserver/webapps/home
jar -xf ../home.war 
rm ../home.war
popd 

echo "deployed in ../lispnyc-appserver/webapps/home"
echo "make sure ../lispnyc-appserver is running"
