(ns org.lispnyc.webapp.homebase.feed.vimeo
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [clj-json.core          :as json]
            [clj-time.core          :as time]
            [clj-time.coerce        :as tcoerce]
            [clj-time.format        :as tformat] ))

;; TODO update to use new Vimeo API
(defn fetch-videos
  ([page-number]
    (let [videos (json/parse-string (fetch-url-raw (str "http://vimeo.com/api/v2/lispnyc/videos.json?page=" page-number)))]
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
  ([] (concat (fetch-videos 1) (fetch-videos 2) (fetch-videos 3)))
  )
