(ns org.lispnyc.webapp.homebase.article
  (:use 
   [clojure.contrib.str-utils :only (str-join)]
   [clojure.contrib.string :only (blank? split split-lines trim)]) )

(defn metadata? [string]
  (not (nil? (re-seq #"\w+ *: *[\w, ]+" (str string)))))

(defn extract-metadata
  "Returns [metadata-map text], metadata-map is the leading key value pairs."
   [text]
  (let [[meta lines] (split-with metadata? (split-lines text))]
    (vector ; return as vec2
     (reduce merge
             ;; convert from list to a keyworded map
             (map #(let [[a b] %1] {(keyword a) b}) ; (kw (str (ns-name *ns*)) 
                  ;; split and trim the individual lines to feed up
                  (map #(vec (map trim (split #":" %1))) meta) ))
     (str-join \newline lines) ) ))

(defn slash-str "Returned string will start with a leading slash."
   [string]
  (if (= \/ (nth string 0 ""))
    string
    (str "/" string) ))

(defn make-vec "Create a vector from a comma delimited string"
  [string]
  (vec (filter
        #(not (= 0 (count %1))) ; drop blanks
        (split #"[\s,]" (str string)) ))) ; seed a str

(defrecord Article [title uri-path section author tags description content])

(defn make-article "Return a functional Article given the free-form text"
   [text]
  (let [[m content] (extract-metadata text)]
    (Article. (:title m)
              (slash-str (:uri-path m))
              (:section m)
              (:author m)
              (make-vec (:tags m))
              (:description m)
              content)))
