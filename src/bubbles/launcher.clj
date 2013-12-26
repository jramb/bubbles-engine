(ns bubbles.launcher
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Bubble Launcher"
    :url "https://github.com/jramb/bubbles-engine"}
 (:gen-class)
 (:require [bubbles.buse :as buse])
 (:require [bubbles.buman :as buman])
 (:require [bubbles.buload :as buload])
 ;;(:require [ht tp.async.client :as c])
 )

;;;;;;;;;;;;;;;;;;;;;;;;;
;; 2010-12-17 J Ramb, Navigate Consulting
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  ;(cl/with-command-line args "Bubble launcher - Navigate Consulting, Sweden"
  ;  [;[config-file c "Config file" "config.clj"]
     ;[server? s? "Start server" true]
  ;   remaining]
    (let [[command & rest] args]
      ;(println "cmd" command)
      ;(println "rest" rest)
      (condp = command
        "buse" (apply buse/-main rest)
        "buman" (apply buman/-main rest)
        "buload" (apply buload/-main rest)
        (println
"Bubble launcher - Navigate Consulting, Sweden

Usage: bubbles <cmd> [param1 param2 ...]
<cmd> one of
  buse [config-file]
  buman
  buload -h")))
    ;;removed this to get rid of the async.client (c/close)
    (println "Launcher done.")
  ;;(use 'mycroft.main)
  ;;(run 8082)
  ;(clojure.main/repl)
  );)

(comment
  (-main "buse" "config-sqlite.clj")
  )
