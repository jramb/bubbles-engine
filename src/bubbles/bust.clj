(ns bubbles.bust
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "BUST is the BUbble STore. This namespace is the API working with the protocol
         (not directly). The store is some kind of database.
         Setup is done in config.clj and the :db :subprotocol must be supported here."
    :url "https://github.com/jramb/bubbles-engine"}
  (:gen-class)
  (:require [bubbles.jsonrpc :as jsonrpc])
  (:require [bubbles.store protocol]);;was: sqlite oracle
  ;;;; (require 'bubbles.tools) below AFTER code (WORKAROUND!)
  (:require [clojail.core :as jail])
  (:require [clojail.testers :as jailt])
  (:require [clojure.java.jdbc :as sql])
  (:require [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:import javax.sql.DataSource)   ; com.mchange.v2.c3p0.DataSources)
  ;;(:require )
  ;;(:use )
  ;;(:import )
  )

(def sb (jail/sandbox jailt/secure-tester :timeout 10000))

#_(err/deferror *bust-exception* []
  [status]
  {:msg (str "Bust exception status " status)
   :unhandled (err/throw-msg Exception)})

#_(err/deferror *bust-bad-request* [*bust-exception*] []
  {:msg (str "Bad request")})

#_(err/deferror *bust-bubble-does-not-exist* [*bust-exception*] []
  {:msg (str "Bubble does not exist")})

#_(err/deferror *bust-need-authorization* [*bust-exception*] [realm]
  {:msg (str "Authorization needed: " realm)})

#_(err/deferror *bust-message-not-found* [*bust-exception*] [message]
  {:msg (str "Message not found: " message)})

#_(err/deferror *bust-bubble-frozen* [*bust-exception*] []
  {:msg (str "Bubble is frozen")})

#_(err/deferror *bust-bubble-already-exist* [*bust-exception*] []
  {:msg (str "Bubble already exists")})

#_(err/deferror *bust-filter-vetoed* [*bust-exception*] []
  {:msg (str "Some filter vetoed")})

;;;;;;;;;;;;;;;;;;;;;;;;;
;; 2010-09-03 J Ramb, Navigate Consulting
;;;;;;;;;;;;;;;;;;;;;;;;;

;(comment def dbs
;  {:saturn  { :classname "oracle.jdbc.driver.OracleDriver"
;              :subprotocol "oracle:thin"
;              :subname "apps/apps@//172.16.10.101:1521/VIS11"  }
;   :jupiter { :classname "oracle.jdbc.driver.OracleDriver"
;              :subprotocol "oracle:thin"
;              :subname "apps/apps@//172.16.10.100:1521/VIS12"  }})

(defn gen-uuid
  "Returns a new UUID without the dashes. Take good care of it, it is unique!"
  []
  (apply str (re-seq #"[^-]" (str (java.util.UUID/randomUUID)))))
;;(str (java.util.UUID/randomUUID))

(def all-config (atom {})) ; holds the complete configuration
(def db-connection (atom {})) ; used in sql/with-connection
(def db-trace (atom false)) ; should the connection be traced?
(def db-store (atom nil)) ; store interface
(def use-sandboxing (atom true)) ; eval or sandbox?

(def *request* nil)

;; (macroexpand-1 '(pingdb))
;; (with-bust (bubbles.store.protocol/ping @db-store))
(comment
   (with-bust (sql/with-query-results rs
                ["select name, now, num_bubbles from xxb_ping where name = ? for update" "PostgreSQL"]
                (first rs))))
;; (with-bust ()
;; @db-connection
;; @db-store


(defmacro with-bust
    ;; old version
  [& body]
  ;; (:serialized @db-connection)...?
  `(if (get-in @all-config [:db :serialized])
     (locking db-connection               ;FIXME
       (sql/with-connection @db-connection
         (sql/transaction ~@body)))
     (sql/with-connection @db-connection
       (sql/transaction ~@body))))

#_(defmacro with-bust
  [& body]
  ;; FIXME: this is always locking!!
  ;; (:serialized @db-connection)...?
  `(if @db-connection      ;is there any SQL necessary
     (if (get-in @all-config [:db :serialized])
       (locking db-connection           ;FIXME
         (sql/with-connection @db-connection
           (sql/transaction ~@body)))
       (sql/with-connection @db-connection
         (sql/transaction ~@body)))
     ;; else no SQL
     (if (get-in @all-config [:db :serialized])
       (locking db-store
         ~@body)
       (do ~@body))))


(defn connect-db [config]
  (reset! all-config config)
  (reset! db-store (.newInstance (Class/forName (:store config))))
  (reset! use-sandboxing (or (nil? (:use-sandboxing config))  ; default is true!
                             (:use-sandboxing config)))
  (if-let [db (:db config)]
    (let [jdbc (:connection-uri db)
          classname (:classname db)]
      (reset! db-trace (:trace db))     ;is this ever used? FIXME
      (if (:pooled db)
        (let [jdbc (or jdbc (str "jdbc:" (:subprotocol db) ":" (:subname db)))
              ds   (doto
                       (com.mchange.v2.c3p0.ComboPooledDataSource.) ; class loading has side effect... 8-/
                     (.setDriverClass classname)
                     (.setJdbcUrl jdbc)
                     ;;(.setConnectionTesterClassName "com.mysql.jdbc.integration.c3p0.MysqlConnectionTester")
                     (.setIdleConnectionTestPeriod 120)
                     )]
          (if-let [user (:user db)]
            (.setUser ds user))
          (if-let [passwd (:passwd db)]
            (.setPasswd ds passwd))          
          (reset! db-connection {:datasource ds}))
        ;;do not use pooling:
        (reset! db-connection db)))
    (do
      (reset! db-connection nil)
      (reset! db-trace false)))
  (with-bust
    (bubbles.store.protocol/startup @db-store))
  (println "db-store:" (type @db-store))
  (println "db-connection:" @db-connection)
  (println "use-sandboxing:" @use-sandboxing))



;;(reset! db-connection {})
;;Test with C-c M-m: (macroexpand-1 '(with-bust "Hej"))

(defn pingdb []
  {:body
   (let [p (bubbles.store.protocol/ping @db-store)]
     ;; e.g. sqlite returns string, other DBs return a Date (or Timestamp)
     (update-in p [:now] #(try (.getTime %) (catch Exception e (str %)))))})


(defn db-retrieve-all-bubbles [bubble]
  (assert (map? bubble))
  ;(log/debug (str "Fetching all bubbles " (:domain bubble)))
  (let [domain (:domain bubble)]
    {:result
     (apply vector
        (bubbles.store.protocol/fetch-all @db-store domain))}))

(defn db-retrieve-bubble-silent [bubble]
  (assert (map? bubble))
  ;;(log/debug (str "Fetching bubble " (:uuid bubble) "/" (:domain bubble) " " (:name bubble)))
  (let [uuid (:uuid bubble)
        domain (:domain bubble)]
    (if uuid
      (bubbles.store.protocol/fetch @db-store domain uuid)
      (bubbles.store.protocol/fetch-by-name @db-store domain (:name bubble)))))

(defn db-retrieve-bubble [bubble]
  (let [fetched (db-retrieve-bubble-silent bubble)]
    (if fetched
      fetched
      (throw+ {:bust :bubble-does-not-exist}))))

(defn db-backend
  "Calls backend functionality (i e sends a message)"
  [bubble message params]
  ;; (when-not (and refuuid type order (bubble :uuid)
  ;;             (empty-or-exists bubble (:uuid bubble))
  ;;             (empty-or-exists bubble refuuid))
  ;;   (throw+ {:bust :bad-request})); 400
  (bubbles.store.protocol/backend @db-store bubble message params))

;;FIXME: should take care of both :type and :parent somehow.
(defn db-get-bubbles-of-type
  "Returns a list of all code bubbles with the given type.
  If type is nil then returns all code bubbles without a type.
  which is either :code :data or :all"
  [domain type which]
  (bubbles.store.protocol/fetch-all-by-type @db-store domain type which))

;; FIXME: is "get-code-bubbles" really necessary?
(defn get-code-bubbles
  [domain uuid]
  ; FIXME remove never-ending loops, maybe even recursion?
  (let [s (db-get-bubbles-of-type domain uuid :code)
        l (when (not (empty? s))
            (vec (doall (map #(get-code-bubbles domain %) s))))]
     (if uuid
       (if (empty? l) uuid [uuid l])
       (if (empty? l) nil l))))


(defn type-bubble
  "Returns the type bubble of the given bubble."
  [bubble]
  (if-let [t (:type bubble)]
    (db-retrieve-bubble (assoc bubble :uuid t))))

(comment this does not work because of the lazyness in interater, it forgets about the connection...
  ;defn bubble-ancestors
  "Returns a lazy list of all a bubbles ancestors, bubble must be initialized."
  [bubble]
  (iterate type-bubble bubble))

(defn state-parent
  "The state parent is either the :parent or (if :parent is empty) the :type"
  [bubble]
  (or (:parent bubble)
      (:type bubble)))

(defn bubble-full-state
  "Retrieves the full bubble state, i e the state of this and all parent bubbles, merged together
with lower state definitions having precedence (first level only!)."
  ([bubble]
     (bubble-full-state bubble 20))
  ([bubble max-level]
      (assert bubble)
      (if (and (:type bubble) (> max-level 0))
        (merge
         (bubble-full-state (db-retrieve-bubble (assoc bubble :uuid (state-parent bubble))) (dec max-level))
         (:state bubble))
        (:state bubble))))


(defn bubble-get-in
  "Almost like get-in, but for bubbles. Searches upwards the type hierarchy until a value is found."
  ([bubble ks]
     (bubble-get-in bubble ks nil))
  ([bubble ks not-found]
     (assert bubble)
     (loop [b bubble cnt 1]
       (let [c (get-in b ks :not-found)]
         (if (not= :not-found c)
           c
           (if (and (:type b) (< cnt 20))  ;a little fuse to avoid never ending loops
             (recur
              (db-retrieve-bubble (assoc b :uuid (:type b)))
              (inc cnt))
             not-found                     ;return not-found value
             ))))))

(defn state-get-in
  ([bubble ks]
     (state-get-in bubble ks nil))
  ([bubble ks not-found]
     (bubble-get-in bubble (conj (seq ks) :state) not-found)))

;; FIXME: could this use bubble-get-in instead?
(defn get-code
  "Searches for the specified message code stating with the
  given bubble. The return value is a partial expression which expects
  local bindings of some type, i e the result must be assembled,
  depending on the code-type."
  [bubble message code-type]
  ;(log/debug (str message " -> " bubble))
  (assert bubble)
  (loop [b bubble cnt 1]
    ;(assert (< cnt 50))
    (let [code-map (:code b)
          c (get-in code-map [code-type message])]
      (if (nil? c)
        ; (nil? c):
        (if (and (:type b) (< cnt 20))  ; little fuse to avoid never ending loops
          (recur
            (type-bubble b) ;(db-retrieve-bubble (assoc b :uuid (b :type)))
            (inc cnt))
          ; else: failed to find a message, provide a default result
            ; was: :message-not-found
          (throw+ {:bust :message-not-found :message message})
          )
          c ; <<--- THIS little bugger is what we wanted!
        ))))

(defn make-function
  [code]
  ;;(log/debug code)
  (if @use-sandboxing
    (sb `(do
           ;;(use ['bubbles.tools :as 'x])
           (use 'bubbles.tools)         ;do we want this?? FIXME?
           ~code))
    (eval code)))

;;(use '[bubbles.tools :as yy])
;;(yy/gen-uuid)
(comment
  (def code '(gen-uuid))
  (sb code) ;-> fails
  (sb '(do (bubbles.tools/gen-uuid)))
  (sb '(do (gen-uuid))) ;->fails
  (use 'bubbles.tools)
  (sb '(do (use 'bubbles.tools) (gen-uuid)))
  (sb 'new-bubble))



(defn apply-filters
  "Returns all filter codes. It starts with the given
  bubble and goes through all bubble type ancestors."
  [bubble]
  ;(log/debug (str "In apply-filters: " bubble))
  (try
    (loop [b bubble code bubble cnt 1 checked-filters #{}]
      (let [filters (get-in code [:code :filter])
            filtered-bubble
              (loop [b2 b f filters]
                (if (empty? f)
                  b2
                  (let [[fname fcode] (first f)
                        nb (or (if (not (contains? checked-filters fname))
                             ((make-function fcode) b2))
                               b2)
                        ]
                    (recur nb (rest f)))))]
          (if (and (:type code) (< cnt 20))  ; little fuse to avoid never ending loops
            (recur filtered-bubble (type-bubble b) (inc cnt) (into checked-filters (keys filters)))
            filtered-bubble)))
    #_(catch Throwable e (throw+ {:bust :filter-vetoed}))))


(defn message-function
  [bubble message code-type]
  (let [c (get-code bubble message code-type)]
    (assert (not (nil? c)))        ; c might be ":message-not-found"
    ;(with-ns bubbles.sandbox) ; FIXME somehow...
    (make-function c)
    #_(comment condp = code-type
      :message (eval (concat '(fn [^Map b]) (list c)))
      :extract (eval (concat '(fn [^Map b]) (list c)))
      :filter  (eval (concat '(fn [^Map b]) (list c)))
      )))

(defmacro logexception
  [& body]
  `(try ~@body
        (catch Throwable e#
          (log/spy
           (with-out-str  (.printStackTrace e#))) (throw e#))))

(defn updater-message
  "Internal: takes a loaded bubble and applies the update message on it.
  The return value of updater-message is the new bubble.
  If the value new-bubble is nil then the bubble is not meant to be updated.

  Message names ending with '?' will never modify the state but only return a return-value.
  Message NOT ending with '?' must generate a vector to return anything other than nil."
  [bubble message params]
  ;(log/debug bubble) ; DEBUG!
  (assert bubble)
  (assert (not (:deleted bubble)))
  (let [state         (or (bubble :state) {}) ; (parsed-state bubble)
        msg-function  (message-function bubble message :message)
        result        (logexception (msg-function (assoc bubble :params params)))
        new-state     (:state result)
        ]
      (if (= new-state :message-not-found)
        (throw+ {:bust :message-not-found :message message}))
      (if new-state
        (assoc bubble :state new-state)
        bubble)))


(defn extract
  "Internal: takes a loaded bubble and runs the extract message on it.
  The return value of extract is ALWAYS a map with the keys
    :body (defaults to nil)
    :content-type (defaults to 'text/plain')"
  [bubble message params]
  (assert bubble)
  (assert (not (:deleted bubble)))
  (let [state         (or (bubble :state) {}) ; (parsed-state bubble)
        msg-function  (message-function bubble message :extract)
        direct-result (msg-function (assoc bubble :params params))]
    (when (= direct-result :message-not-found)
      (throw+ {:bust :message-not-found :message message}))
    (let [result (cond
                  (and (map? direct-result)
                       (contains? direct-result :body))
                  direct-result
                  (string? direct-result)  {:body direct-result}
                  :else  {:body direct-result :content-type "application/json"})]
      (assoc result
        :content-type (or (:content-type result) "text/plain")))))


(defn increment-version
  "Returns the next version number for the given str (as a str)."
  [ver]
  (if (number? ver)
    (inc ver)
    (inc (Integer. ver))))

(defn- is-bubble-done? [bubble]
  (when-not (:code bubble)  ; code-bubbles are never "done"
    (cond
      (:deleted bubble) true
      ; FIXME: done?-extact should be a filter instead!
      (:code bubble) false ; code-bubble is never done...
      :else (try+
             (:body (extract bubble "done?" nil))
             (catch [:bust :message-not-found] _ false)))))


(defn- empty-or-exists [bubble uuid]
  (or (nil? uuid)
      (db-retrieve-bubble-silent (assoc bubble :uuid uuid))))


(defn initialize-bubble
  [bubble]
  (apply-filters
    (try+
      (or (updater-message bubble "init" nil) bubble)
      (catch [:bust :message-not-found] e bubble))))

(defn db-create-bubble [bubble]
  (assert (map? bubble))
  (assert (empty? (disj (set (keys bubble)) :uuid :domain :name :type :state :code :parent)))
  (assert (not (= \. (first (:uuid bubble)))))
  (when-not (and (bubble :uuid) ; must have an uuid
               (empty-or-exists bubble (:type bubble)) ; must have a valid type
               (empty-or-exists bubble (:parent bubble))) ; must have a valid parent
    (throw+ {:bust :bad-request})); 400
  ;; Now check that the bubble does not already exist
  (try+
    (db-retrieve-bubble bubble) ; no need to save the result as it would be wrong if we find something
    (throw+ {:bust :bubble-already-exist})
    (catch [:bust :bubble-does-not-exist] _ "ok!"))
  (let [;bubble (assoc bubble :uuid (or (bubble :uuid) (gen-uuid)))
        bubble (if (:code bubble)
                 bubble ; exception: don't init code bubble
                 (initialize-bubble bubble)) ; initialize bubble
        done? (is-bubble-done? bubble) ; check if already done...
        ]
    (bubbles.store.protocol/create @db-store bubble done?)
    (db-retrieve-bubble bubble)
    ))


(defn db-update-bubble [bubble] ; {{{
  (assert (map? bubble))
  (assert (bubble :uuid))
  (assert (:version bubble))
  (let [old (db-retrieve-bubble bubble)]
    (if (:deleted old)
      (throw+ {:bust :bubble-frozen}) ; 405 - Method not allowed
      (let [new-bubble (assoc old
                         :state    (or (bubble :state) (old :state))  ; will not erase state
                         :code     (bubble :code)
                         :parent   (bubble :parent)
                         :type     (bubble :type)
                         :version  (increment-version (bubble :version))
                      )
            new-bubble (if (:code bubble)
                         new-bubble ; exception: don't filter code bubble
                         (apply-filters (assoc new-bubble :old old)))
            done? (is-bubble-done? new-bubble) ; check if done
          ]
      (assert (= (old :domain) (new-bubble :domain)))
      (assert (= (old :version) (bubble :version)))
      (assert (or (map? (new-bubble :state)) (nil? (old :state))))  ; can't update to nil
      (bubbles.store.protocol/update @db-store bubble new-bubble done?)
      (db-retrieve-bubble bubble) ; FIXME: move to Store?
      )))) ; }}}

(defn db-clobber-bubble
  "Will create or overwrite bubble. This is hard stuff, be careful."
  [bubble]
  (assert (and (:domain bubble) (:uuid bubble)))
  (let [current (db-retrieve-bubble-silent bubble)]
    (if current
      (db-update-bubble (assoc bubble :version (:version current)))
      (db-create-bubble bubble))))

(defn mass-load-bubbles
  [b-list domain]
  (dorun
   (for [b (flatten b-list)]
     (do
       (assert (= (:domain b) domain))  ;no cheating!
       (db-clobber-bubble b)))))

(defn db-make-ref
  "Creates or updates a bubble reference"
  [bubble refuuid type order]
  (when-not (and refuuid type order (bubble :uuid)
              (empty-or-exists bubble (:uuid bubble))
              (empty-or-exists bubble refuuid))
    (throw+ {:bust :bad-request})); 400
  (bubbles.store.protocol/add-ref @db-store bubble refuuid type order))

(defn db-remove-ref
  "Removes a bubble reference"
  [bubble refuuid type]
  (bubbles.store.protocol/remove-ref @db-store bubble refuuid type))

(defn db-get-refs
  "Fetches bubble references"
  ([bubble type params]
      (bubbles.store.protocol/get-refs @db-store bubble type params))
  ([bubble type]
      (bubbles.store.protocol/get-refs @db-store bubble type nil)))

(defn db-message
  "Sends message to bubble, fetches from and stores bubble to db."
  [bubble message params]
  (let [b (db-retrieve-bubble-silent bubble)]
    (cond
     (not b)       (throw+ {:bust :bubble-does-not-exist})
     (:deleted b)  (throw+ {:bust :bubble-frozen})
      :else         (when-let [new-bubble (updater-message b message params)]
                      (db-update-bubble new-bubble)))))

(defn db-extract
  "Runs the extraction message for the given bubble and returns the value.
  This will not modify the BUST."
  [bubble message params]
  (let [b (db-retrieve-bubble-silent bubble)]
    (cond
     (not b) (throw+ {:bust :bubble-does-not-exist})
      ;Hmmm...(:deleted b) (throw+ {:bust :bubble-frozen})
      :else (let [result (extract b message params) ]
              ;(if new-bubble  ; has the bubble been updated?
              ;  (db-update-bubble new-bubble))
              (assert (map? result))
              result))))

;; login is either basic-auth-based or external checked (persona)
;; tored in :session {:id {:login-time 1330801167437, :name "browserid@jramb.com",
;; :status "okay", :email "browserid@jramb.com", :audience "http://localhost:8090",
;; :issuer "browserid.org"}
;; or for basic authentication: {:name username :password password :status "unchecked"}
;; the bubble-login is the user-specific bubble <domain>-user-<name>
;; There are three stages
;; not logged in (:session :id is empty)
;; registered (:session :id :name is not empty)
;; validated (:session :id :status = "okay") -- not possible by basic authentication

(defn hash-password [password salt]
  (assert (> (count salt) 10))          ;would like to have >64 bit of salt
  (assert (> (count password) 6))       ;come on, how low can we go?
  (let [md (java.security.MessageDigest/getInstance "SHA-512")
        encoder (sun.misc.BASE64Encoder.)]
    (.update md (.getBytes salt "UTF-8")) ;assume text salt
    (.encode encoder
             (loop [mangle (.getBytes password "UTF-8")
                    passes 1e5]         ; paranoid, but are we paranoid enough?
               (if (= 0 passes)
                 mangle
                 (recur (.digest md mangle) (dec passes)))))))

;; (time (hash-password "navigate" "pw:navigate"))
;;-> "yg32nB2GRXYeNMSdK2MhUGzvqxQYh082pqcrdZljWkbzUjAXCPF9mUnRPlaLeynpzIMHuBELRIje\n5AYE3xQkhg=="


(defn get-user-bubble [context username]
  (let [uuid (format "%s-user-%s" (.toLowerCase (:domain context)) username)]
    (db-retrieve-bubble-silent
     (assoc context :uuid uuid))))

(defn check-password [given login-bubble]
  (let [{:keys [hash salt]} (get-in login-bubble [:state :password])]
    (= hash (hash-password given salt))))

(defn validate-bubble-login
  "Validates the login against the bubble and returns the login bubble if the authentication was ok.
  Returns nil if the login bubble could not be fetched."
  [context]
  (let [session-id (get-in *request* [:session :id])
        domain  (:domain context)
        checked (and (= (:status session-id) "okay")
                     (= (or (:domain session-id) domain) domain))
        username (:name session-id)]
    (when-let [username (:name session-id)]
      (let [login-bubble (get-user-bubble context username)]
        (when login-bubble
          (if (or checked
                  (check-password (:password session-id) login-bubble))
            login-bubble
            (do
              (log/info (format "Authentication failed for user %s" username))
              (throw+ {:bust :need-authorization :realm domain}))))))))




;;;; JSON-RPC {{{

;; Helper that create jsonrpc answers:
(defn jsonrpc-result [request result]
  (jsonrpc/wrap-result (get request :id) result))
(defn jsonrpc-error [request error-type message data]
  (jsonrpc/wrap-error (get request :id) (jsonrpc/JSONRPC-ERRORS error-type) message data))


; Internal implementation
(defmulti domain-rpc :method)
(defmethod domain-rpc "foo" [j]
  (jsonrpc-result j "bar"))
(defmethod domain-rpc "echo" [j]
  (jsonrpc-result j (str j)))
(defmethod domain-rpc "error" [j]
  (throw+ "Making an error!"))
(defmethod domain-rpc :default [j]
  (jsonrpc-error j :method-not-found "Procedure not found." nil))

(defn call-jsonrpc-domain
  [domain jsonrpc]
  "Executes the jsonrpc request in the context of the domain.
  The result is either a jsonrpc-result or jsonrpc-error."
  (try
      (domain-rpc (assoc jsonrpc :domain domain))
    (catch Throwable e (jsonrpc-error jsonrpc :internal-error "exception" (str e)))))


(defn bubble-rpc
  [domain uuid jsonrpc]
  (let [method (get jsonrpc :method)]
    (condp = method
      ;"sys.size" (jsonrpc-result jsonrpc 123)
      (try
        (try+
          (let [bubble {:domain domain :uuid uuid}
                params (get jsonrpc :params)]
            (if (= (last method) \?)
              (let [method (apply str (drop-last method))
                    {:keys [body]} (db-extract bubble method params)]
                (jsonrpc-result jsonrpc body))
              (do (db-message bubble method params)
                (jsonrpc-result jsonrpc nil)))) ; messages should not return anything
          (catch [:bust :bad-request] _
              (jsonrpc-error jsonrpc :invalid-request "invalid request" nil))
          (catch [:bust :bubble-does-not-exist] _
              (jsonrpc-error jsonrpc :invalid-request "bubble does not exist" nil))
          (catch [:bust :message-not-found] e
            (jsonrpc-error jsonrpc :method-not-found (str "message not found: " (:message e)) nil))
          (catch [:bust :need-authorization] e
            (jsonrpc-error jsonrpc :invalid-request (format "Authentication required: Basic realm=\"%s\"" (:realm e)) nil)
              #_{:headers {"WWW-Authenticate" (format "Basic realm=\"%s\"" (:realm e))} :status 401})
          (catch [:bust :bubble-frozen] _
              (jsonrpc-error jsonrpc :invalid-request "bubble frozen")))
        (catch Throwable e (jsonrpc-error jsonrpc :internal-error "exception" (str e)))))))

(defn handle-jsonrpc-domain
  [domain jsonrpc]
  {:body (call-jsonrpc-domain domain jsonrpc)
   :content-type "application/json"
   })

(defn handle-jsonrpc-uuid
  [domain uuid jsonrpc]
  {:body (bubble-rpc domain uuid jsonrpc)
   :content-type "application/json"
    })

;;;; JSON-RPC }}}

; bubble code might want this, putting it here, but creates circular references..
;;(require 'bubbles.tools)



