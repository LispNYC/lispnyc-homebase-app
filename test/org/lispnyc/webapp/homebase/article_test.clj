(ns org.lispnyc.webapp.homebase.article-test
  (:use [org.lispnyc.webapp.homebase.article] :reload-all)
  (:use [clojure.test]))

(deftest test-metadata
  (is (= false (metadata? nil) ))
  (is (= false (metadata? "") ))
  (is (= true  (metadata? "foo: bar") ))
  (is (= true  (metadata? "foo: hi there") ))
  (is (= true  (metadata? " foo: hi there") ))
  )

(deftest test-slash-str
  (is (= "/hello-world" (slash-str "hello-world")))
  (is (= "/hello-world" (slash-str "/hello-world")))
  (is (= "/" (slash-str "")))
  (is (= "/" (slash-str nil)))
  )

(deftest test-make-vec
  (is (= (make-vec "foo, bar, baz")  ["foo" "bar" "baz"]))
  (is (= (make-vec "foo, bar,, baz") ["foo" "bar" "baz"]))
  (is (= (make-vec "foo,  bar, baz") ["foo" "bar" "baz"]))
  (is (= (make-vec "foo,bar,baz")    ["foo" "bar" "baz"]))
  (is (= (make-vec "") []))
  (is (= (make-vec nil) []))
  )

(def partial-article-text
     "   title: How not to do HTML
description: RED GREEN BLUE

Hi there
- h")

(deftest test-article
  (let [a (make-article partial-article-text)]
    (is (= (:title a)      "How not to do HTML"))
    (is (= (:uri-path a)   ""))
    (is (= (:author a)     ""))
    (is (= (:description a) "RED GREEN BLUE"))
    ))


(def basic-article-text
     "   title: How not to do HTML
   uri-path: blog/meh.html
     author: Heow Goodman
       tags: red, white, blue
description: RED GREEN BLUE

Hi there
========

Foo, bar, baz and a big meh.

Otherwise we're all ok, because today is the day to test wether it's easy or not to read and associate articles.

ok.

- h")

(deftest test-article
  (let [a (make-article basic-article-text)]
    (is (= (:title a)      "How not to do HTML"))
    (is (= (:uri-path a)   "/blog/meh.html"))
    (is (= (:author a)     "Heow Goodman"))
    (is (= (:tags a)        ["red" "white" "blue"]))
    (is (= (:description a) "RED GREEN BLUE"))
    ))