(ns org.lispnyc.webapp.homebase.servlet
  (:use ring.util.servlet)
  (:require org.lispnyc.webapp.homebase.core)
  (:gen-class :extends javax.servlet.http.HttpServlet))

(defservice org.lispnyc.webapp.homebase.core/app-routes) ; actual servlet
