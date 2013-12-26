(ns bubbles.buload
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "BUbble LOader, Download and Upload bubble code."
    :url "https://github.com/jramb/bubbles-engine"}
 (:gen-class)
 (:require [clojure.tools.cli :as cli])
 (:require [clojure.tools.logging :as log])
 (:require [bubbles.bucl :as cl]))

(defn -main [& args]
  (println "BULOAD - Navigate Consulting, Sweden")
  (let [[options args banner]
        (cli/cli args
                 ["-h" "--help" "Show help" :default false :flag true]
                 ["-g" "--download" "Download (get)" :default false :flag true]
                 ["-u" "--upload"   "Upload" :default false :flag true]
                 ["-b" "--buse"     "Buse URL" :default cl/*buse*]
                 ["-d" "--domain"   "Domain" :default cl/*domain*]
                 ["-f" "--filename" "File name" :flag false])
        filename (:filename options)]
    (when (:help options)
      (println banner)
      (System/exit 0))
    (when-not (and (or (:download options) (:upload options)) filename)
      (println "Missing or invalid parameters, check with -h")
      (System/exit 0))
    (binding [cl/*buse* (or (:buse options) (System/getenv "BUBBLE_URL"))
              cl/*domain* (or (:domain options) (System/getenv "BUBBLE_DOMAIN"))]
      (cond
       (:download options) (do
                             (println "Downloading" cl/*buse* cl/*domain* "to" filename)
                             (cl/spit-code filename))
       (:upload options) (do
                           (println "Uploading" cl/*buse* cl/*domain* "from" filename)
                           (cl/slurp-code filename))
       )))
  (println "End of buload"))
