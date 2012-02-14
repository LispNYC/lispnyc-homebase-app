(ns org.lispnyc.webapp.homebase.feed.reddit
  (:require [net.cgrand.enlive-html :as enlive])
  (:import  [java.io.File]))

(defn fetch-url [url]
  (enlive/xml-resource (java.net.URL. url)))

(defn fetch [r]
  (let [data (fetch-url (str "http://reddit.com/r/" r ".xml"))]
    (map #(hash-map :title    (enlive/text (nth % 0))
                    :link     (enlive/text (nth % 1))
                    :pub-date (enlive/text (nth % 2)))
         (partition 3 (enlive/select data #{[:rss :channel :item :title]
                                            [:rss :channel :item :link]
                                            [:rss :channel :item :pubDate]}))) ))