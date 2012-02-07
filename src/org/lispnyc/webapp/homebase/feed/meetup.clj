(ns org.lispnyc.webapp.homebase.feed.meetup
  (:require [net.cgrand.enlive-html :as enlive]))

(defn fetch-url [url]
  (enlive/html-resource (java.net.URL. url)))

(defn- decomp-meetup "Decompose by grabbing the first child data in the xml, which is our next meeting."
  [data target-keyword]
  (first
   (map enlive/text
        (enlive/select data [:items (enlive/nth-child 1) target-keyword]))))

(defn fetch-meetup []
  (let [meetup-data (fetch-url "http://api.meetup.com/events.xml?group_urlname=LispNYC&key=5247666759864d49d1e1a2b263463")
        time        (decomp-meetup meetup-data :time)
        venue       (decomp-meetup meetup-data :venue_name)
        map-url     (decomp-meetup meetup-data :venue_map)]
    {
     :title       (decomp-meetup meetup-data :name)
     :description (decomp-meetup meetup-data :description)
     :venue       (if (= 0 (count venue))
                    "no venue selected"
                    (str "<a href=\""
                         map-url "\" target=\"_blank\">"
                         venue " (map)</a>"))
     :time        (. time substring 0 (- (count time) 12)) ; remove year
     :address     (decomp-meetup meetup-data :venue_address1)
     :address2    (decomp-meetup meetup-data :how_to_find_us)
     :map-url     map-url
     } ))
