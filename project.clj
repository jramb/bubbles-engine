(defproject buse "1.0.0-SNAPSHOT"
  :description "Bubbles"
  :dependencies [
                 [org.clojure/clojure "1.2.1"]  ; 1.2.1 has problems with cyclic load dependency in bust.clj
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/java.jdbc "0.2.1"]
                 ;;[org.clojure/data.json "0.1.3"] ;replaced with cheshire
                 [org.clojure/data.xml "0.0.4"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/tools.cli "0.2.1"]
                 [slingshot "0.10.3"]
                 ;;[mycroft "0.0.2"]
                 [compojure "1.0.1"]
                 [hiccup "1.0.0"]
                                        ;[clojureql "1.0.0-beta2-SNAPSHOT"]
                 ;;[enlive "1.0.0"]
                                        ;[ring/ring-jetty-adapter "0.2.3"]
                 [ring/ring-jetty-adapter "0.3.5"]
                                        ;slf4 needed to make log4j play in jetty...
                 [org.slf4j/slf4j-api "1.5.6"]
                 [org.slf4j/slf4j-log4j12 "1.5.6"]
                 [log4j "1.2.16" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 ;;[http.async.client "0.2.2"] ; https://github.com/neotyk/http.async.client  for BUMAN+TEST
                 ;;[http.async.client "0.4.0"] ; https://github.com/neotyk/http.async.client  for BUMAN+TEST
                 [clj-http "0.3.0"]
                 [clojail "0.5.1"]
                 ;;[postgresql/postgresql "8.4-702.jdbc4"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 ;;[swank-clojure "1.3.3"]
                 [swank-clojure "1.4.0"]
                 [claws "0.0.1-SNAPSHOT"]
                 [com.draines/postal "1.8.0"] ;for sending mail
                 ;; future[tnrglobal/bishop "1.0.5"]
                 [cheshire "4.0.0"]
                 ;; local stuff
                 [com.mchange/c3p0 "0.9.2"] ; replaces [self/c3p0 "0.9.1.2"]
                 [self/c3p0-oracle-thin-extras "0.9.1.2"]
                 [self/ojdbc "14"]
                 [org.xerial/sqlite-jdbc "3.7.2"] ; not tested yet, from https://github.com/clojure/java.jdbc/
                 ;;[self/sqlite-jdbc "3.7.8-SNAPSHOT"]
                 ]
  :plugins [[lein-swank "1.4.4"]]
  :repositories {"local" ~(str (.toURI (java.io.File. "maven_repository")))}
  :dev-dependencies [;; never used this;;[uk.org.alienscience/leiningen-war "0.0.12"]
                     ;;[jline "0.9.94"]
                     ;; lein-ngserver contains vimclojure/server 2.2.0:
                     ]
  ;;:resources-paths ["resources"]              ;adds files in here to the root of the jar
  :omit-source true ; source obfuscation?
  :jar-exclusions [#"(?:^|/).svn/" #"AwsCredentials.properties"]
  ;;:repositories {"lib-extra" "file://lib-extra/" }
  :aot [bubbles.buse
        bubbles.buman
        bubbles.buload
        bubbles.tools
        bubbles.store.sqlite
        bubbles.store.oracle
        bubbles.store.dynamodb
        bubbles.store.postgresql
        ]
  :jar-name "bubbles.jar"
  :war {:name "bubbles.war"
        ;:webxml "war/example.xml"
        ;:web-content "html"
        }

  :uberjar-name "bubbles-standalone.jar"
  :disable-implicit-clean true
  ;:main bubbles.buman
  ;:main bubbles.buse
  :main bubbles.launcher
  )
