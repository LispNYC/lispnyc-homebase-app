(ns org.lispnyc.webapp.homebase.feed.twitter
  (:require [net.cgrand.enlive-html :as enlive])
  (:import  [java.io.File]))

(defn fetch-url [url]
  (enlive/xml-resource (java.net.URL. url)))

(defn fetch []
  (let [data (fetch-url (str "https://api.twitter.com/1/statuses/user_timeline.xml?include_entities=true&include_rts=true&screen_name=lispnyc&count=5"))]
    (map #(hash-map :pub-date (enlive/text (nth % 0))
                    :text     (enlive/text (nth % 1))
                    :user     (enlive/text (nth % 2)))
         ; note: order is dependent on location in XML
         (partition 3 (enlive/select data #{[:statuses :> :status :> :created_at]
                                            [:statuses :> :status :> :text]
                                            [:statuses :> :status :> :user :> :screen_name] })))))