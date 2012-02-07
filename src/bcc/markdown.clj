(ns bcc.markdown
  (:require [clojure.contrib.duck-streams :only slurp* :as ds])
  (:import (org.mozilla.javascript Context ScriptableObject)))

(defn markdown-to-html [txt]
  (let [cx (Context/enter) scope (.initStandardObjects cx)
        input (Context/javaToJS txt scope)
        script (str
                (ds/slurp*
                 (-> (clojure.lang.RT/baseLoader)
                     (.getResourceAsStream "html/showdown.js")))
                "\n"
                "new Showdown.converter().makeHtml(input);")]
    (try
     (ScriptableObject/putProperty scope "input" input)
     (let [result (.evaluateString cx scope script "<cmd>" 1 nil)]
       (Context/toString result))
     (finally (Context/exit)))))
