<img src="http://lispnyc.org/static/images/theme-barsky-2.png" alt="lispnyc logo" title="LispNYC's homebase webapp" />

# LispNYC's Homebase Webapp

It's not perfect, contains Unix-isims, is a growing work in progress and called
home by many Lispnycs.

## Requirements

In order to compile and run the project, either stand-alone or within
the Jetty webapp framework, it requires:

  * Java - OpenJDK or the official Sun/Oracle
  * Clojure
  * [leiningen](https://github.com/technomancy/leiningen)
  * [Maven](http://maven.apache.org/) - version in Linux distros is fine
  * [lispnyc-appserver project](https://github.com/lispnyc/lispnyc-appserver)
  
Some resources from the [lispnyc-appserver project](https://github.com/lispnyc/lispnyc-appserver) project are used directly here, they're symlinked (or copied) over:

    pebbleblog-articles  -> lispnyc-appserver/pebbleblog-articles
    html                 -> src/html
    src/html/static      -> lispnyc-appserver/homebase-static 

The *static* directory is copied directly from the lispnyc-appserver project because Jetty won't easily serve up symlinked directories.  Just keep that in mind so that HTML modifications don't get out of sync between the Standalone and WAR operation.

## Standalone Execution

There are two ways to run this, standalone or via a WAR deployment along with the other webapps.  Running standalone is quicker but you don't get access to the wiki CMS or blog system, only the blog data.

It's pretty easy, just run:

    ./make-symlinks.sh # only run this once
    lein deps
    ./start

Look at [http://localhost:8000](http://localhost:8000)

## WAR Deployed Execution

Because our homebase webapp (home.war) acts as the default application, it can't just be dropped into webapps after Jetty is running.  Therefore home.war sould be expanded into webapps/home when Jetty is initialized.  The *build-deploy.sh* script is designed to make that process easier.

    ./make-symlinks.sh # run this only once
    lein deps
    ./build-deploy.sh

Ensure the [lispnyc-appserver](https://github.com/lispnyc/lispnyc-appserver) webapp server is running and poin your browser to *http://localhost:8000*
