#!/bin/bash

if [ -d "../lispnyc-appserver" ]; then
  echo "found lispnyc-appserver project"
else
  echo "download the lispnyc-appserver project from github"
  exit 1
fi

lein uberwar

cp home.war ../lispnyc-appserver/webapps/
rm -rf ../lispnyc-appserver/webapps/home 2>/dev/null
mkdir ../lispnyc-appserver/webapps/home
pushd ../lispnyc-appserver/webapps/home
jar -xf ../home.war 
rm ../home.war
popd 
echo "deployed in ../lispnyc-appserver/webapps"
echo "make sure ../lispnyc-appserver is running"
