(ns org.lispnyc.webapp.homebase.feed.meetup
  (:use     org.lispnyc.webapp.homebase.feed.util)
  (:require [net.cgrand.enlive-html :as enlive]
            [clj-time.core          :as time]
            [clj-time.coerce        :as tcoerce]))

(defn- decomp-item [data target-keyword]
  (enlive/text (first (enlive/select data target-keyword))))

(defn- make-event [item]
  (hash-map :type :meetup-past
            :title        (decomp-item item [:item :> :name])
            :description  (apply str (decomp-item item [:item :> :description]))
            :venue        (decomp-item item [:item :> :venue :> :name])
            :time         (time/to-time-zone (tcoerce/from-long (str->long (decomp-item item [:item :> :time])))
                                             (time/time-zone-for-id "America/New_York"))
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

