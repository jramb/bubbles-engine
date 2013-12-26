{
 :store "bubbles.store.postgresql.Standard" ; instance of the store interface bubbles.store.protocol.Store
 :db {:classname "org.postgresql.Driver"
      :connection-uri "jdbc:postgresql://localhost:5432/bubbles?user=jorg&password=bubbles"
      :subprotocol "postgresql"
      :subname "//localhost:5432/bubbles"
      ;;:user "jorg"
      ;;:password "bubbles"
      :pooled true ;false is slower!
      :trace true ; default false
      ;;:serialized false
      }
 :pretty-print-response true ;false ; seems to make problems, sometimes...
 :use-sandboxing true       ;default is true! Remember ~/.java.policy
 :jetty {:port 8090
         :host "0.0.0.0"
         ;;:host "127.0.0.1" ; local access only
         ;; :configurator - a function called with the Jetty Server instance
         ;; :port         - the port to listen on (defaults to 80)
         ;; :host         - the hostname to listen on
         ;; :join?        - blocks the thread until server ends (defaults to true)
         ;; :ssl?         - allow connections over HTTPS
         ;; :ssl-port     - the SSL port to listen on (defaults to 443, implies :ssl?)
         ;; :keystore     - the keystore to use for SSL connections
         ;; :key-password - the password to the keystore
         ;; :truststore   - a truststore to use for SSL connections
         ;; :trust-password - the password to the truststore
         ;; :max-threads  - future feature
         }
 :mail {:mailer {:host "smtp.gmail.com"
                 :user "example@gmail.com"
                 :pass "myS1kritPa55wor1"
                 :ssl :yes}
        :defaults {:from "The Bubble Engine <example@gmail.com>"
                   :bcc "jorg@example.com"
                   ;;:to
                   ;;:subject
                   ;;:body
                   }}
 :domains {
           "TEST"
           {:admin ["jorg@jramb.com"]}
           "DEMO_2"
           {:admin ["jorg@jramb.com"]}
           "ICHNAEA"
           {:admin ["jorg@jramb.com"]}
           } 
 }
