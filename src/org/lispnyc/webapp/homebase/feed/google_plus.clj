(ns org.lispnyc.webapp.homebase.feed.google-plus
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string         :as string]
            [clj-time.format        :as time]))

(defn fetch 
  ([] (fetch "103516573087603154830" 1.0))
  ([id relevance]  
     (println "fetching google-plus" id)
     (let [data (fetch-url (str "https://plus.google.com/communities/" id)) 
           ]
       (vec (map #(hash-map :type      :google-plus-lispnyc-community
                            :link      (str "http://plus.google.com/" (:href (:attrs (nth % 0))))
                            :title     (str (reduce str (take 80 (enlive/text (nth % 1)))) "...")
                            :pub-date  (parse-date (time/formatters :year-month-day) (first (:content (nth % 0))))
                            :weight    0
                            :relevance relevance)
                 (partition 2 (enlive/select data #{[:a.o-U-s]
                                                             [:div.Bt.Pm :> :div.Ct] 
                                                             }))) ))) )
(comment
  (def d1 (fetch-url (str "https://plus.google.com/communities/103516573087603154830"))) ; lispnyc
  (def d2 (fetch-url (str "https://plus.google.com/communities/101016130241925650833"))) ; cl
  (def d3 (fetch-url (str "https://plus.google.com/communities/103410768849046117338"))) ; clojure
)

(comment defn make-str [s] (if (string? s) s ""))
(comment defn foo [n x]
  [
   (make-str (first (:content (nth (enlive/select d1 #{x}) n))))
   ;(make-str (first (:content (nth (enlive/select d2 #{x}) n))))
   ;(make-str (first (:content (nth (enlive/select d3 #{x}) n))))
   ]
  )