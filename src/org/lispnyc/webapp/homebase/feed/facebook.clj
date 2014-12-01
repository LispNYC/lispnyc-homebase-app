(ns org.lispnyc.webapp.homebase.feed.facebook
  (:use     org.lispnyc.webapp.homebase.feed.util
            org.lispnyc.webapp.homebase.feed.facebook-creds)
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string         :as string]
            [clj-time.format        :as time]
            [clojure.data.json      :as json]))

(defn fetch 
  ([] (fetch "19432875259"))
  ([id]  
     (println "fetching facebook" id)
     (let [data (:data (json/read-json (fetch-url-raw (str "https://graph.facebook.com/v2.1/" id "/feed?access_token=" access-token)))) ]
       (map (fn [d] (hash-map
                     :type          :facebook
                     :link          (:link (first (:actions d)))
                     :title         (shorten-title (:message d))
                     :pub-date      (parse-date (time/formatters :date-time-no-ms) (:updated_time d))
                     :relevance     0.7 )) data))))


