(ns org.lispnyc.webapp.homebase.feed.vimeo
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [clj-json.core          :as json]
            [clj-time.core          :as time]
            [clj-time.coerce        :as tcoerce]
            [clj-time.format        :as tformat] ))

(comment defn parse-date [d]
  (try
    (tformat/parse )
    (catch java.lang.IllegalArgumentException e nil)))

(defn fetch-videos[]
  (let [videos (json/parse-string (fetch-url-raw "http://vimeo.com/api/v2/lispnyc/videos.json"))]
    (map (fn [v] (hash-map
           :title         (v "title")
           :video-url     (v "url")
           :thumbnail-url (v "thumbnail_small")
           :description   (apply str (take 40 (v "description")))
           :duration      (v "duration")
           :tags          (map #(.trim %) (.split (v "tags") ","))
           :date          (first (filter #(not (nil? %)) ; find the date in the tags
                                   (map #(parse-date (:date tformat/formatters) %)
                                        (map #(.trim %) (.split (v "tags") ",")))))
           )) videos)))
