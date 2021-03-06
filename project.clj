(defproject org.lispnyc.webapp.homebase "3.3.1"
  :description "LispNYC's webserver and social integration homebase."
  :url         "https://github.com/LispNYC/lispnyc-homebase-app"
  :main org.lispnyc.webapp.homebase.core ; required for main
  :dependencies [
                 [org.clojars.clizzin/jsoup "1.5.1"] ; html2text
                 [org.clojure/clojure "1.7.0"]
                 [compojure "0.5.2"]
                 [ring/ring-jetty-adapter "0.3.5"]
                 [enlive "1.1.5"]
                 [ring/ring-servlet "0.3.5"]
                 [hiccup "0.3.1"]
                 ;[swank-clojure "1.3.0-SNAPSHOT"]
                 [clj-time "0.4.2"]
                 [twitter-api "0.7.6"]
                 [com.ecyrd/jspwiki "2.8.4"]
                 [org.clojure/core.memoize "0.5.1"]
                 [png-extract "1.0.4"]
                 [org.clojure/data.json "0.2.5"] ; facebook feed
                 [org.clojars.toxi/clj-json "0.5.0"] ; vimeo TODO: change

                 [org.clojars.gfodor/commons-lang "2.5"] ; wiki 
                 [org.clojars.aaroniba/log4j "1.2.16"]   ; wiki 
                 [javax.servlet.jsp "2.1.0.v201004190952"] ; wiki

                 [local/jdom "0.1"] ; wiki
                 [local/jaxen "0.1"] ; wiki
                 [local/oscache "0.1"] ; wiki
                 [local/oro "0.1"] ; wiki
                 [local/ecs "0.1"] ; wiki
                 [local/jrcs-diff "0.1"] ; wiki
                 [local/lucene "0.1"] ; wiki
                 [local/lucene-highlighter "0.1"] ; wiki
                 [local/jsonrpc "1.0"] ; wiki
                 [local/freshcookies-security "0.60"] ; wiki

                 [javax.mail.glassfish "1.4.1.v201005082020"] ; wiki, pebble
                 [pebble "2.5.3"]          ; pebble 
                 [acegi-security "1.0.6"]  ; pebble 
                 [radeox "1.0-b2" ]        ; pebble 
                 [org.clojure/tools.nrepl "0.2.2"] ; debug
                 [clojure-complete "0.2.2"] ; debug, tab completion
                 ]
  :plugins [[lein-ring "0.8.3"]      ; build wars
            [lein-localrepo "0.5.3"] ; access 3rd party jars
            ]
;  :dev-dependencies [[ring/ring-jetty-adapter "0.3.5"]
;                     [ring/ring-devel "0.3.5"]
;                     ; [swank-clojure "1.3.0-SNAPSHOT"]
;                     ; [uk.org.alienscience/leiningen-war "0.0.12"]
;                     ; [lein-run "1.0.1-SNAPSHOT"]
;                     ]
  ;:aot [org.lispnyc.webapp.homebase.servlet]   ;; servlet class compiled as a java web server
  ;:war {:name "home.war"} ;; resulting war name
  :disable-implicit-clean true ;; don't nuke lib directory
  :ring { :handler org.lispnyc.webapp.homebase.core/app-routes
          ; :servlet-class org.lispnyc.webapp.homebase.servlet
         } 
  :jvm-opts ["-Dhttp.agent=curl/7.27.0"] 
  )

