#!/bin/bash

if [ -d "../lispnyc-appserver" ]; then
  echo "found lispnyc-appserver project"
else
  echo "download the lispnyc-appserver project from github and put it"
  echo "as a peer directory here: ../lispnyc-appserver"
  exit 1
fi

if [ -h "pebbleblog-articles" ]; then
  echo "only run this script once!  exiting"
  exit 1
fi

ln -s $PWD/../lispnyc-appserver/pebbleblog-articles .
ln -s $PWD/../lispnyc-appserver/etc .
ln -s $PWD/src/html .

# ok I lied, Jetty dosen't like symlinks for routed files, we'll copy them over
# remember to keep them in sync and out of github
echo "NOTE: changes made to src/html/static need to be mirrored in ../lispnyc-appserver/homebase-static"
mkdir src/html/
cp -vr $PWD/../lispnyc-appserver/homebase-static src/html/static
ln -s src/html/static homebase-static

echo "forcing some build deps not in the maven or clojar repos..."

#
# from jetty
#
mvn install:install-file -DgroupId=javax.mail.glassfish -DartifactId=javax.mail.glassfish -Dversion=1.4.1.v201005082020 -Dpackaging=jar -Dfile=../lispnyc-appserver/lib/jndi/javax.mail.glassfish_1.4.1.v201005082020.jar
mvn install:install-file -DgroupId=org.mortbay.jetty    -DartifactId=servlet-api-2.5      -Dversion=2.5.0.v200910301333 -Dpackaging=jar -Dfile=../lispnyc-appserver/lib/servlet-api-2.5.jar
mvn install:install-file -DgroupId=javax.servlet.jsp    -DartifactId=javax.servlet.jsp    -Dversion=2.1.0.v201004190952 -Dpackaging=jar -Dfile=../lispnyc-appserver/lib/jsp/javax.servlet.jsp_2.1.0.v201004190952.jar

#
# from pebble jar
#
mkdir tmp
pushd tmp
jar -xvf ../../lispnyc-appserver/webapps/blog.war WEB-INF/lib/

mvn install:install-file -DgroupId=pebble -DartifactId=pebble -Dversion=2.5.3 -Dpackaging=jar -Dfile=WEB-INF/lib/pebble-2.5.3.jar 
mvn install:install-file -DgroupId=acegi-security -DartifactId=acegi-security -Dversion=1.0.6 -Dpackaging=jar -Dfile=WEB-INF/lib/acegi-security-1.0.6.jar

popd
rm -rf tmp

#
# from jsp wiki
#
mkdir tmp
pushd tmp
jar -xvf ../../lispnyc-appserver/webapps/wiki.war WEB-INF/lib

# TODO: use localrepo everwhere
# stuff into local maven
lein localrepo install WEB-INF/lib/jdom.jar                       local/jdom 0.1
lein localrepo install WEB-INF/lib/jaxen.jar                      local/jaxen 0.1
lein localrepo install WEB-INF/lib/oscache.jar                    local/oscache 0.1
lein localrepo install WEB-INF/lib/oro.jar                        local/oro 0.1
lein localrepo install WEB-INF/lib/ecs.jar                        local/ecs 0.1
lein localrepo install WEB-INF/lib/jrcs-diff.jar                  local/jrcs-diff 0.1
lein localrepo install WEB-INF/lib/lucene.jar                     local/lucene 0.1
lein localrepo install WEB-INF/lib/lucene-highlighter.jar         local/lucene-highlighter 0.1
lein localrepo install WEB-INF/lib/jsonrpc-1.0.jar                local/jsonrpc 1.0
lein localrepo install WEB-INF/lib/freshcookies-security-0.60.jar local/freshcookies-security 0.60

popd
rm -rf tmp
