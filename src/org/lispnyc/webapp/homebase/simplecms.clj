(ns org.lispnyc.webapp.homebase.simplecms
  (:use     [org.lispnyc.webapp.homebase.article :as article])
  (:require [compojure.core                      :as ww])
  (:import  [java.io File]))

(defn- walk-dir [dirpath pattern]
  (filter #(re-matches pattern %1)
          (map #(.getPath %1) (-> dirpath File. file-seq))))

(defn publish-article "Return a route by calling the result of template-article"
   [article template-fn]
   (println "uri:" (:uri-path article))
   (ww/GET (:uri-path article) [] (apply str ((template-fn article)))))

(defn publish-path "Make and publish articles based on the directory path and filename pattern."
  [dirpath pattern template-fn]
  (map #(publish-article %1 template-fn)
       (map #(article/make-article (slurp %1)) (walk-dir dirpath pattern)) ))

(defn indexed-at "Return index of the [coll]ection using the [pred]icate"
  ([pred coll] (indexed-at pred coll 0))
  ([pred coll cnt] (cond
                    (empty? coll) nil
                    (pred (first coll)) cnt
                    (true? true) (recur pred (rest coll) (inc cnt)) )))

(defn splice-in-list
  "Splice into the collection: (s-i-l '(a B d) 1 '(b c)) => (a b c d)"
  [coll n splice-coll]
  (concat (take n coll) splice-coll (nthnext coll (inc n))))

(defn expand-publish-path "Given a collection of routes, expand the vaules of publish-path into real routes, the result is spliced into the collection."
   [coll]
  (let [idx (indexed-at #(and (list? %1) (= 'publish-path (first %1))) coll)]
    (if (= nil idx) coll
        (recur (splice-in-list
                coll idx
                (let [pp (nth coll idx)] ;; rebuild pub-path, no eval
                  (publish-path (first (rest pp)) (second (rest pp)))))))))

(defmacro defroutes-pathed "Define routes, expand any publish-path functions"
  [route-name & routes]
  (let [spliced-routes (expand-publish-path routes)]
    `(ww/defroutes ~route-name ~@spliced-routes) ))
