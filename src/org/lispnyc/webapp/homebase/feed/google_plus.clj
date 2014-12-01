(ns org.lispnyc.webapp.homebase.feed.google-plus
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string         :as string]
            [clj-time.format        :as time]))

(defn fetch 
  ([] (fetch "103516573087603154830" 1.0))
  ([id relevance]  
     (println "fetching google-plus" id)
     (let [data (fetch-url (str "https://plus.google.com/communities/" id))]
       (vec (map #(hash-map :type      :google-plus-lispnyc-community
                            :link      (str "http://plus.google.com/" (:href (:attrs (nth % 0))))
                            :title     (shorten-title (enlive/text (nth % 1)))
                            :pub-date  (parse-date (time/formatters :year-month-day) (first (:content (nth % 0))))
                            :weight    0
                            :relevance relevance)
                 (partition 2 (enlive/select data #{[:a.o-U-s]
                                                             [:div.Bt.Pm :> :div.Ct] 
                                                             }))) ))) )
