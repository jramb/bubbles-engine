(ns bubbles.store.protocol
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Backend Store protocol"
    :url "https://github.com/jramb/bubbles-engine"}
  (:gen-class))


(defprotocol Store
  (startup [this] "Run once on startup of the engine")
  (ping [this] "Returns a map with :name, :now, :num_bubbles.")
  (fetch [this domain uuid] "Fetches a bubble by uuid")
  (create [this bubble done?] "Creates the bubble in the store")
  (update [this bubble new-bubble done?] "Updates the bubble in the store")
  (add-ref [this bubble refuuid type order] "Adds a reference.")
  (remove-ref [this bubble refuuid type] "Removes a reference.")
  (get-refs [this bubble type params])
  (fetch-by-name [this domain name] "Fetches a bubble by name") ; do we need this?!
  (fetch-all [this domain] "Fetches all bubbles of the domain") ; do we need this??
  (fetch-all-by-type [this domain type which] "Fetches all code bubbles of the domain, which is :code :data or :all")
  (backend [this bubble message params] "Calls the backend with message and optional parameters")
  )


(defn read-clob
  "Reads (as in clojure reader) the stream of clojure expression passed as a CLOB."
  [clob]
  (if (string? clob)
    (when-not (empty? clob)
      (read-string clob))
    ; not a str, assume Oracle CLOB: class oracle.sql.CLOB
    (when clob
      (read (java.io.PushbackReader. (.getCharacterStream clob))))
    ))

(defn db-row-to-bubble [db-row]
  {:uuid          (:uuid db-row)
   :domain        (:domain db-row)
   :name          (:name db-row)
   :parent        (:parent db-row) 
   :creation_date (str (:creation_date db-row))
   :update_date   (str (:update_date db-row))
   :version       (:version db-row)
   :state         (read-clob (:state db-row))
   :code          (read-clob (:code db-row))
   :type          (:type db-row)
   :deleted       (= (:deleted_flag db-row) "Y")
  })

(defn short-bubble [db-row]
  (if db-row
    {:uuid          (:uuid db-row)
     :name          (:name db-row)
     :creation_date (str (:creation_date db-row))
     :update_date   (str (:update_date db-row))
     :type          (:type db-row)
     }))


(defn str-nn
  [v]
  (if v (str v)))

