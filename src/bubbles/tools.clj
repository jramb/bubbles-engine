(ns bubbles.tools
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Tools, used by bubble methods!"
    :url "https://github.com/jramb/bubbles-engine"}
  ;;(:gen-class)
  (:require [bubbles.bust :as bust])
  (:require [bubbles.setup :as setup])
  (:require [cheshire.core :as json])
  (:require [clojure.tools.logging :as log])
  (:require [postal.core :as postal])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def VALID-CHARS
  (map char (concat (range 48 58) ; 0-9
                    (range 65 91) ; A-Z
                    (range 97 123)))) ; a-z

(defn random-str [length]
  (apply str (take length (repeatedly #(rand-nth VALID-CHARS)))))

;(dotimes [x 30] (println (random-str 6)))
(defn gen-uuid
  "Returns a string with at least as many randomness as a UUID."
  []
  (random-str 22))

(defn new-bubble
  "Creates a new bubble"
  [self bubble]
  (let [bubble (assoc bubble
                      :domain (:domain self)
                      :uuid (or (:uuid bubble) (gen-uuid))
                      :parent (:uuid self))]
    (bust/db-create-bubble bubble)))

(defn send-message
  "Sends a message to a bubble"
  ([self bubble-id message parameter]
    (bust/db-message {:domain (:domain self)
                      :uuid bubble-id} message parameter))
  ([self bubble-id message] (send-message self bubble-id message nil)))

(defn extract-full
  "Extracts from a bubble (with type)"
  ([self bubble-id extr parameter]
    (bust/db-extract {:domain (:domain self)
                      :uuid bubble-id} extr parameter))
  ([self bubble-id extr] (extract-full self bubble-id extr nil)))

(defn extract
  "Extracts from a bubble"
  ([self bubble-id extr parameter]
    (:body (extract-full self bubble-id extr parameter)))
  ([self bubble-id extr]
    (:body (extract-full self bubble-id extr nil))))

(defn add-ref
  "Adds or updates a bubble ref"
  [self refuuid type order]
  (bust/db-make-ref self refuuid type order))

(defn remove-ref
  "Removes a bubble ref"
  [self refuuid type]
  (bust/db-remove-ref self refuuid type))

(defn get-refs
  "Fetches bubble refs"
  ([self type]
    (get-refs self type nil))
  ([self type params]
    (vec (bust/db-get-refs self type params))))

(defn log-info
  "Writes info to the log"
  [& stuff]
  (log/info (apply str (interpose " " stuff))))

(def validated-user bust/validate-bubble-login)

(defn require-authentication
  "Raises the demand for authentication"
  [realm]
  (throw+ {:bust :need-authorization :realm realm}))

(defn request-info
  []
  bust/*request*)
;;(clojure.core/assert)
(defn verify; overrides clojure.core macro
  [assertion failed-string]
  (when-not assertion
    (throw (Exception. failed-string))))

(defn backend
  "Calls the backend and returns the result (str)."
  [self message params]
  (bust/db-backend self message params))

(def as-json
  ;;"Returns the given structure as a JSON string."[structure]
  json/generate-string)

(defn get-state
  "Returns the bubbles state using the bubble hierarchy"
  ([self]
     (bust/bubble-full-state self))
  ([self ks]
     (bust/state-get-in self ks))
  ([self ks not-found]
     (bust/state-get-in self ks not-found)))

(defn send-mail
  "Sends an email. Result is a map like this:
:code 0, :error :SUCCESS, :message 'message sent'
Throws an exception otherwise!"
  [self to subject body]
  (when (and to subject body)
    (let [mailer-setup (setup/mail-config)
          ;; body (str body "\n---\nDebug-info: " (get-in self [:params]))
          message {:from (format "Bubble %s/%s <bubble.noreply@gmail.com>"
                                 (:domain self)
                                 (:uuid self))
                   :to to
                   :subject subject
                   :body body
                   }]
      (log/info
       (str "send-mail: " message
            "->"
            (postal/send-message
             (with-meta
               (merge (:defaults mailer-setup) message)
               (:mailer mailer-setup))))))))

(comment
  (send-mail nil
   "jorg@example.org"
   "Testing email from bubbles"
   "Denna epost skickades av bubble-motorn.
Du behöver inte göra något med det eller agera på det.

Ha en bra dag!

Jörg"))



