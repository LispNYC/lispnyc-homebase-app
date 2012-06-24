(ns org.lispnyc.webapp.homebase.core
  (:require [org.lispnyc.webapp.homebase.feed.pebble-blog :as pebble]
            [org.lispnyc.webapp.homebase.feed.meetup      :as meetup]
            [org.lispnyc.webapp.homebase.feed.vimeo       :as vimeo]
            [org.lispnyc.webapp.homebase.feed.wiki        :as wiki]
            [org.lispnyc.webapp.homebase.feed.util        :as util] 
            [org.lispnyc.webapp.homebase.news             :as news]
            [clj-time.core                                :as time]
            [clj-time.format                              :as tformat]
            [clj-time.local                               :as tlocal]
            [clojure.core.memoize                         :as memo]
            [hiccup.core                                  :as html]
            [ring.adapter.jetty                           :as jetty]
            [ring.middleware.cookies                      :as cookies]
            [ring.middleware.params                       :as params]
            [compojure.core                               :as ww]
            [compojure.route                              :as route]
            [net.cgrand.enlive-html                       :as enlive]
            [clojure.contrib.shell-out                    :as shell]
            [swank.swank])
  (:import  [java.io]
            [java.util]
            [com.ecyrd.jspwiki]
            [extract.PNGExtractText])
  (:gen-class)) ; required for main

(def homebase-data-dir "homebase-data/")
(def idea-file (str homebase-data-dir "soc-2011.ideas.txt"))
(def rsvp-file (str homebase-data-dir "rsvp-meeting.txt"))

(defn make-saying []
  (rand-nth ["LispNYC: Providing parenthesis to New York since 2002"
             "\"A language that doesn't affect the way you think about programming, is not worth knowing.\" - Alan Perlis"
             "\"Like DNA, such a language [Lisp] does not go out of style.\" - Paul Graham, ANSI Common Lisp"
             "\"Only the creatively intelligent can prosper in the Lisp world.\" - Richard Gabriel"
             "\"The greatest single programming language ever designed.\" - Alan Kay (about Lisp)"
             "\"Lisp is a programmable programming language.\" - John Foderaro"
             "\"Will write code that writes code that writes code that writes code for money.\" - on comp.lang.lisp"
             "\"Lisp is a language for doing what you've been told is impossible.\" - Kent Pitman"
             ]))

(defn make-ad
  "Randomly select a PNG file from the ad directory, comment is url"
  []
  (let [adpath "static/images/ads/"
        file   (rand-nth
                (filter #(.endsWith (.getName %) ".png")
                        (file-seq (clojure.java.io/file (str "./homebase-" adpath)))))
        path   (str adpath (.getName file)) 
        url    (extract.PNGExtractText/getComment
                (new java.io.FileInputStream file))]
    {:path path
     :url  url
     } ))

(def core-cache
  {:meetup       (atom [])
   :past-meetups (atom [])
   :videos       (atom []) })

(def do-fetch-meetup       (memo/memo-ttl #(meetup/fetch-meetup) (news/in-min 10)))
(def do-fetch-past-meetups (memo/memo-ttl #(meetup/fetch-past-meetups) (news/in-min 10)))
(def do-fetch-videos       (memo/memo-ttl #(vimeo/fetch-videos) (news/in-min 10)))

;; see news for async/error details
;; TODO: macroify
(defn fetch-meetup []
  (.start (Thread. #(news/set-data! (do-fetch-meetup) core-cache :meetup)))
  (deref (:meetup core-cache)))

(defn fetch-past-meetups []
  (.start (Thread. #(news/set-data! (do-fetch-past-meetups) core-cache :past-meetups)))
  (deref (:past-meetups core-cache)))

(defn fetch-videos []
  (.start (Thread. #(news/set-data! (do-fetch-videos) core-cache :videos)))
  (deref (:videos core-cache)))

(defn make-news-title [item]
  (if (empty? (:user item)) ; add user to title if there is one
    (:title item)
    (str (:user item) ": " (:title item))))

(def max-news-pages 6)

(defn news-title [visit]
  (cond (= 1 visit) "LispNYC News"
        (= 2 visit) "Lisp News: with more planetary sources"
        (= 3 visit) "Lisp News: weighed mostly by merrit"
        (= 4 visit) "Lisp News: all news by relative merrit"
        (= 5 visit) "Lisp News: weighed less by merrit, more by date"
        (= 6 visit) "Lisp News: most up to date news"
        :else "All the news that fits, we print."
        ))

(defn news-pager [visit]
  (html/html
   (if (> visit 1) [:a {:class "pager-better" :href (str "/news?p=" (max 1 (- visit 1)))} "&lt; better "])
   (map #(vec (if (= visit %) (list :span { :class "pager-current"} (str " " visit " "))
                  (list :a {:class "pager-page" :href (str "/news?p=" %)} (str " " % " ")) ))
        (range 1 (+ 1 max-news-pages)))
   (if (< visit max-news-pages) [:a {:class "pager-newer" :href (str "/news?p=" (min max-news-pages (+ visit 1)))} " newer &gt;"]) ))

(defn htmlify-news
  ([items]      (htmlify-news items 0    true))
  ([items page] (htmlify-news items page true))
  ([items page pager?]
     (html/html
      [:table {:id "news" }(map #(html/html
                     [:tr
                      [:td [:img {:src (str "/static/images/icon-"
                                            (apply str (rest (str (:type %))))
                                            "-32.png")}] ]
                      ;; link the title if there are no embedded links
                      [:td (if (.contains (make-news-title %) "href")
                             (make-news-title %)
                             [:a {:href (:link %) :target "_blank"} (make-news-title %)])]]) items)]
      (if pager? [:p {:class "pager"} (news-pager page)]) )))

(defn incstr [str-value]
  (try
    (let [v (+ 1 (Integer/parseInt str-value))]
      (if (<= v max-news-pages) v 1)) 
    (catch java.lang.NumberFormatException _ 1)))

;;
;; templates
;;
(defn template-index "Associate the announcemet, meeting and news entry with the html."
  ([announcement meeting news] (template-index announcement meeting news (make-ad)))
  ([announcement meeting news ad] (enlive/template
   "html/template-index.html" [] ;; src and args
   ;; metadata is tricky
   [(and (enlive/has [:meta]) (enlive/attr-has :name "description"))] (enlive/set-attr :content (:title meeting))
   [(and (enlive/has [:meta]) (enlive/attr-has :name "keywords"))]    (enlive/set-attr :content (:title meeting)) 
   [(and (enlive/has [:meta]) (enlive/attr-has :name "author"))]      (enlive/set-attr :content "Heow Goodman")

   ;; make home active
   [:div#header [:a (enlive/nth-of-type 4)]] (enlive/set-attr :class "active")

   ;; announcement
   [:span.announcementHeader] (enlive/html-content (:title announcement))
   [:p.announcementContent]   (enlive/html-content (str (:content announcement)))

   ;; meeting
   [:title]              (enlive/content (str "New York City Lisp User Group: " (:title meeting)))
   [:span.meetingHeader] (enlive/html-content (str "meeting - " (tformat/unparse (.withZone (tformat/formatter "EEEE, MMMM d, h:mm a") (time/time-zone-for-id "America/New_York")) (:time meeting)) " - <i>" (:title meeting) "</i>"))
   [:p.meetingContent]   (enlive/html-content (str (:description meeting) "<p>Location:<br>"(:venue meeting) "<br>" (:address meeting) "<br>" (:address2 meeting)
                                                   (if (.contains (:venue meeting) "Google")
                                                     "<br><br>You <a href=\"/meeting/rsvp\">must RSVP here</a> or at <a target=\"_blank\" href=\"http://www.meetup.com/LispNYC/\">Meetup</a>" "")
                                                   "</p><p><a target=\"_blank\" href=\"" (:event-url meeting) "\">more</a></p>"))
   ;; news entry
   [:span.blogHeader] (enlive/html-content "news")
   [:p.blogContent]   (enlive/html-content (str (htmlify-news news 1 false) "<p><a href=\"/news\">more news</a></p>"))

   ;; ad
   [:a#ad]   (enlive/set-attr :href (:url  ad))
   [:img#ad] (enlive/set-attr :src  (:path ad)) 
   
   ;; witty saying
   [:div#footerLeft]     (enlive/html-content (str "&nbsp;&nbsp;" (make-saying)))   
   ))
)

(defn template-wiki "do the same with the wiki data"
  ([wiki-article] (template-wiki wiki-article 4 (make-ad)))
  ([wiki-article active-header-index] (template-wiki wiki-article active-header-index (make-ad)))
  ([wiki-article active-header-index ad]
     (enlive/template
      "html/template-index.html" []
      [:title]              (enlive/content (str "New York City Lisp User Group: " (:title wiki-article)))

      ;; header nav
      [:div#header [:a (enlive/nth-of-type active-header-index) ]] (enlive/set-attr :class (if (= 1 active-header-index) "activeLastMenuItem" "active") )

      ;; wipe out announcement and news
      [:div#announcement] (enlive/content "")
      [:div#news] (enlive/content "")
                      
      [:content]            (enlive/html-content (:content wiki-article))
      [:span.meetingHeader] (enlive/content (:title wiki-article))
      [:p.meetingContent]   (enlive/html-content (:content wiki-article))

      ;; ad
      [:a#ad]   (enlive/set-attr :href (:url  ad))
      [:img#ad] (enlive/set-attr :src  (:path ad)) 
      
      [:div#footerLeft]     (enlive/html-content (str "&nbsp;&nbsp;" (make-saying)))
      )))

(defn only-date [dt]
  (time/date-time (time/year dt) (time/month dt) (time/day dt)))

(defn assoc-meeting-with-video [meeting videos]
  (assoc meeting :video (first (filter #(= (only-date (:time meeting))
                                           (only-date (:date %))) videos))))

(defn unparse-meeting-date [mdate]
  (tformat/unparse (.withZone (tformat/formatter "MMM d, YYYY")
                              (time/time-zone-for-id "America/New_York"))
                   mdate))

(defn template-meetings []
  (let [active-header-index 1
        ad                  (make-ad)]
    (enlive/template
     "html/template-index.html" []
     [:title]              (enlive/content (str "New York City Lisp User Group: Past Meetings" ))
     
     ;; header nav
     [:div#header [:a (enlive/nth-of-type active-header-index) ]] (enlive/set-attr :class (if (= 1 active-header-index) "activeLastMenuItem" "active") )

     ;; wipe out announcement and news
     [:div#announcement] (enlive/content "")
     [:div#news] (enlive/content "")
                      
     [:span.meetingHeader] (enlive/content "past-meetings")
     [:p.meetingContent]   (enlive/html-content      (let [meetings-with-videos
           (reverse (sort-by :time (map #(assoc-meeting-with-video % (fetch-videos)) (fetch-past-meetups))))]
       (html/html
        [:table {:id "meeting" }
         (map #(html/html [:tr 
                           [:td {:class "meeting-date" :valign "top"} (unparse-meeting-date (:time %))]
                           [:td {:class "meeting-title" :valign "top"} [:a {:href (:event-url %) :target "_blank"} (:title %)]]
                           [:td {:class "meeting-video"} (if (not (nil? (:video %)))
                                  (list
                                   [:a {:href (:video-url (:video %)) :target "_blank"}  [:img {:src (:thumbnail-url (:video %))}] "(video)" ] ) 
                                  )]])
              meetings-with-videos)])))

     ;; ad
     [:a#ad]   (enlive/set-attr :href (:url  ad))
     [:img#ad] (enlive/set-attr :src  (:path ad))
      
     [:div#footerLeft]     (enlive/html-content (str "&nbsp;&nbsp;" (make-saying)))
     )))

;;
;; pages
;;
(defn debug-page []
  (future (swank.swank/start-repl))
  "Debugging started on localhost, swank on in to :4005 kind sir.")

(defn news-page [cookies params]
  {:cookies { "visits" (str (incstr (:value (cookies "visits")))) }
   :body (let [vp      (let [visits  (incstr (:value (cookies "visits")))
                             page    (util/str->int (params "p"))]
                         (if (> page 0) page visits))
               content (take 30 (news/fetch vp))
               html    (htmlify-news content vp)]
           ((template-wiki {:title (news-title vp) :content html} 3)) )
   })

(defn meeting-page []
  ((template-meetings)))

;;
;; form processing
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

(defn mail-generic [params thanks-target]
  (if (empty? (params "jobtitle")) ; linkbait
    (let [msg (map->mailstr params)
          cmd (str "/bin/echo '" msg "' | /usr/bin/mail heow@alphageeksinc.com -s '" (validate-input (params "subject")) "'")]
      (shell/sh "/bin/sh" "-c" cmd)
      (str "<html><meta http-equiv=\"REFRESH\" content=\"0;url=/" thanks-target "\"></HEAD></html>") )))

(defn mail-rsvp [params]
  (mail-generic params "rsvp-thanks"))

(defn mail-contact [params]
  (mail-generic params "contact-thanks"))

(defn mail-blog [params]
  (mail-generic params "blog-thanks"))


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
;; Jetty routes
;;
(ww/defroutes app-routes
  (ww/GET "/"          [] ((template-index (wiki/fetch-wikipage "front-page") (fetch-meetup) (take 10 (news/fetch 1)))))
  (ww/GET "/home"      [] ((template-index (wiki/fetch-wikipage "front-page") (fetch-meetup) (take 10 (news/fetch 1)))))
  (ww/GET "/debug"     [] (debug-page))
  (ww/GET "/meeting"   [] (meeting-page))
  (ww/GET "/meetings"  [] (meeting-page))
  (ww/GET "/news"      {params :params cookies :cookies} (news-page cookies params))
  (ww/GET "/robots.txt" [] "User-agent: *\r\nDisallow: /wiki/\r\nAllow: /\r\n" )
  
  (ww/POST "/blog-signup" {params :params} (mail-blog    params))
  (ww/POST "/rsvp"     {params :params} (mail-rsvp    params))
  (ww/POST "/contact"  {params :params} (mail-contact params))
  
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
(ww/wrap! app-routes wrap-context cookies/wrap-cookies params/wrap-params)

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
