(ns org.lispnyc.webapp.homebase.feed.twitter-creds
  (:require twitter.oauth))

;; insert your twitter credentials here, create a twitter application
(def twit-creds (twitter.oauth/make-oauth-creds *app-consumer-key*
                                                *app-consumer-secret*
                                                *user-access-token*
                                                *user-access-token-secret*
                                                ))
