(ns org.lispnyc.webapp.homebase.feed.meetup
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clj-time.coerce        :as tcoerce]))

(defn- decomp-item [data target-keyword]
  (enlive/text (first (enlive/select data target-keyword))))

(defn- make-event [item]
  (hash-map :type :meetup-past
            :title        (decomp-item item [:item :> :name])
            :description  (apply str (decomp-item item [:item :> :description]))
            :venue        (decomp-item item [:item :> :venue :> :name])
            :time         (tcoerce/from-long (str->long (decomp-item item [:item :> :time])))
            :meeting-date (decomp-item item [:item :> :time])
            :address      (decomp-item item [:item :> :venue :> :address_1])
            :address2     (decomp-item item [:item :> :how_to_find_us])
            :event-url    (decomp-item item [:item :> :event_url])))

(defn fetch-past-meetups []
  (let [data (fetch-url "http://api.meetup.com/2/events.xml?status=past&group_urlname=LispNYC&key=5247666759864d49d1e1a2b263463")]
    (map #(make-event %)
         (enlive/select data #{[:item]}))
    ))

(defn fetch-meetup []
  (let [data (fetch-url "http://api.meetup.com/2/events.xml?group_urlname=LispNYC&key=5247666759864d49d1e1a2b263463")
        item (enlive/select data #{[:items (enlive/nth-child 1)]})]
    (make-event item)))

(comment defn- decomp-meetup "Decompose by grabbing the first child data in the xml, which is our next meeting."
  [data target-keyword]
  (first
   (map enlive/text
        (enlive/select data [:items (enlive/nth-child 1) target-keyword]))))


(comment defn fetch-meetup []
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