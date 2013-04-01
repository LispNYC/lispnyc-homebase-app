(defproject org.lispnyc.webapp.homebase "3.1.0"
  :description "LispNYC's webserver and social integration homebase."
  :run-aliases {:server [org.lispnyc.webapp.homebase.core start-server "localhost" "8080"]}   ;; 2011-01-06 currently broken with lein 1.4.2
  :main org.lispnyc.webapp.homebase.core ; required for main
  :dependencies [
                 [org.clojars.clizzin/jsoup "1.5.1"] ; html2text
                 [org.clojure/clojure "1.3.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.5.2"]
                 [ring/ring-jetty-adapter "0.3.5"]
                 [enlive "1.0.0-SNAPSHOT"]
                 [ring/ring-servlet "0.3.5"]
                 [hiccup "0.3.1"]
                 [swank-clojure "1.3.0-SNAPSHOT"]
                 [clj-time "0.4.2"]
                 [clojure-twitter "1.2.6-SNAPSHOT"] 
                 [com.ecyrd/jspwiki "2.8.4"]
                 [org.clojure/core.memoize "0.5.1"]
                 [png-extract "1.0.3"]
                 [org.clojars.toxi/clj-json "0.5.0"]
                 [org.clojars.gfodor/commons-lang "2.5"] ; wiki 
                 [org.clojars.aaroniba/log4j "1.2.16"]   ; wiki 
                 [javax.servlet.jsp "2.1.0.v201004190952"] ; wiki
                 [javax.mail.glassfish "1.4.1.v201005082020"] ; wiki, pebble
                 [pebble "2.5.3"]          ; pebble 
                 [acegi-security "1.0.6"]  ; pebble 
                 [radeox "1.0-b2" ]        ; pebble 
                 [org.clojure/tools.nrepl "0.2.2"] ; debug
                 [clojure-complete "0.2.2"] ; debug, tab completion
                 ]
  :dev-dependencies [[ring/ring-jetty-adapter "0.3.5"]
                     [ring/ring-devel "0.3.5"]
                     [swank-clojure "1.3.0-SNAPSHOT"]
                     [uk.org.alienscience/leiningen-war "0.0.12"]
                     [lein-run "1.0.1-SNAPSHOT"]]
  ;:aot [org.lispnyc.webapp.homebase.servlet]   ;; servlet class compiled as a java web server
  ;:war {:name "home.war"} ;; resulting war name
  :disable-implicit-clean true ;; don't nuke lib directory
  :ring { :handler       org.lispnyc.webapp.homebase.core/app-routes
          ; :servlet-class org.lispnyc.webapp.homebase.servlet
         } 
  )

