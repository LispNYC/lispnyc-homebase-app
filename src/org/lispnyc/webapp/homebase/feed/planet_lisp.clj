(ns org.lispnyc.webapp.homebase.feed.planet-lisp
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string         :as string]
            [clj-time.format        :as time]))

;; once again, it's ALMOST rfc822 compliant
;; convert 'GMT' to '+0000'
(defn parse-pl-date [datestr]
  (parse-date (time/formatters :rfc822) (.replace datestr " GMT" " +0000")))

(defn fetch []
  (println "fetching planet-lisp")
  (let [data (fetch-url "http://planet.lisp.org/rss20.xml")]
    (vec (map #(hash-map :type      :planet-lisp
                         :title     (enlive/text (nth % 0))
                         :link      (enlive/text (nth % 1))
                         :pub-date  (parse-pl-date (enlive/text (nth % 2)))
                         :relevance 0.7)
              (partition 3 (enlive/select
                            data #{[:item :> :title]
                                   [:item :> :guid]
                                   [:item :> :pubDate]}))))))

