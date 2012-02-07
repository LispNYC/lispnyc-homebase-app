#!/bin/bash

if [ -d "../lispnyc-jetty" ]; then
  echo "found lispnyc-jetty project"
else
  echo "download the lispnyc-jetty project from github"
  exit 1
fi

lein uberwar

cp home.war ../lispnyc-jetty/webapps/
rm -rf ../lispnyc-jetty/webapps/home 2>/dev/null
mkdir ../lispnyc-jetty/webapps/home
pushd ../lispnyc-jetty/webapps/home
jar -xf ../home.war 
rm ../home.war
echo "deployed in ../lispnyc-jetty/webapps"
popd 
echo "make sure ../lispnyc-jetty is running"