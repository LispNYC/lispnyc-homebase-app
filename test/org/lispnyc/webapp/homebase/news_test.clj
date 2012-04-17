(ns org.lispnyc.webapp.homebase.news-test
  (:use [org.lispnyc.webapp.homebase.news] :reload-all)
  (:use [clojure.test])
  (:require   [clojure.set :as set]) )

(deftest test-keyword-count
  (is (= 0 (count-positive-keywords "hello there")))
  (is (= 1 (count-positive-keywords "hello there Rich Hickey")))
  (is (= 1 (count-positive-keywords "hello there rich hickey")))
  )

(deftest test-filtering
  (let [recs-plus  [{ :title "Hello there Right Hickey" }]
        recs-minus [{ :title "My braces cause a lisp" } ]
        recs-none  [ { :title "noting interesting at all" } ]
        recs       (set/union recs-plus recs-minus recs-none)
        ]
        (is (= 1 (count (filter-by-keyword recs))))
        (is (= 1 (count (filter-by-keyword recs-plus))))
        (is (= 0 (count (filter-by-keyword recs-minus))))
        (is (= 0 (count (filter-by-keyword recs-none))))
    )
  )