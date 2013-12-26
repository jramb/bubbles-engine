(ns bubbles.bucl
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "BUCL is the BUbble CLient."
    :url "https://github.com/jramb/bubbles-engine" }
  (:gen-class)
  (:require [bubbles.jsonrpc :as jsonrpc])
  ;;(:require [clojure.data.json :as json])
  (:require [clojure.contrib.duck-streams :as duck])
  (:require [clojure.pprint :as pp])
  (:require [clojure.contrib.io :as io])
  ;(:require [clojure.java.io :as io]) ;; future?
  (:require [clojure.string :as str])
  ;;(:require [http.async.client :as c])
  (:require [clj-http.client :as ch])
  (:require [clojure.tools.logging :as log])
  (:use [clojure.test]))

;(def *buse* "http://172.16.10.104:8090")
(def *buse* (or (System/getenv "BUBBLE_URL") "http://localhost:8090"))
(def *domain* (or (System/getenv "BUBBLE_DOMAIN") "TEST"))

(def VALID-CHARS
  (map char (concat (range 48 58) ; 0-9
    (range 65 91) ; A-Z
    (range 97 123)))) ; a-z

(defn random-str [length]
  (apply str (take length (repeatedly #(rand-nth VALID-CHARS)))))

(defn gen-uuid-OLD
  "Returns a new UUID without the dashes. Take good care of it, it is unique!"
  []
  (apply str (re-seq #"[^-]" (str (java.util.UUID/randomUUID)))))

; Note: slower than above: (time (dorun (map (fn [_] (gen-uuid)) (range 50000))))
; 4.5 s
(defn gen-uuid
  "Returns a string with at least as many randomness as a UUID."
  []
  (random-str 22))

(defn ping-url []
  (str *buse* "/ping"))

(defn buse-domain-url []
  (str *buse* "/d/" *domain*))

(defn buse-url [& string]
  (str (buse-domain-url) "/b/" (apply str string)))

(defn buse-extract-url
  ([uuid method] (str (buse-domain-url) "/" uuid "/x/" method))
  ([method]      (str (buse-domain-url)          "/x/" method)))

(defn buse-rpc-url
  ([uuid] (str (buse-domain-url) "/" uuid "/rpc"))
  ([]     (str (buse-domain-url)          "/rpc")))

(defn mime-type [req]
  ;; note: async.http generated headers with keywords, ch uses (lowercase) strings
  (first (str/split (str (get-in req [:headers "content-type"])) #";")))

(def SEND-ACCEPT-JSON-HEADER
  {"Content-Type" "application/json; charset=utf-8"
   "Accept"       "application/json" })

(def SEND-ACCEPT-CLOJURE-HEADER
  {"Content-Type" "application/clojure; charset=utf-8"
   "Accept"       "application/clojure" })

(defn parse-proxy [proxy]
  (if-let [ [_ _ host port] (re-matches #"(http://)?(.*):(\d+)" proxy) ]
    {:host host :port (Integer. port)}))

(defn get-proxy-from-env []
  (if-let [env (System/getenv "http_proxy")]
    (parse-proxy env)))

(def current-proxy (get-proxy-from-env))

(defn- call-rest
  ([mode url body header]
     ;;(log/debug (str "\nmode=" mode "\nurl=" url "\nhdr=" (str header) "\nbody=" body)) ; DEBUG
     ;;(println (str "\nmode=" mode "\nurl=" url "\nhdr=" (str header) "\nbody=" body)) ; DEBUG
     ;;(println mode url)
     ;;(println (str "header=" header " type=" (type header))) ; DEBUG
     (try
       (mode url
             {
              :headers (merge SEND-ACCEPT-CLOJURE-HEADER header)
              :timeout 15000 ; ms
              :body (if body (str body))
              ;;:auth {:type :basic :user "anton" :password "navigate" :preemptive true} ;; DEBUG!! :preemptive true?
              :proxy current-proxy  ;(get-proxy-from-env)
              ;;:body xx :cookies
              }                   ; this map is used by clj-http, not the old async-http
             )
       (catch Exception e
         (get-in (bean e) [:data :object])))
     #_(let [r ]
       #_(comment http.async.client code
                       (c/await r)
                       {:body (c/string r) :status (c/status r) :headers (c/headers r)})
       r                                ; clj-http is easier... :-)
      ))
  ([mode url body] ; no header
   (call-rest mode url body nil)))

(defn call-rest-get
  ([url header]     (call-rest ch/get #_c/GET url nil header))
  ([url]            (call-rest-get url nil)))

(defn call-rest-post
  ([url body header]  (call-rest ch/post #_c/POST url body header))
  ([url body]         (call-rest-post url body nil)))

(defn call-rest-put
  ([url body header]   (call-rest ch/put #_c/PUT url body header))
  ([url body]          (call-rest-put url body nil)))

(defn call-rpc
  "Calls the URL with a JSON-RPC. Awaits the result.
  Returns a map with the keys :jsonrpc, :result (or :error) and :id."
  ([url method params header]
    ;(log/debug (str "url=" url " method=" method " params=" params))
    (let [jsonrpc (jsonrpc/wrap-request method params)
          req (call-rest ch/post #_c/POST url jsonrpc (merge SEND-ACCEPT-JSON-HEADER header))
          body (:body req)
          ]
      (if (= (:status req) 200)
        (jsonrpc/unwrap-result body)
        (jsonrpc/format-error (:id params) (jsonrpc/JSONRPC-ERRORS :server-error) "Server error." (str body)))))
   ([url method params]
    (call-rpc url method params nil))
   ([url method]
    (call-rpc url method nil nil)))


#_(defn read-bubble [bubble-string]
  (try
    (read-string bubble-string)
    #_(catch Exception e (is false (str "Exception: " e ">>>" bubble-string "<<<")))))

(defn fetch-raw-bubble [uuid]
  (call-rest-get (buse-url uuid)))

(defn fetch-bubble [uuid]
  (let [raw (fetch-raw-bubble uuid)]
    (when-let [body (:body raw)]
      (read-string body))))


#_(defn cleanup
  [bubble]
  (dissoc bubble :creation_date :update_date :deleted))

(defn create-bubble
  "Create bubble or throws an exception."
  [bubble]
  (let [req (call-rest-post (buse-url) bubble)]
    (if (= (:status req) 201)
      (if-let [body (:body req)]
        (read-string body))
      (throw (Exception. "Could not create bubble!")))))

(defn update-bubble
  "Update an existing bubble or throws an exception."
  [bubble]
  (let [req (call-rest-put (buse-url (:uuid bubble)) (assoc bubble :domain *domain*))]
    (if (= (:status req) 200)
      (read-string (:body req))
      (throw (Exception. "Could not update bubble!")))))

(defn- local-update-bubble
  [bubble adjust-bubble]
  (merge (dissoc adjust-bubble :version) bubble))

(defn upcreate
  "Updates bubble and if that fails create it."
  [bubble]
  (let [b (dissoc bubble :creation_date :update_date :deleted :version)
        b (assoc bubble :domain *domain*)
        uuid (:uuid b)
        old-b (fetch-bubble uuid)]
    (if old-b
      (let [new-b (merge old-b b)
            req (call-rest-put (buse-url uuid) new-b)]
        (if (= (:status req) 200)
          (read-string (:body req))
          (throw (Exception. "Could not update bubble!"))))
      (let [req (call-rest-post (buse-url) b)]
        (if (= (:status req) 201)
          (read-string (:body req))
          (throw (Exception. "Could not create bubble!"))))
      )
    #_(format "Updated bubble: %s"  uuid)))


(defn send-message
  "Sends a message to the bubble with optional parameters."
  ([bubble-id message]
    (let [req (call-rest-post (buse-url bubble-id "/msg/" message) nil)]
      (when (not= (:status req) 200)
        (throw (Exception. (str "Could not message bubble\n" (:body req)))))))
  ([bubble-id message parameter]
    (let [req (call-rest-post (buse-url bubble-id "/msg/" message) parameter)]
      (when (not= (:status req) 200)
        (throw (Exception. (str "Could not message bubble\n" (:body req))))))))

(defn ping []
  (let [p (call-rest-get (ping-url))
        info (read-string (:body p))]
    info))

(defn fetch-all-bubbles
  "Returns a vector of the first 200 bubbles."
  []
  (let [re (call-rest-get (buse-url))]
    (:result (read-string (:body re)))))

;(defn upload-bubble
;  [bubble]
;  (let [uuid (:uuid bubble)
;        existing-bubble (fetch-bubble (:uuid bubble))]
;    (if existing-bubble
;      ()
;      (create-bubble bubble))


(defn print-source-code
  [uuid]
  (let [bubble (fetch-bubble uuid)]
    (println (str "; Bubble uuid" (:uuid bubble)))
    (pp/pprint (dissoc bubble :state :deleted :update_date :creation_date :version))
    (println)
    ))


(defn output-code [tree]
  (for [b tree]
    (if (vector? b)
      (output-code b)
      (print-source-code b))))

(defn spit-code
  [filename]
  (let [req (call-rest-get (buse-url "code"))
        r   (if-let [body (:body req)] (read-string body))]
    (duck/with-out-writer filename
      (dorun (output-code r)))
    #_(c/close)))

(defn update-or-create-bubble
  [bubble]
  (println "Uploading bubble" (:uuid bubble))
  (upcreate bubble)
  true)

(defn slurp-code
  [filename]
  (println "Slurping" filename)
  (with-open [r (java.io.PushbackReader. (io/reader filename))]
    (doall
      (map update-or-create-bubble (take-while #(not= ::eof %)
        (repeatedly #(read r false ::eof)))))
    #_(try ; previous version, working, but error in the end
      (loop [b (read r)]
        (if (not= ::eof))
        (println "Uploading bubble" (:uuid b))
        (update-or-create-bubble b)
        (recur (read r)))
      (catch Exception e (println (str e))))
    #_(c/close)))

