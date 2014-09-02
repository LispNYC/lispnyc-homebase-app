(ns org.lispnyc.webapp.homebase.feed.twitter-creds
  (:require twitter.oauth))

;; create a twitter application and insert your twitter credentials
(def twit-creds (twitter.oauth/make-oauth-creds *app-consumer-key*
                                                *app-consumer-secret*
                                                *user-access-token*
                                                *user-access-token-secret*
                                                ))
