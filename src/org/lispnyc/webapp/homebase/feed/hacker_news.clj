(ns org.lispnyc.webapp.homebase.feed.hacker-news
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string         :as string]
            [clj-time.format        :as time]))

(def hn-url "https://news.ycombinator.com/")

(defn get-2ago
  "['foo 'bar \"1\" \"hour\" \"ago\" 'baz] => [\"1\" \"hour\"]"
  ([ss cnt] (if (empty? ss) [] (if (< 0 cnt)
                                 (conj (get-2ago (rest ss)(dec cnt)) (first ss))
                                 (get-2ago (rest ss)))))
  ([ss] (if (empty? ss) [] (if (= "ago" (first ss)) (get-2ago (rest ss) 2)
                               (recur (rest ss)) ))) )

;; "foos => foo, foo => foo"
(defn strip-s [word]
  (if (= \s (first (reverse word)))
    (apply str (reverse (rest (reverse word))))
    word))

;; "90 points by hawke 1 hour ago  | 32 comments" => date
(defn parse-hn-date [s]
  (let [vt (get-2ago (reverse (string/split s #"\s")))]
    (if (not= 2 (count vt)) (now)
        (let [value (str->int (first vt))
              type  (strip-s (second vt))]
          (.minusSeconds (now) (* value (cond (= type "second") 1
                                              (= type "minute") 60
                                              (= type "hour")   (* 60 60)
                                              (= type "day")    (* 60 60 24)
                                              (= type "week")   (* 60 60 24 7)
                                              (= type "month")  (* 60 60 24 30)
                                              :else 1))) ))))

;; 'foo' => '/foo', '/foo' => '/foo'
(defn slash [s] (if (= \/ (first s)) s (str "/" s)))

(defn fetch
  ([count url]
     (println "fetching hacker-news " count " " url)
     (if (= 0 count) nil
      (let [data     (fetch-url url)
            articles (vec (map #(hash-map :type      :hacker-news
                                          :link      (:href (:attrs (nth % 0)))
                                          :title     (enlive/text (nth % 0)) #_(org.lispnyc.webapp.homebase.core/validate-input (nth % 0)) 
                                          :pub-date  (parse-hn-date (enlive/text (nth % 1)))
                                          :weight    (/ (max 1 (str->int (.replace (enlive/text (nth % 2))
                                                                                   " points" "")))
                                                        10)  
                                          :relevance 0.7
                                          :page count)
                          (partition 3 (enlive/select
                                        data #{
                                               [:table :tr :td.title :> :a]
                                               [:td.subtext]
                                               [(enlive/attr-starts :id "score_")]}))) )
            next    (:href (:attrs (first (enlive/select data [:table [:tr enlive/last-child] :td.title :a]))))
            ]
        (clojure.set/union articles (fetch (dec count) (str hn-url (slash next))))
        ))) 
  ([] (filter #(not= nil (:link %)) ; isn't perfect
              (fetch 3 hn-url))))

