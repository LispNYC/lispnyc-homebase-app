(ns org.lispnyc.webapp.homebase.feed.reddit
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clj-time.format        :as time]))

(defn fetch [r]
  (println "fetching subreddit " r)
  (let [data (fetch-url-xml (str "http://reddit.com/r/" r ".xml"))]
        (map #(hash-map :type      (keyword (str "reddit-" r))
                      :title     (enlive/text (nth % 0))
                      :link      (enlive/text (nth % 1))
                      :pub-date  (parse-date (time/formatters :rfc822)
                                             (enlive/text (nth % 2)))
                      :relevance 0.6)
           (partition 3 (enlive/select data #{[:rss :channel :item :title]
                                              [:rss :channel :item :link]
                                              [:rss :channel :item :pubDate]}))) ))
  