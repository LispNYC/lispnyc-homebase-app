(ns org.lispnyc.webapp.homebase.feed.pebble-blog
  (:require [net.cgrand.enlive-html :as enlive])
  (:import  [java.io.File]))

(defonce blog-path "./pebbleblog-articles/blogs/")

(def blog-factory (new net.sourceforge.pebble.dao.file.FileDAOFactory))
(def blog-service (new net.sourceforge.pebble.domain.BlogService))

(defn initialize[]
  (. net.sourceforge.pebble.dao.DAOFactory setConfiguredFactory blog-factory))

;; run only once beacuse the magic factory has side-effects
(def initialize (memoize initialize))

(defn make-blog [blog-path]
  (new net.sourceforge.pebble.domain.Blog blog-path))

(def make-blog (memoize make-blog)) ;; memoized because it's expensive

(defn alphanumericize-input "Only keep specific characters in the input string."
     [input-str]
     (let [re (re-pattern "[a-zA-Z0-9 ]")]
       (apply str (filter #(re-matches re (str %1)) input-str))))

(defn fetch-blog "Initialize a net.sourceforge.pebble.domain.Blog from the give file path and return a Clojure keyword struct."
  [blog-path]
  (initialize)
  (let [blog    (make-blog blog-path)
        ;; TODO: figure out most recent entries that doesn't involve ehcache
        entries (take 10 (.getBlogEntries blog-service blog))
        entry   (first (sort-by #(* -1 (.getTime (.getLastModified %1))) (filter #(.isPublished %1) entries)))]
    (if (nil? entry) nil ; filter out nill later
      {
       :author     (.getAuthor  blog) ; entry author is username
       :title      (.getTitle   entry)
       :content    (if (= 0 (count (.getExcerpt entry)))
                     (.getContent entry)
                     (str (.getExcerpt entry)
                          "<p><a href=\"" "/blog/"  
                          (first (reverse (re-seq #"\w+" (.getRoot blog)))) "/" ; get name from /data/blogs/heow/
                          (.replace (alphanumericize-input (.toLowerCase (.getTitle entry))) " " "-" )
                          "\">Read more...</a></p>")) ;; direct link to article
       :mod-date   (.getLastModified entry)
       })))

(defn fetch-announcement []
  (fetch-blog (str blog-path "default/")))

(defn fetch-blogs "Examine the blog-path to determine which blogs are available for initialization, ignoring empty blogs and the default (system announcement) one."
  ([] (fetch-blogs blog-path))
  ([blog-path]
     (sort-by #(* -1 (.getTime (:mod-date %1))) ; sort by decending date
      (let [blog-dir   (. (new java.io.File blog-path) listFiles)
            blog-paths (map #(. %1 getPath) blog-dir)] ; list of blog paths
        (filter #(and (not (nil? %1)) (not (= %1 (str blog-path "default")))) ; ignore default (announcement) and empties
                (map #(fetch-blog %1) blog-paths)))))) 
  