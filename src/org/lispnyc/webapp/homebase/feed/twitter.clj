(ns org.lispnyc.webapp.homebase.feed.twitter
  (:use     org.lispnyc.webapp.homebase.feed.util
            org.lispnyc.webapp.homebase.feed.twitter-creds)
  (:require [net.cgrand.enlive-html :as enlive]
            [clj-time.format        :as time]
            twitter.api.restful))

;; still not quite rfc822
(def format-twit (time/formatter "EEE MMM dd HH:mm:ss Z yyyy"))

(defn search [hashtag]
  (println (str "fetching twitter-search #" hashtag))
  (let [response (twitter.api.restful/search-tweets :oauth-creds twit-creds :params {:q (str "#" hashtag)})
        tweets   (:statuses (:body response))]
    (vec (map #(hash-map :type     (keyword (str "tweet-" hashtag))
                         :pub-date (parse-date format-twit (:created_at %)) 
                         :title    (:text %)
                         :link     (str "https://twitter.com/" (:screen_name (:user %)) "/status/" (:id %))
                         :weight    0
                         :relevance 0.1
                         ) tweets)) ))

(defn fetch-twitter-friends [user-name]
  (println "fetching twitter home-timeline for" user-name)
  (let [response   (twitter.api.restful/statuses-home-timeline :oauth-creds twit-creds :params {:screen-name user-name})
        tweets     (:body response)]
    (vec (map #(hash-map :type      :tweet-friend
                         :pub-date  (parse-date format-twit (:created_at %)) 
                         :title     (:text %)
                         :link      (str "https://twitter.com/" (:screen_name (:user %)) "/status/" (:id %))
                         :weight    (:retweet_count %)
                         :user      (:screen_name (:user %))
                         :relevance 0.5) tweets)) ))

(defn fetch
  ([hash-tag] (search hash-tag))
  ([](println "fetching twitter, our feed")
     (let [user-name "LispNyc" 
           response  (twitter.api.restful/statuses-user-timeline :oauth-creds twit-creds :params {:screen-name user-name}) 
           tweets    (:body response)]
       (vec (map #(hash-map :type      :tweet-lispnyc
                            :pub-date  (parse-date format-twit (:created_at %)) 
                            :title     (:text %)
                            :link      (str "https://twitter.com/" (:screen_name (:user %)) "/status/" (:id %))
                            :weight    (:retweet_count %)
                            :user      user-name
                            :relevance 1.0) tweets)) )))

