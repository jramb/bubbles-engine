(ns bubbles.buse
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "BUbble SErver, the speaking part."
    :url "https://github.com/jramb/bubbles-engine"}
  (:gen-class)
  (:require [bubbles.bust :as bust])
  (:require [bubbles.setup :as setup])
  (:require [bubbles.jsonrpc :as jsonrpc])
  (:require [bubbles.freemind :as freemind])
  (:require [bubbles.html :as html])
  (:require [compojure.core :as compojure])
  (:require [compojure.route :as route])
  (:require [compojure.handler :as c-h])
  (:require [clout.core :as clout])
  (:require [clj-http.client :as ch])
  (:require [ring.adapter.jetty :as jetty])
  ;;(:use [ring.middleware params keyword-params nested-params multipart-params cookies session flash file file-info])
  (:require [ring.middleware.multipart-params :as ringmp])
  (:require [clojure.stacktrace :as stacktrace])
  (:require [clojure.tools.cli :as cli])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp])
  (:require [cheshire.core :as json])   ;replaces clojure.data.json
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.string :as str])
  (:require [swank.swank :as swank]))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; 2010-09-03 J Ramb, Navigate Consulting
;;;;;;;;;;;;;;;;;;;;;;;;;

(let [i (atom 0)]
    (defn get-call-counter
        []
        @i)
    (defn inc-call-counter
          "Returns an incremeted call counter for each call."
          []
          (swap! i inc)))

(defn time-now []
  (.getTime (java.util.Date.)))
(def dateFormat (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))
(defn time-now-str []
  (.format dateFormat (java.util.Date.)))

(def START-TIME (time-now))

(defn java-mem []
  (let [rt (Runtime/getRuntime)
        maxMem    (.maxMemory rt)
        allocMem  (.totalMemory rt)
        freeMem   (.freeMemory rt)
        b2mb  #(long (/ % 1048576))
        ]
        {:free_mb (b2mb freeMem)
         :alloc_mb (b2mb allocMem)
         :max_mb (b2mb maxMem)
         :total_free_mb  (b2mb (+ freeMem (- maxMem allocMem)))}))

(defn wrap-timer
  "A ring middleware: adds an execution timer."
  [handler]
  (fn [req]
    (let [start (System/nanoTime)       ; (. System (nanoTime))
          resp  (handler req)]
      (if resp
        (let [time-ms (int (/ (double (- (System/nanoTime) start)) 1000000.0))
              resp (assoc-in resp [:headers "X-execution-time-ms"] (str time-ms))
              resp (assoc-in resp [:headers "X-call-counter"] (str (inc-call-counter)))
              ]
          resp)))))

;; http://nakkaya.com/2010/01/29/gzip-output-compression-in-compojure/
;; Not used, not tested
#_(defn wrap-gzip [handler]
   (fn [request]
     (let [response (handler request)
           out (java.io.ByteArrayOutputStream.)
           accept-encoding (.get (:headers request) "accept-encoding")]
       (if (and (not (nil? accept-encoding))
                (re-find #"gzip" accept-encoding))
         (do
           (doto (java.io.BufferedOutputStream.
                  (java.util.zip.GZIPOutputStream. out))
             (.write (.getBytes (:body response)))
             (.close))
           {:status (:status response)
            :headers (assoc (:headers response)
                       "Content-Type" "text/html"
                       "Content-Encoding" "gzip")
            :body (java.io.ByteArrayInputStream. (.toByteArray out))})
         response))))



(defn in?
  "true if seq contains elm"
  [elm seq]
  (some #(= elm %) seq))


(def mime-type-json     "application/json")
(def mime-type-clojure  "application/clojure")

(defn hdr-content-type [req]
  (let [ct (or (:content-type req)
               (get-in req [:headers "content-type"] ""))
        ct (first (str/split ct #";"))]
    (condp = ct
      "application/clojure" mime-type-clojure
      "application/json"    mime-type-json
      nil)))

(defn hdr-accept [headers]
  (let [ct (or (headers "accept") (headers "content-type") "application/json")
        all (str/split ct #"\s*(;[^,]*)?,\s*")
       ]
    (condp in? all
      "application/clojure" mime-type-clojure
      "application/json" mime-type-json
      ;"*/*" mime-type-json
      nil)))


(defn read-body
  "Reads the body either as JSON or as Clojure (default).
Returns a modified req object with a possibly modified :body.
Currenty this will try to parse any body! Probably default should be JSON?"
  [req]
  (assoc req :body
         (when (:body req)
           (when-let [b (slurp (:body req))] ;FIXME: we should be able to read this directly
             (when-not (empty? b)
               (if (= (hdr-content-type req) mime-type-json)
                 (jsonrpc/parse-json b) ;read-json in clojure.data.json
                 (read-string b)))))))

(defn map-to-content-type [ctype m]
  (condp = ctype
    mime-type-json     (let [m (if (:code m) (assoc m :code (str (:code m))) m)]
                         (if (setup/pretty-print-response?)
                           (jsonrpc/generate-json m {:pretty true})  ;pprint-json
                           (jsonrpc/generate-json m)))  ;json-str
    mime-type-clojure  (if (setup/pretty-print-response?)
                         (with-out-str (pp/pprint m))
                         (str m))
    (str m)                             ; default
    ))


#_(defn split-content-type [full]
  (let [sp (re-find #"(.*)(;\s*charset=(.*))" "text/html; charset=utf-8")]))

(defn wrap-json [handler]
  "Ring middleware: automatically decode/encode JSON and Clojure.
  If the request says it sends those, decode :body to Clojure.
  Otherwise read as Clojure.
  If the body of the response is automatically converted to JSON
  as needed."
  ;; (get-in req [:headers "accept"]) eg "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
  (fn [req]
    (let [req-type (:content-type req)  ;:content-type must be set
          resp (handler (read-body req))]
      (if resp
        (let [response-headers (get-in resp [:headers "Content-Type"])
              resp-type (or
                      response-headers
                      (:content-type resp)   ; the handler could override the format?
                      (hdr-accept (:headers req))
                      req-type
                      mime-type-json)
              body (if (or
                        (seq? (:body resp))
                        (vector? (:body resp))
                        (map? (:body resp))) ; FIXME: vector? other?
                     (map-to-content-type resp-type (:body resp))
                     (str (:body resp)))
              resp-type (if (re-find #"(?s);\s*charset=" resp-type)
                          resp-type
                          (str resp-type "; charset=utf-8"))
              headers (assoc (:headers resp) "Content-Type" resp-type)]
          (assoc resp :body body :content-type resp-type :headers headers))))))


(defn user-friendly-error [req e]
  (let [to-user (with-out-str (stacktrace/print-cause-trace e))]
    (log/error (str req " -->\n" to-user))
    to-user))


(defn wrap-access-control
  "A ring middleware to be used for access control. Applied to ALL requests."
  [handler]
  (fn [req]
    (when (setup/check-access req)            ;not inline to allow dynamic changes via swank
      (handler req))))


(defn wrap-bust-request
  "A ring middleware: wraps the request in a bust session."
  [handler]
  (fn [req]
    ;; too early, :params in req (via compojure) is not parsed yet.
    (let [dom (clout/route-matches "/d/:domain/*" req)
          domain-valid (setup/domain-setup (:domain dom))]   ;domain is valid if setup exists
      ;;(log/debug (str "clout sees: " dom))
      (if (or domain-valid (clout/route-matches "/ping" req))
        ;;   (throw+ {:bust :bubble-does-not-exist}))
        (try+
          (binding [bust/*request* (assoc-in req [:params :domain] (:domain dom))]
            ;;(log/debug "Creating bust context") ;DEBUG
            (bust/with-bust (handler req)))
          (catch [:bust :bad-request] _ {:status 400})
          (catch [:bust :bubble-already-exist] _ {:status 400 :body {:error "Bubble already exists!"}})
          (catch [:bust :bubble-does-not-exist] _ {:status 404 :body {:error "Bubble does not exist!"}})
          (catch [:bust :message-not-found] e {:status 404 :body {:error (str "Message not found: " (:message e))}})
          (catch [:bust :bubble-frozen] _ {:status 405 :body {:error "Bubble is frozen!"}})
          (catch [:bust :need-authorization] e {:headers {"WWW-Authenticate" (format "Basic realm=\"%s\"" (:realm e))} :status 401}))
        ))))

(defn wrap-logger
  "Logs the request and the response in full. Enable only in case of emergency."
  [handler]
  (fn [req]
    (log/debug req)
    (when-let [res (handler req)]
      (log/spy
       res)
      )))


(defn wrap-logger-short [handler]
  (fn [req]
    (when-let [res (handler req)]
      ;;(log/debug (str "req< :c-t=" (:content-type req) ", hdrs=" (select-keys (:headers req) ["accept" "content-type"])))
      ;;(log/debug (str "res> :c-t=" (:content-type res) ", hdrs=" (select-keys (:headers res) ["accept" "content-type"])))
      (log/info
       (format "[%s] %s %s \"%s\" %s %s {:ms %s :cnt %s}"
               (time-now-str)
               (:remote-addr req)
               (.toUpperCase (name (:request-method req)))
               (:uri req)
               (:status res)
               (let [b (:body res)]
                 (when (string? b)
                   (count b)  ;(:content-length req) ;req? res?
                   ))
               (get-in res [:headers "X-execution-time-ms"] "-")
               (get-in res [:headers "X-call-counter"] "-")
               ))
      res)))


(defn wrap-exception
  "Catches any unhandled exception."
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable e {:status 500 :content-type "text/plain" :body (user-friendly-error req e)})
      ;(catch Exception e {:status 500 :content-type "text/plain" :body (user-friendly-error req e)})
      ;(catch Error e {:status 500 :content-type "text/plain" :body (user-friendly-error req e)})
      )))

(let [decoder (sun.misc.BASE64Decoder.)]
  (defn decode-basic
    "Decodes basic authentication, returns a vector [username password].
    It this does not return anything, authentication failed."
    [authstr]
    (let [base64 (second (re-find #"Basic (.*)$" authstr))
          decoded (String. (.decodeBuffer decoder base64))
          pair (vec (.split decoded ":"))]
      pair)))


(defn wrap-basic-authentication
  "Wraps the call in a basic authentication. Check of the authorization
  information is done 'later on'."
  [handler]
  (fn [req]
    (let [auth (get (:headers req) "authorization")
          id (get-in req [:session :id])]
      (if (and auth (not id))           ;login is not checked when we are logged in
        (let [[username password] (decode-basic auth)]
          (if username
            ;; (binding [bust/*request* (assoc req :username username :password password)] (handle req))
            ;; having the password here means: this is not validated yet!
            (handler (assoc-in req [:session :id] {:name username :password password :status "unchecked"}))
            {:headers {"WWW-Authenticate" (format "Basic realm=\"Secure Area\"")} :status 401}
            ))
        (handler req)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-bubble
  [{params :params}]
  {:body (if (params :uuid)
    (bust/db-retrieve-bubble {:domain (params :domain) :uuid (params :uuid)})
    (bust/db-retrieve-all-bubbles {:domain (params :domain)})
    )})


(defn rpc-uuid
  [{:keys [body params]}]
  (bust/handle-jsonrpc-uuid (params :domain) (params :uuid) body)
  )

(defn uuid-extract
  [req]
  (let [{:keys [body params]} req]
    (bust/db-extract {:domain (params :domain) :uuid (params :uuid)} (params :msg) params)))

;;
;; FIXME: this should probably not be done here...
(defn domain-extract
  [req]
  (let [{:keys [body params]} req]
    (if (setup/is-admin? req)
      (case (:msg params)
        "home"    (html/domain-home req)     ;TODO FIXME: rem- or improve
        "testing" (html/testing req)
        "inspect" {:status 200
                   :headers {"Content-Type" "text/html"}
                   :body (freemind/html-page "code-mm"
                                             (str "Domain " (:domain params)))}
        "code-mm" {:status 200
                   ;;:content-type "application/x-freemind"
                   :headers {"Content-Disposition" (str "filename=" (:domain params) ".mm")
                             "Content-Type" "application/x-freemind;charset=UTF-8"}
                   :body (freemind/render-domain (:domain params))}
        "code-upload" (freemind/upload-domain
                       (:domain params)
                       (get-in params [:uploadFile :tempfile]))
        #_(html/page "Not implemented" (html/view ["Not implemented" (str params)] :main))
        {:status 200 :content-type "text/plain" :body (str "not implemented\n" params)}
        )
      (html/not-admin req))))

(defn update-bubble
  [{:keys [body params]}]
  {:status 200 #_"Ok" :body (bust/db-update-bubble (assoc body :domain (params :domain) :uuid (params :uuid)))})

(defn create-bubble
  [{:keys [params body]}]
  {:status 201 :body (bust/db-create-bubble (assoc body :domain (params :domain)))})

(defn message
  [{:keys [params body]}]
  (let [b (bust/db-message {:domain (params :domain) :uuid (params :uuid)} (params :msg) body)]
    {:status 204 #_"No Content" :body nil}))


(defn list-code-bubbles
  [{:keys [params body]}]
  {:status 200 :body (bust/get-code-bubbles (params :domain) (params :uuid))})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(defn serve-resource [path]
    (.getResourceAsStream
      (clojure.lang.RT/baseLoader) path))

(defn redirect-to [loc]
  {:status 302
   :headers {"Location" loc}})

;; (bust/with-bust (do-ping nil))
(defn do-ping [req]
  (let [ping (bust/pingdb)
        session (:session req)
        now (time-now)]
   (assoc ping :body
          (assoc (:body ping)
            :calls (get-call-counter)
            ;;:version (:version (meta *ns*))
            :start_time START-TIME
            :buse_time now
            :memory (java-mem)
            :id (:id session)
            :user (:username req)
            :pass (:password req)))))

(defn upload [req]
  (log/info req)
  ;; sent it forth to domain-extract! :-)
  (assert (get-in req [:params :domain]))
  (domain-extract req))

(defn- verify-persona
  "This basically just proxies the request to persona.org/verify!"
  [assertion audience session]
  (let [response (ch/post
                  "https://verifier.login.persona.org/verify" ;; was: "https://browse rid.org/verify"
                  {:headers {"Content-Type" "application/x-www-form-urlencoded"}
                   :timeout 15000 ; ms
                   :body (str "assertion=" assertion "&audience=" audience)})
        id-info (jsonrpc/parse-json (:body response)) ;was:read-json
        login-time (time-now)
            ;; TODO: fetch detailed info for known users. Right now just copy the email (id) to name.
        id-info (assoc id-info :name (:email id-info))
        response (assoc (dissoc response :headers)  ;need to strip off the headers because they contain bad stuff (such as connection close, content-length...)
                   :session (assoc session
                              :id (assoc (dissoc id-info :expires) :login-time login-time))
                       :body    (jsonrpc/generate-json (assoc id-info :login-time login-time))
                       )]
    (log/info (str "Persona verify - " response))
    (when (and (:expires id-info)
               (> (:expires id-info) login-time))
      response)))


(defn verify-password [username password domain session]
  (when (> (count password) 6)
    (if-let [login-bubble (bust/with-bust (bust/get-user-bubble {:domain domain} username))]
      (when (bust/check-password password login-bubble)
        (let [id {:login-time (time-now)
                  :name (or (:name login-bubble)
                            (:sig login-bubble)
                            username)
                  :status "okay"
                  :email (:email login-bubble)
                  :issuer "bust"
                  :domain domain ; only valid in this domain
                  }]
          {:body (jsonrpc/generate-json id)     ;must convert here, because there is no wrapper
           :headers {"Content-Type" "application/json; charset=utf-8"}
           :status 200
           :session (assoc session :id id)
           })))))
 
(defn verify-login [req]
  (let [{:keys [assertion audience username password domain]} (:params req)
        session (:session req)]
    (or (cond (and assertion (not= assertion "null"))    (verify-persona assertion audience session)
              (and username password domain)             (verify-password username password domain session)
              )
        {;; this is actually a log-out
         :body #_{:status "logout"
                :reason "logout"}  "{\"status\":\"logout\",\"reason\":\"logout\"}"
         :headers {"Content-Type" "application/json; charset=utf-8"}
         :status 200
         :session (dissoc session :id)
         })))



(compojure/defroutes buse-routes
  ;; basically same as (def buse-routes (compojure/routes ...))
  ;; (-> ...) wraps, (compojure/routes ...) selects first (which returns something)
  ;;;;;;;;;;;;;;;;
  ; (GET...), (POST..) etc return Ring handlers.
  ; Difference PUT vs POST: PUT stores the given resource as it is. POST "does something" on the resource.
  ;(wrap-logger (GET "/uuid" _ (bust/gen-uuid)))
  (compojure/GET "/uuid" _ (bust/gen-uuid))
  ;; (->
  ;;  (compojure/GET "/test" _ "Jörg")
  ;;  wrap-logger) ; FIXME remove this /test when done
  ;; START static
  (compojure/GET   "/" _ (redirect-to (setup/web-root)))
  (route/files     "/"  {:root "public"}) ; custom overrrides html!
  (route/resources "/"  {:root "html"})
  (->
   (compojure/routes
    (compojure/GET  "/login" req (html/login req))
    (compojure/POST "/login" req (verify-login req)))
   ;;  cannot wrap-bust-request, since :domain is not parsed
   wrap-exception
   ;;wrap-json
   )
  ;;(compojure/POST "/upload" req (upload req))  ;do we want generic upload??
  ;;(POST "/do" ;{body :body} (str (eval (read-string (slurp body))))) ; Interesting backdoor. DANGER, do not use
  ;;;;;;;;;;;;;;;; Good stuff ahead
  (-> 
   (compojure/routes
    (compojure/GET "/ping" req (do-ping req))
    ;; JSON-RPC:
    ;;--> {"jsonrpc": "2.0", "method": "subtract", "params": [42, 23], "id": 1}
    ;;<-- {"jsonrpc": "2.0", "result": 19, "id": 1}
    (compojure/POST "/d/:domain/rpc" {:keys [body params]} (bust/handle-jsonrpc-domain (params "domain") body))
    (compojure/POST "/d/:domain/:uuid/rpc" req (rpc-uuid req))
    ;; Upload (needs multipart-stuff, provided by site below
    (compojure/POST "/d/:domain/upload" req (upload req))
    (compojure/POST "/d/:domain/:uuid/upload" req (upload req))
    ;; Extracting/reporting
    (compojure/GET "/d/:domain/x/:msg" req (domain-extract req))
    (compojure/GET "/d/:domain/:uuid/x/:msg" req (uuid-extract req))
    ;;
    ;; Restricted functions
    ;; ----------------------------------------------------------------------
    ;; GET code bubbles (list of)
    (compojure/GET "/d/:domain/b/:uuid/code" req (list-code-bubbles req))
    (compojure/GET "/d/:domain/b/code"       req (list-code-bubbles req))
    ;; TBD: do we want these?
    (compojure/GET "/d/:domain/b"  req (get-bubble req))
    (compojure/GET "/d/:domain/b/" req (get-bubble req)) ; this seems to be needed. is this a bug??
    ;; end TBD, FIXME
      ;;;;(GET ["/d/:domain/b/:uuid", :uuid #"[\p{Print}]+" ] req (get-bubble req))
    (compojure/GET "/d/:domain/b/:uuid" req (get-bubble req))
    ;; PUT updates an existing bubble
    (compojure/PUT "/d/:domain/b/:uuid" req (update-bubble req))
    ;; POST creates a NEW bubble
    (compojure/POST "/d/:domain/b/" req (create-bubble req))
    ;; POST to /msg/ sends a message (DEPRECATED, use RPC instead) FIXME
    (compojure/POST "/d/:domain/b/:uuid/msg/:msg" req (message req)))
   wrap-bust-request
   wrap-json
   wrap-exception
   wrap-basic-authentication
   ;;wrap-logger ;; remove! mega-debug
   )
  ;;;;;;;;;;;;;;;;;
  ;; Generic Upload (do we want that?)
  ;; (->
  ;;  (compojure/routes
  ;;   (compojure/POST "/upload" req (upload req)))
  ;;  wrap-bust-request
  ;;  wrap-exception
  ;;  wrap-basic-authentication
  ;;  wrap-json
  ;;  )
  ;;;;;;;;;;;;;;;;;
  (route/not-found "Page not found")    ;not really necessary, but leaves less info to caller
  )

;; TODO: understand why this 'site' is necessary/can't be done above
;;;; c-h/api is:
;; wrap-keyword-params
;; wrap-nested-params
;; wrap-params
;;;; site adds
;; wrap-multipart-params
;; wrap-flash
;; wrap-session
;; wrap-cookies
(def final-routes
  (-> buse-routes
      c-h/site
      ;;(wrap-params {:encoding "sdlsds"})
      wrap-timer
      wrap-logger-short
      wrap-access-control))

;(defonce server (jetty/run-jetty #'my-app {:port 8080 :join? false}))

(defn start-server! [config-file]
  (setup/load-config config-file)
  ;; (log/with-logs
  (log/info (setup/jetty-config))
  (defonce server (jetty/run-jetty
                   (var final-routes)
                   (setup/jetty-config)))
  (log/info "Starting BUSE server...")
  (.start server)
  (log/info "BUSE started"))

(defn start-swank []
  (print "Starting Swank: ")
  (swank/start-repl))

(defn -main [& args]
  (require 'bubbles.tools)              ;later we will require these
  (println "BUSE launcher - Navigate Consulting, Sweden")
  (let [[options remaining banner]
        (cli/cli args
                 ["-h" "--help" "Show help" :default false :flag true]
                 ["-b" "--server" "Start server" :default true :flag true]
                 ["-s" "--swank"  "Start swank"  :default true :flag true]
                 )]
    (if (:help options)
      (do
        (println banner))
      (let [ [config-file & remaining] remaining]
        (when (:server options)
          (start-server! config-file))
        (when (:swank options)
          (start-swank))))))

;; (-main "/home/jorg/Navigate/bubbles/server/trunk/config-sqlite.clj")

;; (buse-routes {:remote-addr "0:0:0:0:0:0:0:1", :scheme :http, :request-method :get, :query-string nil, :content-type nil, :uri "/ping", :server-name "localhost", :headers {"cache-control" "max-age=0", "connection" "keep-alive", "dnt" "1", "accept-charset" "ISO-8859-1,utf-8;q=0.7,*;q=0.7", "accept-encoding" "gzip, deflate", "accept-language" "en-us,en;q=0.5", "accept" "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", "user-agent" "Mozilla/5.0 (X11; Linux i686; rv:7.0.1) Gecko/20100101 Firefox/7.0.1", "host" "localhost:8090"}, :content-length nil, :server-port 8090, :character-encoding nil, :body nil})
;; (buse-routes {:request-method :get, :uri "/ping",  :headers { }  :body nil })
;; :body (java.io.StringReader. "(+ 4 5)")
;; (defn pi [_] (buse-routes {:request-method :get, :uri "/ping",  :headers { }  :body nil }) nil)
;; (time (dorun (pmap pi (range 1 1000))))
