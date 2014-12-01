(ns org.lispnyc.webapp.homebase.feed.util
  (:require [net.cgrand.enlive-html :as enlive]
            [clj-time.format        :as time])
  (:import  [java.io.File]))

;; now easier with slurp
(comment defn fetch-url-raw [url]
  (with-open [stream (.openStream (java.net.URL. url))]
    (let  [buf (java.io.BufferedReader. 
                (java.io.InputStreamReader. stream))]
      (apply str (line-seq buf)))))

(defn fetch-url-raw [url] (slurp url))

(defn fetch-url [url]
  (enlive/html-resource (java.net.URL. url)))

(defn fetch-url-xml [url]
  (enlive/xml-resource (java.net.URL. url)))

(defn parse-date [formatter datestr]
  (try
    (time/parse formatter datestr) 
    (catch java.lang.Exception _ nil)))

;; "42" => 42, default 0
(defn str->int [str]
  (try
    (Integer/parseInt str)
    (catch java.lang.NumberFormatException _ 0)))

(defn str->long [str]
  (try
    (Long/parseLong str)
    (catch java.lang.NumberFormatException _ 0)))

(defn now [] (new org.joda.time.DateTime))

(defn shorten-title [s]
  (if (< (count s) 81) s (str (apply str (take 77 s)) "...")))
