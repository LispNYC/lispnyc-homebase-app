(ns org.lispnyc.webapp.homebase.simplecms-test
  (:use [org.lispnyc.webapp.homebase.simplecms] :reload-all)
  (:use [clojure.test]))

(deftest test-splice-in-list
  (is (= '(a b c d) (splice-in-list '(a B d) 1 '(b c)) ))
  )
