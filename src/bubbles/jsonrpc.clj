(ns bubbles.jsonrpc
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "JSON-RPC toolbox."
    :url "https://github.com/jramb/bubbles-engine"}
  (:require [cheshire.core :as json])   ;replaces clojure.data.json
  (:require [clojure.string :as str]))


(def JSONRPC-ERRORS
  {:method-not-found -32601
   :parse-error -32700
   :invalid-request -32600
   :invalid-params -32602
   :internal-error -32603
   :server-error -32099 ; to -32000
   })

(let [i (atom 0)]
    (defn current-id
        "Returns the current ID sequence value."
        []
        @i)
    (defn next-id
          "Returns a new ID from the sequence."
          []
          (swap! i inc)))


(defn parse-json [^String s]
  ;; clojure.data.json would have used read-json
  (json/parse-string s true))

(defn generate-json
  ([c]
     ;; clojure.data.json would have used pprint-json or json-str
     (json/generate-string c))
  ([c p]
     (json/generate-string c {:pretty true})))


(defn wrap-request
  "Wraps the method (str) and the params into a JSON-RPC 2.0 json string."
  [method params]
  (assert (string? method))
  (generate-json
    {"jsonrpc" "2.0"
     "method" method
     "params" params
     "id" (next-id)}))

(defn wrap-result
  "Wraps the result into a JSON-RPC 2.0 json string."
  [id result]
  (generate-json
    {"jsonrpc" "2.0"
     "result" result
     "id" id}))

(defn format-error
  ([id code message data]
    {"jsonrpc" "2.0"
     "error" {"code" code "message" message "data" data}
     "id" id})
  ([id code message]
    {"jsonrpc" "2.0"
     "error" {"code" code "message" message}
     "id" id})
   )

(defn wrap-error
  "Wraps the error into a JSON-RPC 2.0 json string."
  [id code message data]
  (generate-json (format-error id code message data)))


#_(def json-to-clojure parse-json)

(defn unwrap-result
  "Takes a JSON string in the format of JSON-RPC result and
  returns a clojure map with the keys :jsonrpc, :result (or :error) and :id."
  [message]
   (let [cl (parse-json message)
         {:keys [jsonrpc result id error]} cl]
     ;(println "Result=" result "id=" id "jsonrpc=" jsonrpc)
     cl
     ))

