(ns org.lispnyc.webapp.homebase.feed.google-groups
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string         :as string]
            [clj-time.format        :as time]))

;; This is also somewhere in the start script "java -Dhttp.agent"
;; uncomment for standalone running and testing
(. System setProperty "http.agent" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:10.0.1) Gecko/20100101 Firefox/10.0.1")

(defn parse-google-date
  "Return real DateTime based on the Google group date formats, which are
  typcially 'Mar 26', or '2:42pm', depending on when it is."
  [datestr]
  (let [now          (new org.joda.time.DateTime)
        this-year    (.getYear now)
        format-mmmdd (time/formatter "MMM dd")]
    (try
      (.withYear (time/parse format-mmmdd datestr) this-year)
      (catch java.lang.IllegalArgumentException _
        (try
          (let [this-month  (.getMonthOfYear now)
                this-day    (.getDayOfMonth now)
                format-hhmm (time/formatter "kk:mmaa")]
            (.withDayOfMonth
             (.withMonthOfYear
              (.withYear (time/parse format-hhmm datestr) this-year)
              this-month)
             this-day))
          (catch java.lang.IllegalArgumentException _ nil)) ))))

(defn fetch []
  (println "fetching google-groups...")
  (let [data (fetch-url (str "http://groups.google.com/a/lispnyc.org/group/lisp/topics"))]
    (vec (map #(hash-map :type      :lisp-group
                         :link      (str "http://groups.google.com" (:href (:attrs (nth % 0))))
                         :title     (enlive/text (nth % 1))
                         :pub-date  (parse-google-date
                                     (string/trim (enlive/text (nth % 2))))
                         :weight    0
                         :relevance 1)
              (partition 3 (enlive/select data #{
                       [:div.maincontoutboxatt :p :table :tr :> :td :> :a :font]
                       [:div.maincontoutboxatt :p :table :tr :> :td :> [:a (enlive/attr-starts :href "/a/lispnyc.org/group/lisp/browse_thread/thread")]]
                       [:div.maincontoutboxatt :p :table :tr :> :td.padt2 :> :div.padt2 :> :font :> :nobr]
                       }))) )))