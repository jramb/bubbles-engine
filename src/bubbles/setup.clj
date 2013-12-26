(ns bubbles.setup
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Setup related (obviously)"
    :url "https://github.com/jramb/bubbles-engine"}
  (:require [clojure.tools.logging :as log])
  (:require [bubbles.bust :as bust]))

(def *config* (atom {}))

(defn load-config
  "Loads configuration from file-name. Setup is stored in
buse/*config*, but the db-part is copied to atoms in bust!"
  [file-name]
  (log/info (str "config file: " file-name))
  (let [new-config (load-file file-name)#_(read-string (slurp file-name))]
    (bust/connect-db new-config)        ;note: copies part of config to an atom in bust
    (reset! *config* new-config)))

(defn pretty-print-response?
  []
  (:pretty-print-response @*config*))

(defn web-root
  []
  (:web-root @*config* "https://github.com/jramb/bubbles-engine"))

;; jetty-config: http://mmcgrana.github.com/ring/ring.adapter.jetty.html
(defn jetty-config
  []
  (into {:port 8080 :join? false} ; :join? false => will not block
        (:jetty @*config*)))

(defn mail-config
  []
  (assert (:mail @*config*))
  (:mail @*config*))

(defn check-access
  "This is called on every buse request. If this returns false then noting is returned to request. To be refined."
  [req]
  ;;(log/debug req)
  ;;(not= (:remote-addr req) "0:0:0:0:0:0:0:1")
  true)


(defn domain-setup
  "Returns the domain setup or nil of this is an invalid domain"
  [domain]
  (get-in @*config* [:domains domain]))


(defn is-admin? [req]
  (let [domain        (get-in req [:params :domain])
        domain-admins (:admin (domain-setup domain))
        current-user  (get-in req [:session :id :name])]
    (some #(= current-user %) domain-admins)))


