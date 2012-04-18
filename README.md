<img src="http://lispnyc.org/static/images/theme-barsky-2.png" alt="lispnyc logo" title="LispNYC's homebase webapp" />

# LispNYC's Homebase Webapp

It's not perfect, contains Unix-isims and in some ways is suboptimal (see mail
handling), but works and is a growing work in progress.

## Requirements

In order to compile and run the project, either stand-alone or within
the Jetty webapp framework, it requires:

  * Java
  * Clojure
  * [leiningen](https://github.com/technomancy/leiningen)  (or Maven)
  * [lispnyc-appserver project](https://github.com/lispnyc/lispnyc/lispnyc-appserver)
  
Some resources from the [lispnyc-appserver project](https://github.com/lispnyc/lispnyc-appserver) project are used directly here, they're symlinked (or copied) over:

    homebase-data        -> lispnyc-appserver/homebase-data
    pebbleblog-articles  -> lispnyc-appserver/pebbleblog-articles
    html                 -> src/html
    src/html/static      -> lispnyc-appserver/homebase-static 

The *static* directory is copied directly from the lispnyc-appserver project because Jetty won't easily serve up symlinked directories.  Just keep that in mind so that HTML modifications don't get out of sync between the Standalone and WAR operation.

## Standalone Execution

There are two ways to run this, standalone or via a WAR deployment along with the other webapps.  Running standalone is quicker but you don't get access to the blog system, only the blog data.

It's pretty easy, just run:

    ./make-symlinks.sh # only run this once
    lein deps
    ./start

Look at [http://localhost:8000](http://localhost:8000)

## WAR Deployed Execution

First a caveat: we're having trouble running the webapp as an actual
war due to our SimpleCMS system and Clojure's compiled functions.  For
now we just expand the LispNYC homebase WAR in the webapp directory of
the lispnyc-appserver project. This is done by the *build-deploy.sh*
script.

    ./make-symlinks.sh # only run this once
    lein deps
    ./build-deploy.sh

Ensure the [lispnyc-appserver](https://github.com/lispnyc/lispnyc-appserver) webapp server is running and look at *http://localhost:8000*
