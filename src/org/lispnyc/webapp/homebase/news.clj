(ns org.lispnyc.webapp.homebase.news
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [clojure.core.memoize                           :as memo]
            [clojure.set                                    :as set]
            [clojure.string                                 :as string]
            [clj-time.core                                  :as t]
            [clj-time.format                                :as tfmt]
            [org.lispnyc.webapp.homebase.feed.google-groups :as goog]
            [org.lispnyc.webapp.homebase.feed.pebble-blog   :as blog]
            [org.lispnyc.webapp.homebase.feed.reddit        :as reddit]
            [org.lispnyc.webapp.homebase.feed.twitter       :as twitter]
            [org.lispnyc.webapp.homebase.feed.hacker-news   :as hn]
            [org.lispnyc.webapp.homebase.feed.planet-lisp   :as pl]))

(def positive-keywords ["turing"
                        "hickey"
                        "alankay"
                        "mccarthy"
                        "minksy"
                        "lisp"
                        "common-lisp"
                        "clojure"
                        "clojurescript"
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
                        "allegro"
                        "franz"
                        "symbolics"
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
                        "robot"
                        "artificial intelligence"
                        " ai "])

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

(defn age-in-hours [now d]
  (float (/ (t/in-secs (t/interval d now))
            3600) ))

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
     (filter #(and (>= (count-positive-keywords (:title %)) pos-count)
                   (=  (count-negative-keywords (:title %)) 0) )
             records))
  ([records] (filter-by-keyword 1 records)))

(defn make-dec-vec
  "make a vec: [5 4 3 2 1 0 0 0 0]"
  [max-val len]
  (take len (apply conj (vec (reverse (range 1 (+ 1 max-val))))
                        (vec (repeat (max 1 (- len max-val))  0)) )) )

(defn assoc-weight
  "[{}{}{}] => [{:rw 3}{:rw 2}{:rw 1}]"
  [max-val vec-recs]
  (vec (map #(assoc %1 :weight %2)
            vec-recs
            (make-dec-vec max-val (count vec-recs)) )))

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
    (println "v: " visits "r: " r " w: " w)
    (reverse (sort-by #(+ (* r  (:relevance %))
                          (* w  (:weight %))
                          (* -1 (age-in-hours now (:pub-date %)))) items))))

(defn in-hrs [seconds] (int (* seconds 1000 60 60)))
(defn in-min [seconds] (int (* seconds 1000 60)))

;; memoize the working functions by time
(def fetch-google-groups   (memo/memo-ttl #(goog/fetch)             (in-hrs 4)))
(def fetch-pebble-blog     (memo/memo-ttl #(blog/fetch)             (in-min 20)))
(def fetch-hacker-news     (memo/memo-ttl #(hn/fetch)               (in-hrs 1)))
(def fetch-planet-lisp     (memo/memo-ttl #(pl/fetch)               (in-hrs 6)))
(def fetch-reddit-lisp     (memo/memo-ttl #(reddit/fetch "lisp")    (in-hrs 1.1)))
(def fetch-reddit-clojure  (memo/memo-ttl #(reddit/fetch "clojure") (in-hrs 1.2)))
(def fetch-reddit-scheme   (memo/memo-ttl #(reddit/fetch "scheme")  (in-hrs 1.3)))
(def fetch-twitter-lispnyc (memo/memo-ttl #(twitter/fetch)          (in-min 20)))
(def fetch-twitter-friends (memo/memo-ttl #(twitter/fetch-friends)  (in-min 21)))
(def fetch-twitter-clojure (memo/memo-ttl #(twitter/fetch "clojure")(in-hrs 1.4)))
(def fetch-twitter-lisp    (memo/memo-ttl #(twitter/fetch "lisp")   (in-hrs 1.5)))
(def fetch-twitter-scheme  (memo/memo-ttl #(twitter/fetch "scheme") (in-min 22)))

;; store the previously-known good values as a fallback
(def prev-google-groups   (atom []))
(def prev-pebble-blog     (atom []))
(def prev-hacker-news     (atom []))
(def prev-planet-lisp     (atom []))
(def prev-reddit-lisp     (atom []))
(def prev-reddit-clojure  (atom []))
(def prev-reddit-scheme   (atom []))
(def prev-twitter-lispnyc (atom []))
(def prev-twitter-friends (atom []))
(def prev-twitter-clojure (atom []))
(def prev-twitter-lisp    (atom []))
(def prev-twitter-scheme  (atom []))

;; return last good values
(defmacro wrap-catch [form last-good]
  `(try
     (reset! ~last-good ~form)
     (deref ~last-good)
     (catch java.lang.Exception e# (deref ~last-good))))

(defn filtered-fetch []
  (apply set/union ; make one list
           (pvalues ; run them all in parallel
            (wrap-catch (assoc-weight 10     (fetch-google-groups)) prev-google-groups)
            (wrap-catch (assoc-weight 10     (fetch-pebble-blog)) prev-pebble-blog)
            (wrap-catch (filter-by-keyword   (fetch-hacker-news)) prev-hacker-news)
            (wrap-catch (assoc-weight 10     (fetch-planet-lisp)) prev-planet-lisp)
            (wrap-catch (assoc-weight 10     (fetch-reddit-lisp)) prev-reddit-lisp)
            (wrap-catch (assoc-weight 10     (fetch-reddit-clojure)) prev-reddit-clojure)
            (wrap-catch (assoc-weight 10     (fetch-reddit-scheme)) prev-reddit-scheme)
            (wrap-catch (assoc-weight 10     (fetch-twitter-lispnyc)) prev-twitter-lispnyc)
            (wrap-catch (filter-by-keyword   (fetch-twitter-friends)) prev-twitter-friends)
            (wrap-catch (filter-by-keyword   (fetch-twitter-clojure)) prev-twitter-clojure)
            (wrap-catch (filter-by-keyword 2 (fetch-twitter-lisp)) prev-twitter-lisp)
            (wrap-catch (filter-by-keyword 2 (fetch-twitter-scheme)) prev-twitter-scheme)
            )))

(defn fetch
  ([visits] (weigh-and-sort visits (filtered-fetch)))
  ([]       (weigh-and-sort 1 (filtered-fetch))))

