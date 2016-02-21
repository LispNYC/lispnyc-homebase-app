(ns org.lispnyc.webapp.homebase.news
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [clojure.core.memoize                           :as memo]
            [clojure.set                                    :as set]
            [clojure.string                                 :as string]
            [clj-time.core                                  :as t]
            [clj-time.format                                :as tfmt]
            [net.cgrand.enlive-html                         :as enlive]
            [org.lispnyc.webapp.homebase.feed.google-plus   :as plus]
            [org.lispnyc.webapp.homebase.feed.pebble-blog   :as blog]
            [org.lispnyc.webapp.homebase.feed.reddit        :as reddit]
            [org.lispnyc.webapp.homebase.feed.twitter       :as twitter]
            [org.lispnyc.webapp.homebase.feed.hacker-news   :as hn]
            [org.lispnyc.webapp.homebase.feed.planet-lisp   :as pl]
            [org.lispnyc.webapp.homebase.feed.facebook      :as fb] 
            [org.lispnyc.webapp.homebase.feed.mail-list     :as list] ))

(def positive-keywords ["turing"
                        "hickey"
                        "alan kay"
                        "mccarthy"
                        "minksy"
                        "stallman"
                        "sussman"
                        " rms "
                        " esr "
                        "lisp"
                        "common-lisp"
                        "clos " ; not close
                        "clojure"
                        "clojurescript"
                        "concurrency"
                        "multiple"
                        "functional"
                        "repl"
                        "lein"
                        "closure"
                        "scheme"
                        "sbcl"
                        "clisp"
                        "lispnyc"
                        "lambda"
                        "abcl"
                        "allegro" ; catches allegrograph
                        "franz"
                        "symbolics"
                        " ecl "
                        "prolog"
                        "cliki"
                        "comp.lang"
                        "recursive"
                        "slime"
                        "symposium" 
                        "conference"
                        "meta"
                        "elisp"
                        "emacs"
                        "light table"
                        "lighttable"
                        "robot"
                        "artificial intelligence"
                        "meta-program"
                        "metaprogram"
                        "meta program"
                        "self-modify"
                        "self modify"
                        " ai "
                        " alu "
                        "immutab"
                        "sicp"
                        "datomic"
                        ])

(def negative-keywords ["pronounce"
                        "pronouncing"
                        "speech"
                        "impediment"
                        "braces"
                        "retainer"
                        "lispth"
                        "accent"
                        "thh"
                        "scheme_bot" ; too spammy
                        "ponzi"
                        "ponsi"
                        "schemes"
                        "replac" ; repl is part of a word
                        ])

(defn count-keywords
  "simple and stupid, yes it can count some things twice"
  [wordlist sentence]
  (let [lc (string/lower-case sentence)] ; not painfully inefficient
    (apply + (map #(if (.contains lc %) 1 0) wordlist))) )

(defn count-positive-keywords [sentence]
  (count-keywords positive-keywords sentence))

(defn count-negative-keywords [sentence]
  (count-keywords negative-keywords sentence))

;; TODO: make faster
;; http://shenfeng.me/using-tagsoup-extract-text-from-html.html
(defn- emit-str [node]
  (cond (string? node) node
        (and (:tag node)
             (not= :script (:tag node))) (emit-str (:content node))
             (seq? node) (map emit-str node)
             :else ""))

;; html parsing
(defn extract-text [html]
  (when html
    (let [r (enlive/html-resource (java.io.StringReader. html))]
      (string/trim (apply str (flatten (emit-str r)))))))

(defn remove-punct "remove all punctuation, leaving words"
  [s]
  (apply str (filter #(re-matches #"[a-zA-Z\\ ]" (str %1))
                     (string/lower-case s))))

(defn get-wordset "return a set of words, without punction or html"
  [n] (set (.split #" " (remove-punct (extract-text n)))))

(defn age-in-hours [now d]
  (if (nil? d) (do (println "WARNING: empty date!") 48) ; 2 days
      (if (.isBefore now d) (do (println "WARNING: future date!") 0)
          (float (/ (t/in-secs (t/interval d now))
                    3600)))))

(defn weigh [item]
  (let [now  (new org.joda.time.DateTime)
        age  (age-in-hours now (:pub-date item))
        ours 0 #_(cond
              (= :tweet-lispnyc (:type item)) 1
              (= :lispnyc-blog  (:type item)) 168 ; subsidise for 1 week 
              (= :lisp-group    (:type item)) 1
              :else 0)
        kw    (count-positive-keywords (:title item))]
    (assoc item
      :weight (+ (* -1 age)   ; negative age in hours
                 (* 0.9 ours) ; our content gets weighed up
                 (* 2 kw)
                 )) ))

(defn filter-by-keyword
  "must contain positive keywords and not contain any negative"
  ([pos-count records]
     (vec (filter #(and (>= (count-positive-keywords (:title %)) pos-count)
                        (=  (count-negative-keywords (:title %)) 0) )
                  records)))
  ([records] (filter-by-keyword 1 records)))

(defn filter-by-title
  "title must be real, > 13 chars"
  ([records] (vec (filter #(> (count (:title %)) 13 ) records))))

(defn make-dec-vec "make a vec: [5 4 3 2 1 0 0 0 0]"
  [max-val len]
  (take len (apply conj (vec (reverse (range 1 (+ 1 max-val))))
                        (vec (repeat (max 1 (- len max-val))  0)) )) )

(defn ensure-num "ensure input is an actual number"
  [n] (if (number? n) n 0))

(defn assoc-weight "[{}{}{}] => [{:rw 3}{:rw 2}{:rw 1}]"
  [max-val vec-recs]
  (vec (map #(assoc %1 :weight (ensure-num %2))
            vec-recs
            (make-dec-vec max-val (count vec-recs)) )))

(defn assoc-words
  "add wordset to each record, this is to find duplicates"
  [vec-recs] (vec (map #(assoc % :words (get-wordset (:title %))) vec-recs)))

(defn in? "true if seq contains e"
  [seq e] (some #(= e %) seq))

(def duplicate-threshold 0.7) ; 70% the same

;; TODO: optimize
(defn- find-dupes [recs]
  (if (empty? recs) '()
      (cons (if (not (in? (let [f (first recs)
                                r (rest recs)]
                            (if (<= (count (:words f)) 4) [false] 
                                (map #(if (> (/ (count (clojure.set/intersection
                                                        (:words f) (:words %)))
                                                (max 1 (count (:words f))))
                                             0.7)
                                        (do
                                          (comment println "dupicate A: " (apply str (take 10 (extract-text (:title f)))))
                                          (comment println "dupicate B: " (apply str (take 10 (extract-text (:title %)))))
                                          true)) r)) )
                          true))
              (first recs))
            (find-dupes (rest recs))) ))

(defn remove-dupes
  "remove duplicate records, uses wordsets (:words)"
  [vec-recs]
  (filter #(not (nil? %)) (find-dupes vec-recs)))

;; code-style is for thinking and debugging
(defn weigh-and-sort [visits items]
  (let [r (cond (= 1 visits) 10000
                (= 2 visits) 1000
                (= 3 visits) 100
                :else        1)
        w (cond (= 1 visits) 100
                (= 2 visits) 100
                (= 3 visits) 100
                (= 4 visits) 10
                (= 5 visits) 1
                :else        0)
        now     (now)]
    (comment println "v: " visits "r: " r " w: " w)
    (reverse (sort-by #(+ (* r  (ensure-num (:relevance %)))
                          (* w  (ensure-num (:weight %)))
                          (* -1 (max 1 (age-in-hours now (:pub-date %)))) ) items))))

(defn in-hrs [n] (int (* n 1000 60 60)))
(defn in-min [n] (int (* n 1000 60)))

;; memoize the working functions by time
(def fetch-list-lisp       (memo/memo-ttl #(list/fetch)             (in-min 33)))
(def fetch-fb-lispnyc      (memo/memo-ttl #(fb/fetch)               (in-min 32)))
(def fetch-fb-lispers      (memo/memo-ttl #(fb/fetch "269890189732269") (in-min 32)))
(def fetch-plus-lispnyc    (memo/memo-ttl #(plus/fetch)             (in-min 20)))
(def fetch-plus-cl         (memo/memo-ttl #(plus/fetch "101016130241925650833" 0.5) (in-min 21)))
(def fetch-plus-clj        (memo/memo-ttl #(plus/fetch "103410768849046117338" 0.5) (in-min 22)))
(def fetch-pebble-blog     (memo/memo-ttl #(blog/fetch)             (in-min 20)))
(def fetch-hacker-news     (memo/memo-ttl #(hn/fetch)               (in-hrs 4)))
(def fetch-planet-lisp     (memo/memo-ttl #(pl/fetch)               (in-hrs 6)))
(def fetch-reddit-lisp     (memo/memo-ttl #(reddit/fetch "lisp")    (in-hrs 1.1)))
(def fetch-reddit-clojure  (memo/memo-ttl #(reddit/fetch "clojure") (in-hrs 1.2)))
(def fetch-reddit-scheme   (memo/memo-ttl #(reddit/fetch "scheme")  (in-hrs 1.3)))
(def fetch-twitter-lispnyc (memo/memo-ttl #(twitter/fetch)          (in-min 21)))
(def fetch-twitter-friends (memo/memo-ttl #(twitter/fetch-friends)  (in-min 22)))
(def fetch-twitter-clojure (memo/memo-ttl #(twitter/fetch "clojure")(in-hrs 23)))
(def fetch-twitter-lisp    (memo/memo-ttl #(twitter/fetch "lisp")   (in-hrs 24)))
(def fetch-twitter-scheme  (memo/memo-ttl #(twitter/fetch "scheme") (in-min 25)))

;; store the good values
(def duped-feed-data { :list-lisp (atom []) })

(def feed-data
  {:fb-lispnyc        (atom [])
   :fb-lispers        (atom [])
   :plus-lispnyc      (atom [])
   :plus-cl           (atom [])
   :plus-clj          (atom [])
   :pebble-blog       (atom [])
   :hacker-news       (atom [])
   :planet-lisp       (atom [])
   :reddit-lisp       (atom [])
   :reddit-clojure    (atom [])
   :reddit-scheme     (atom [])
   :twitter-lispnyc   (atom [])
   :twitter-friends   (atom [])
   :twitter-clojure   (atom [])
   :twitter-lisp      (atom [])
   :twitter-scheme    (atom [])
   } )

;; return last good values during an error
(defmacro set-data! [form data-recs key]
  `(try
     (reset! (~data-recs ~key) ~form)
     (deref  (~data-recs ~key))
     (catch java.lang.Exception e# (deref (~data-recs ~key)))))

(defn member? 
  "true if list contains at least one instance of elt"
  [list elt]
  (cond (empty? list) false
        (= (first list) elt) true
        true (member? (rest list) elt)))

(defmacro add-new-data! 
  "only add new records to existing list" 
  [form data-recs key]
  `(try
     (swap! (~data-recs ~key) concat (filter #(not (member? (deref (~data-recs ~key)) %)) ~form))
     (deref (~data-recs ~key))
     (catch java.lang.Exception e# (deref (~data-recs ~key)))))

(defn filtered-fetch []
  (concat 
   @(:list-lisp duped-feed-data) ; don't dedup our mailing list
   (remove-dupes
    (do ;; release the threads!  Note: it's faster to let the thread assoc-words
      (.start (Thread. #(set-data! (assoc-words (assoc-weight 10                  (fetch-list-lisp)))       duped-feed-data :list-lisp)))      
      (.start (Thread. #(set-data! (assoc-words (filter-by-title (assoc-weight 9  (fetch-fb-lispnyc))))     feed-data :fb-lispnyc)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-title (assoc-weight 9  (fetch-fb-lispers))))     feed-data :fb-lispers)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-title (assoc-weight 10 (fetch-plus-lispnyc))))   feed-data :plus-lispnyc)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-title (assoc-weight 8  (fetch-plus-cl))))        feed-data :plus-cl)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-title (assoc-weight 8  (fetch-plus-clj))))       feed-data :plus-clj)))
      (.start (Thread. #(set-data! (assoc-words (assoc-weight 10                  (fetch-pebble-blog)))     feed-data :pebble-blog)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-keyword                (fetch-hacker-news)))     feed-data :hacker-news)))
      (.start (Thread. #(set-data! (assoc-words (assoc-weight 10                  (fetch-planet-lisp)))     feed-data :planet-lisp)))
      (.start (Thread. #(set-data! (assoc-words (assoc-weight 8                   (fetch-reddit-lisp)))     feed-data :reddit-lisp)))
      (.start (Thread. #(set-data! (assoc-words (assoc-weight 8                   (fetch-reddit-clojure)))  feed-data :reddit-clojure)))
      (.start (Thread. #(set-data! (assoc-words (assoc-weight 8                   (fetch-reddit-scheme)))   feed-data :reddit-scheme)))
      (.start (Thread. #(set-data! (assoc-words (assoc-weight 7                   (fetch-twitter-lispnyc))) feed-data :twitter-lispnyc)))
;      (.start (Thread. #(set-data! (assoc-words (fetch-twitter-lispnyc)) feed-data :twitter-lispnyc)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-keyword                (fetch-twitter-friends))) feed-data :twitter-friends)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-keyword                (fetch-twitter-clojure))) feed-data :twitter-clojure)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-keyword 2              (fetch-twitter-lisp)))    feed-data :twitter-lisp)))
      (.start (Thread. #(set-data! (assoc-words (filter-by-keyword 2              (fetch-twitter-scheme)))  feed-data :twitter-scheme)))
      ;; meh, send back whatever happens to be in our buckets
      (vec (flatten (map deref (vals feed-data)))) ))))

(defn fetch
  ([visits] (weigh-and-sort visits (filtered-fetch)))
  ([]       (weigh-and-sort 1 (filtered-fetch))))
