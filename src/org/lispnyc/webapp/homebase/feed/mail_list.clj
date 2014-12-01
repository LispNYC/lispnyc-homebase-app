(ns org.lispnyc.webapp.homebase.feed.mail-list
  (:use     org.lispnyc.webapp.homebase.feed.util
            org.lispnyc.webapp.homebase.feed.mail-list-creds) ; usr/pwd
  (:require [clj-time.format :as time]
            [hiccup.core     :as html] ))

(def session (javax.mail.Session/getDefaultInstance (System/getProperties)))

;; pull text out of a mime message
(defn walk-content [c]
  (cond 
   (string? c) c
   (= (type c) com.sun.mail.pop3.POP3Message)     (walk-content (.getContent c))
   (= (type c) com.sun.mail.imap.IMAPMessage)     (walk-content (.getContent c))
   (= (type c) javax.mail.internet.MimeMultipart) (for [i (range (.getCount c))] (walk-content (.getBodyPart c i)))
   (or (= (type c) javax.mail.internet.MimeBodyPart)
       (= (type c) com.sun.mail.imap.IMAPBodyPart)) (cond (.startsWith (.toLowerCase (.getContentType c)) "text/plain;") (walk-content (.getContent c)) 
                                                          (.startsWith (.toLowerCase (.getContentType c)) "multipart/alternative;") (walk-content (.getContent c)) 
                                                          :else "") ))

;; flattens mime types
(defn get-content [m]
  (let [out (walk-content m)]
    (cond (string? out) out
          :else         (apply str (flatten out)))))

(defn make-messages [messages]
  (for [m messages]
    (let [id (re-find #"[0-9a-zA-Z.-]*" (clojure.string/replace (.getMessageID m) "<" ""))]
      (hash-map 
       :type       :mail-list
       :id         id
       :title      (.getSubject m)
       :link       (str "/list-lisp/" id)
       :pub-date   (org.joda.time.DateTime. (.getSentDate m)) ; java.util.Date
       :content    (html/html [:pre (get-content m)] [:br] [:br] "read more on the " [:a {:href "http://list.lispnyc.org" } "LispNYC mailing list"])
       :relevance  0.9))))

;; NOTE: not closing connections for a reason
(defn fetch-pop3 []
  (println "fetching mailing list" mail-list-user-address)
  (let [store (.getStore session "pop3s")] ; with-open fail
    (.connect store "pop.gmail.com" mail-list-user-address mail-list-password)
    (let [folder (. store getFolder "inbox")]
      (.open folder (javax.mail.Folder/READ_ONLY ))
      (make-messages (.getMessages folder))
      )))

;; imap supports folders
(defn fetch-imap [folder-name] 
  (println "fetching email " mail-list-user-address)
  (let [props (System/getProperties)]
    (.setProperty props "mail.store.protocol", "imaps")
    (let [session (javax.mail.Session/getDefaultInstance props)
          store   (.getStore session "imaps")]
      (.connect store "imap.gmail.com" mail-list-user-address mail-list-password)
      (let [folders (.list (.getDefaultFolder store) "*")
            folder  (first (filter #(= folder-name (.getFullName %)) folders))]
        (.open folder javax.mail.Folder/READ_ONLY)
        (let [messages (make-messages (.getMessages folder))]
          ;; println is more than just debugging, it exercises lazy seq so folder/store isn't closed when
          ;; we try to use messages.  Yes there is a better way to do it
          (println "imap" folder-name "message count" (count messages)) 
          (.close folder false)
          (.close store) ; be nice to java
          messages) )))  )

(defn fetch
  ([] (fetch-imap "lispnyc"))
  ([folder-name] (fetch-imap folder-name)))
