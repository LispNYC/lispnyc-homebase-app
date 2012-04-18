(ns org.lispnyc.webapp.homebase.core
  (:require [org.lispnyc.webapp.homebase.simplecms        :as cms]
            [org.lispnyc.webapp.homebase.feed.pebble-blog :as pebble]
            [org.lispnyc.webapp.homebase.feed.meetup      :as meetup]
            [org.lispnyc.webapp.homebase.feed.wiki        :as wiki]
            [org.lispnyc.webapp.homebase.news             :as news]
            [hiccup.core                                  :as html]
            [bcc.markdown                                 :as md]
            [ring.adapter.jetty                           :as jetty]
            [ring.middleware.cookies                      :as cookies]
            [compojure.core                               :as ww]
            [compojure.route                              :as route]
            [net.cgrand.enlive-html                       :as enlive]
            [clojure.contrib.shell-out                    :as shell]
            [clj-time.core                                :as tc]
            [clj-time.format                              :as tf]
            [swank.swank])
  (:import  [java.io]
            [java.util]
            [com.ecyrd.jspwiki])
  (:gen-class)) ; required for main

(def homebase-data-dir "homebase-data/")
(def idea-file (str homebase-data-dir "soc-2011.ideas.txt"))
(def rsvp-file (str homebase-data-dir "rsvp-meeting.txt"))

(defn make-saying []
  (let [sayings [
                 "LispNYC: Providing parenthesis to New York since 2002"
                 "\"A language that doesn't affect the way you think about programming, is not worth knowing.\" - Alan Perlis"
                 "\"Like DNA, such a language [Lisp] does not go out of style.\" - Paul Graham, ANSI Common Lisp"
                 "\"Only the creatively intelligent can prosper in the Lisp world.\" - Richard Gabriel"
                 "\"The greatest single programming language ever designed.\" - Alan Kay (about Lisp)"
                 "\"Lisp is a programmable programming language.\" - John Foderaro"
                 "\"Will write code that writes code that writes code that writes code for money.\" - on comp.lang.lisp"
                 "\"Lisp is a language for doing what you've been told is impossible.\" - Kent Pitman"
                 ]]
    (nth sayings (rand (count sayings))) ))

;;
;; templates
;;

(defn template-index "Associate the announcemet, meeting and recent blog entry with the html."
   [announcement meeting blog]
  (enlive/template
   "html/template-index.html" [] ;; src and args
   ;; metadata is tricky
   [(and (enlive/has [:meta]) (enlive/attr-has :name "description"))] (enlive/set-attr :content (:title meeting))
   [(and (enlive/has [:meta]) (enlive/attr-has :name "keywords"))]    (enlive/set-attr :content (:title meeting)) 
   [(and (enlive/has [:meta]) (enlive/attr-has :name "author"))]      (enlive/set-attr :content "Heow Goodman") 

   ;; announcement
   [:span.announcementHeader] (enlive/html-content (:title announcement))
   [:p.announcementContent]   (enlive/html-content (str (:content announcement)))

   ;; meeting
   [:title]              (enlive/content (str "New York City Lisp User Group: " (:title meeting)))
   [:span.meetingHeader] (enlive/html-content (str "meeting - " (:time meeting) " - <i>" (:title meeting) "</i>"))
   [:p.meetingContent]   (enlive/html-content (str (:description meeting) "<p>Location:<br>"(:venue meeting) "<br>" (:address meeting) "<br>" (:address2 meeting)
                                                   (if (.contains (:venue meeting) "Google")
                                                     "<br><br>You <a href=\"/meeting/rsvp\">must RSVP here</a> or at <a target=\"_blank\" href=\"http://www.meetup.com/LispNYC/\">Meetup</a>" "")
                                                   "</p>"))

   ;; blog entry
   [:span.blogHeader] (enlive/html-content (str "latest blog - <i>" (:title blog) "</i> by " (:author blog)))
   [:p.blogContent]   (enlive/html-content (str (:content blog) "<p><a href=\"/blog/\">more articles by LispNYC members</a></p>"))

   ;; witty saying
   [:div#footerLeft]     (enlive/html-content (str "&nbsp;&nbsp;" (make-saying)))   
   ))

(defn template-article "Associate the article data with the html selecting template based on article section."
  [article]
  (enlive/template
   (str "html/template-" ;; default to home template
        (if (= 0 (count (:section article))) "home" (:section article))
        ".html") []
   ;; metadata is tricky
   [(and (enlive/has [:meta]) (enlive/attr-has :name "description"))] (enlive/set-attr :content (:description article))
   [(and (enlive/has [:meta]) (enlive/attr-has :name "keywords"))]    (enlive/set-attr :content (apply str (interpose "," (:tags article)))) ; list to readable string
   [(and (enlive/has [:meta]) (enlive/attr-has :name "author"))]      (enlive/set-attr :content (:author article))
   
   [:title]              (enlive/content (str "New York City Lisp User Group: " (:title article)))
   [:content]            (enlive/content (:description article))
   [:span.meetingHeader] (enlive/content (:title article))
   [:p.meetingContent]   (enlive/html-content
                          (md/markdown-to-html (:content article)))
   
   [:div#footerLeft]     (enlive/html-content (str "&nbsp;&nbsp;" (make-saying)))
   ))

(defn template-wiki "do the same with the wiki data"
  [wiki-article]
  (enlive/template "html/template-home.html" []
   [:title]              (enlive/content (str "New York City Lisp User Group: " (:title wiki-article)))
   [:content]            (enlive/html-content (:content wiki-article))
   [:span.meetingHeader] (enlive/content (:title wiki-article))
   [:p.meetingContent]   (enlive/html-content (:content wiki-article))
   
   [:div#footerLeft]     (enlive/html-content (str "&nbsp;&nbsp;" (make-saying)))
   ))

;;
;; pages
;;
(def pubpath (cms/publish-path (str homebase-data-dir "simplecms-articles") #".*\.txt" template-article))

(defn debug-page []
  (future (swank.swank/start-repl))
  "Debugging started on localhost, swank on in to :4005 kind sir.")

(defn get-title [item]
  (if (empty? (:user item)) ; add user to title if there is one
    (:title item)
    (str (:user item) ": " (:title item))))

(defn htmlify-news [items]
  (html/html
     [:table (map #(html/html
                    [:tr
                     [:td [:img {:src (str "/static/images/icon-"
                                            (apply str (rest (str (:type %))))
                                            "-32.png")}] ]
                     ;; link the title if there are no embedded links
                     [:td (if (.contains (get-title %) "href")
                            (get-title %)
                            [:a {:href (:link %) :target "_blank"} (get-title %)])]]) items)]) )

(defn incstr [str-value]
  (try
    (let [v (+ 1 (Integer/parseInt str-value))]
      (if (<= v 6) v 1)) 
    (catch java.lang.NumberFormatException _ 1)))

(defn news-title [visit]
  (cond (= 1 visit) "LispNYC-centric"
        (= 2 visit) "less LispNYC, more external sources"
        (= 3 visit) "mostly by merrit"
        (= 4 visit) "all news by relative merrit"
        (= 5 visit) "less by merrit, more by date"
        (= 6 visit) "most up to date news"
        :else "All the news that fits, we print."
        ))

(defn news-page [cookies]
  {
   :cookies { "visits" (str (incstr (:value (cookies "visits")))) }
   :body (let [visits  (incstr (:value (cookies "visits")))
               content (take 30 (news/fetch visits))
               html    (htmlify-news content)]
           ((template-wiki {:title (str "visit: " visits " - " (news-title visits)) :content html})) )
   })
  
;;
;; processing
;; 

(defn validate-input "Only keep specific characters in the input string."
     [input-str]
     (let [re (re-pattern "[a-zA-Z0-9\\-\\_\\ \\.\\!\\?\\@]")]
       (apply str (filter #(re-matches re (str %1)) input-str))))

(defn map->mailstr
  "Given a parameter map, stringify it all parameters formatted as a mail form.  We used to call this FormMail and it's not used for a reason."
  [params]
  (apply str
         (interpose "\n"
              (map #(str (first %1) ": " (validate-input (second %1))) params))))

(defn mail-rsvp [params] ;; TODO: merge
  (let [msg (map->mailstr params)
        cmd (str "/bin/echo '" msg "' | /usr/bin/mail request@lispnyc.org -s '" (validate-input (params "subject")) "'")]
    (shell/sh "/bin/sh" "-c" cmd)
    "<html><meta http-equiv=\"REFRESH\" content=\"0;url=/meeting/rsvp-thanks\"></HEAD></html>" ))

(defn mail-contact [params] ;; TODO: merge
  (if (empty? (params "jobtitle")) ; linkbait
    (let [msg (map->mailstr params)
          cmd (str "/bin/echo '" msg "' | /usr/bin/mail request@lispnyc.org -s '" (validate-input (params "subject")) "'")]
      (shell/sh "/bin/sh" "-c" cmd)
      "<html><meta http-equiv=\"REFRESH\" content=\"0;url=/contact-thanks\"></HEAD></html>" )))

(defn mail-blog-signup [params] ;; TODO: merge
  (if (empty? (params "jobtitle")) ; linkbait
    (let [msg (map->mailstr params)
          cmd (str "/bin/echo '" msg "' | /usr/bin/mail request@lispnyc.org -s 'blog request " (tf/unparse (tf/formatters :basic-date) (tc/now)) "'" )]
      (shell/sh "/bin/sh" "-c" cmd)
      "<html><meta http-equiv=\"REFRESH\" content=\"0;url=/blog-thanks\"></HEAD></html>" )))

(defn mail-idea [params] ;; TODO: merge
  (let [msg (map->mailstr params)
        cmd (str "/bin/echo '" msg "' | /usr/bin/mail management@lispnyc.org -s 'soc 2011 idea'")]
    (shell/sh "/bin/sh" "-c" cmd)
    "<html><meta http-equiv=\"REFRESH\" content=\"0;url=/soc/thanks\"></HEAD></html>" ))

(comment
  ;; TODO: use Java or pebble mail, /usr/bin/mail isn't a valid long-term API
  (defn mail-page []
    (let [session (. net.sourceforge.pebble.util.MailUtils createSession)
          blog    (make-blog "/home/heow/pebble/blogs/heow/")
          config  (new net.sourceforge.pebble.Configuration)
          context (. net.sourceforge.pebble.PebbleContext getInstance)
          ]
      (. config setDataDirectory "/home/heow/pebble") 
      (. config setSmtpHost "localhost") ; just to make sure
      (. context setConfiguration config) ; config mail
      (. net.sourceforge.pebble.util.MailUtils sendMail session blog ["heow@localhost"] "webapp mail test" "foo")
      "mail sent")))

(defn persist-form! [form data-file]
  (dosync
   (with-open [f (java.io.FileWriter. data-file true)]
     (.write f (prn-str form)))))

(defn process-form [form mail-fn data-file]
  (persist-form! form data-file)
  (mail-fn form))

(defn process-wiki-or-404
  "determine and dispatch on wiki topic, or it's a 404"
  [request]
  (let [uri      (:uri request)
        topic    (apply str (rest uri)) ; strip leading slash
        wikipage (wiki/fetch-wikipage topic)]
    (if (empty? (:content wikipage))
      "404 page not found"
      ((template-wiki wikipage)))))

;;
;; jetty routes
;;
(ww/defroutes app-routes
  ;; avoid caching, no vars
  (ww/GET  "/" []     ((template-index (wiki/fetch-wikipage "front-page") (meetup/fetch-meetup) (first (pebble/fetch-blogs)))))
  (ww/GET  "/home" [] ((template-index (pebble/fetch-announcement) (meetup/fetch-meetup) (first (pebble/fetch-blogs)))))

  ;; This seriously makes me cry, call 917-825-2382 if you'd like to have
  ;; a nice long conversation about compling Clojoure functions.
  ;; Pour yourself a drink first.
  ;; unable to ues (publish-path "data" #".*\.txt") in a WAR
  (nth pubpath 0) (nth pubpath 1) (nth pubpath 2) (nth pubpath 3) (nth pubpath 4) (nth pubpath 5) (nth pubpath 6) (nth pubpath 7) (nth pubpath 8) (nth pubpath 9) (nth pubpath 10) (nth pubpath 11) (nth pubpath 12) (nth pubpath 13) (nth pubpath 14) (nth pubpath 15) (nth pubpath 16) (nth pubpath 17) (nth pubpath 18) (nth pubpath 19) (nth pubpath 20)
  
  (ww/GET          "/debug" [] (debug-page))
  (ww/GET          "/news" {cookies :cookies} (news-page cookies))
  ;; (ww/GET          "/mail"  [] (mail-page))

  (ww/POST         "/soc/idea"      {params :params} (process-form params mail-idea idea-file))
  (ww/POST         "/blog-signup"   {params :params} (mail-blog-signup params))
  (ww/POST         "/meeting/rsvp"  {params :params} (process-form params mail-rsvp rsvp-file))
  (ww/POST         "/meetings/rsvp" {params :params} (process-form params mail-rsvp rsvp-file))
  (ww/POST         "/contact"       {params :params} (mail-contact params))
  
  (route/files     "/" {:root "html/"})  ; not used during WAR deployment

  (ww/GET          "/*" {params :params :as request} (process-wiki-or-404 request))
  )

;;
;; WAR servlet mechanics
;; 
(def context-path (atom nil)) ;; This caches the context path of the servlet

(defn- get-context-path "Returns the context path when running as a servlet"
  ([] @context-path)
  ([servlet-req]
     (if (nil? @context-path)
       (reset! context-path (.getContextPath servlet-req)))
     @context-path))

(defn wrap-context "Removes the deployed servlet context from a URI when running as a deployed web application"
  [handler]
  (fn [request]
    (if-let [servlet-req (:servlet-request request)]      
      (let [context (get-context-path servlet-req)
            uri (:uri request)]

        (if (nil? @wiki/servlet-context)
          (reset! wiki/servlet-context (:servlet-context request)))
        
        (if (.startsWith uri context)
          (handler (assoc request :uri
                          (.substring uri (.length context))))
          (handler request)))
      (handler request))))

;; note: we're not wrapping keyword-params, using a string is good for our form processing
(ww/wrap! app-routes wrap-context cookies/wrap-cookies)

;;
;; standalone Jetty server, not used during WAR deployment
;;
(defn start-server [host port]
  (println "starting Jetty on " host ":" port)
  (if (string? port) (start-server host (Integer/parseInt port)) ; rerun as int
      (future (jetty/run-jetty app-routes {:host host :port port})) )) ; don't wrap keyword params

(defn -main
  "For use in standalone operation."
  [& args]
  (if (nil? args)
    (start-server "localhost" "8000")
    (start-server (first args) (first (rest args))) ))
