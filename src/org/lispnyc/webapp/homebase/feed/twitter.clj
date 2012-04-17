(ns org.lispnyc.webapp.homebase.feed.twitter
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clj-time.format        :as time]
            twitter
            [oauth.client           :as oauth]))

;; not quite rfc822
(def format-twit (time/formatter "EEE MMM dd HH:mm:ss Z yyyy"))

(defn search [hashtag]
  (println "fetching twitter-search #" hashtag)
  (let [data (fetch-url-xml (str "http://search.twitter.com/search.atom?q=%23" hashtag "&count=5"))]
    (vec (map #(hash-map :type            (keyword (str "tweet-" hashtag))
                         :pub-date        (parse-date
                                           (time/formatters :date-time-no-ms)
                                           (enlive/text (nth % 0)))
                         :title           (enlive/text (nth % 1))
                         :user            (enlive/text (nth % 2))
                         :link            (enlive/text (nth % 3))
                         :weight          0
                         :relevance       0.1)
              ;; note: order is dependent on location in XML
              (partition 4 (enlive/select data #{[:feed :> :entry :> :published]
                                                 [:feed :> :entry :> :content]
                                                 [:feed :> :entry :> :author :> :name]
                                                 [:feed :> :entry :> :author :> :uri] }))))))

(defn fetch
  ([hash-tag] (search hash-tag))
  ([](println "fetching twitter")
     (let [data (fetch-url-xml (str "https://api.twitter.com/1/statuses/user_timeline.xml?include_entities=true&include_rts=true&screen_name=lispnyc&count=5"))]
       (vec (map #(hash-map :type            :tweet-lispnyc
                            :pub-date        (parse-date
                                              format-twit
                                              (enlive/text (nth % 0)))
                            :title           (enlive/text (nth % 1))
                            :weight          (str->int (enlive/text (nth % 2))) 
                            :user            "LispNyc"
                            :link            (str "https://twitter.com/#!/" (enlive/text (nth % 3)))
                            :relevance       1)
                 ;; note: order is dependent on location in XML
                 (partition 4 (enlive/select data #{[:statuses :> :status :> :created_at]
                                                    [:statuses :> :status :> :text]
                                                    [:statuses :> :status :> :retweet_count]
                                                    [:statuses :> :status :> :user :> :screen_name] })))))))

(def oauth-consumer
     (oauth/make-consumer "XLa0uwfmTLK57Z130QE0g"
                          "gK3mtfemb570z7fZ4SH7X8SDwpSWqEVYSgB1M4PElsY"
                          "https://api.twitter.com/oauth/request_token"
                          "https://api.twitter.com/oauth/access_token"
                          "https://api.twitter.com/oauth/authorize"
                          :hmac-sha1))

(comment ; uncomment to generate and validate with browser
  (def oauth-request-token 
       (oauth/request-token oauth-consumer))
  (println "authorize with browser: "
           (oauth/user-approval-uri
            oauth-consumer (:oauth_token oauth-request-token)))
  (def atr ; oauth access token response
     (oauth/access-token oauth-consumer 
                         oauth-request-token
                         "put the value from the browser here"))
  )

(def atr
     {:oauth_token "166167502-a9AldwBKwmpzWIrZx94gr1zRK6IJFG1WWVzO4qwu",
      :oauth_token_secret "a7awWBvOyg8G2PKU3iqrf5ssURJj0ArpwbKHe68mMKc",
      :user_id "166167502", :screen_name "LispNYC"})

(defn fetch-friends []
  (println "fetching twitter friends")
  (map #(hash-map :type            :tweet-friend
                  :pub-date        (parse-date format-twit (:created_at %))
                  :title           (:text %)                  
                  :weight          (:retweet_count %)
                  :user            (:screen_name (:user %))
                  :link            (str "https://twitter.com/"
                                        (:screen_name (:user %))
                                        "/status/"
                                        (:id_str %))
                  :relevance 0.5)
       (twitter/with-oauth
         oauth-consumer (:oauth_token atr) (:oauth_token_secret atr)
         (twitter/friends-timeline :count "199"))))
