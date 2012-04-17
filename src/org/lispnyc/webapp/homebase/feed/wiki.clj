(ns org.lispnyc.webapp.homebase.feed.wiki
    (:require [clojure.string :as string]))

;; set when we wrap the context in core
(def servlet-context (atom nil))

;; local cache
(def wiki-properties (atom nil)) 

(defn camel-caseify
     "converts hi-there to HiThere"
     [cc]
     (apply str (map string/capitalize (string/split cc #"-"))) )



;;
;; Make ourselves a wiki engine from the servlet-context, this assumes there is
;; a JSPWiki running somewhere inside this app-server and the servlet context
;; is set.
;;
(defn fetch-raw-wikipage [page-name]
  (if-let [ctx @servlet-context]
    (do
      (if (nil? @wiki-properties) ; do once and cache
        (do (reset! wiki-properties (new java.util.Properties))
            (.load @wiki-properties (new java.io.FileInputStream "./etc/jspwiki.properties"))))
      (let [we (com.ecyrd.jspwiki.WikiEngine/getInstance ctx @wiki-properties)]
        ;; page is nil if it does not exist
        (if-let [page (.getPage we page-name)]
          (let [wiki-context (new com.ecyrd.jspwiki.WikiContext we page)]
            (.setRequestContext wiki-context "view")
            (string/replace 
             (try
               (.getHTML we wiki-context page)
               (catch java.lang.NullPointerException _ ""))
             "/wiki/Wiki.jsp?page=" "/"))
          ;; not exist?  if we're not camel-cased, try that version before giving up
          (if (contains? (set "ABCDEFGHIKJLMNOPQRSTUVWXYZ") (first page-name))
            ""
            (fetch-raw-wikipage (camel-caseify page-name))) ))))) ; don't bother with recur

(defn fetch-wikipage [page-name]
  {:title page-name
   :content (fetch-raw-wikipage page-name)
   })

