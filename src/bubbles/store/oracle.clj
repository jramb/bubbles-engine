(ns bubbles.store.oracle
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Oracle DB backend plugin"
    :url "https://github.com/jramb/bubbles-engine"}
  (:gen-class)
  (:require [clojure.java.jdbc :as sql])
  (:require [bubbles.store.protocol :as proto]))


(deftype Standard []
  proto/Store
  (startup [this] nil)                  ;currently does nothing
  (ping [this]
    (sql/with-query-results rs
      ["select name, now, num_bubbles from xxb_ping"]
      (first rs)))
;;;;;;;;;;;;;;;
  (fetch [this domain uuid]
    (sql/with-query-results rs
      ["select * from xxb_bubbles
              where domain = :1 and uuid = :2 for update" ; lock until end of commit cycle (=request)
       domain uuid]
      (if-let [r (first rs)]
        (proto/db-row-to-bubble r))))
;;;;;;;;;;;;;;;
  (create [this bubble done?]
    (with-open [stmt (.prepareStatement (sql/connection)
                                        "insert into xxb_bubbles
        (uuid, domain, name, type, state, code, parent, creation_date, update_date, version, deleted_flag)
        values (:1, :2, :3, :4, :5, :6, :7, current_timestamp, current_timestamp, :8, :9)")]
      (doto stmt
                                        ;(.registerOutParameter 1 java.sql.Types/VARCHAR)
                                        ;(.registerOutParameter 3 oracle.jdbc.driver.OracleTypes/CURSOR)
        (.setString            1 (bubble :uuid))
        (.setString            2 (bubble :domain))
        (.setString            3 (bubble :name)) ; can be null
        (.setString            4 (bubble :type))
        (.setString            5 (proto/str-nn (bubble :state)))
        (.setString            6 (proto/str-nn (bubble :code)))
        (.setString            7 (bubble :parent))
        (.setInt               8 1)
        (.setString            9 (if done? "Y" "N"))
        (.execute)))
    )
;;;;;;;;;;;;;;;
  (update [this bubble new-bubble done?]
    (if true ;; FIXME: @db-trace
      (with-open [stmt (.prepareStatement (sql/connection)
                                          "insert into xxb_bubbles_history
           select * from xxb_bubbles where domain=:1 and uuid=:2")]
        (doto stmt
          (.setString 1 (bubble :domain))
          (.setString 2 (bubble :uuid))
          (.execute)
          )))
    (with-open [stmt (.prepareStatement (sql/connection)
                                        "update xxb_bubbles
             set state = :1
               , code = :2
               , parent = :3
               , type = :4
               , version = :5
               , deleted_flag = :6
               , update_date = current_timestamp
               where domain = :7 and uuid = :8")]
      (doto stmt
                                        ;(.registerOutParameter 1 java.sql.Types/VARCHAR)
        (.setString            1 (proto/str-nn (new-bubble :state)))
        (.setString            2 (proto/str-nn (new-bubble :code)))
        (.setString            3 (new-bubble :parent))
        (.setString            4 (new-bubble :type))
        (.setInt               5 (new-bubble :version))
        (.setString            6 (if done? "Y" "N"))
        (.setString            7 (bubble :domain))
        (.setString            8 (bubble :uuid))
        (.execute)
        ))
    )
;;;;;;;;;;;;;;;
  (add-ref [this bubble refuuid type order]
    (sql/with-query-results rs
      ["select sort_order from xxb_bubble_refs
         where domain=:1 and from_uuid=:2 and to_uuid=:3 and type=:4"
       (:domain bubble) (:uuid bubble) refuuid type]
      (if (first rs)                    ; check if ref exists
        (with-open [stmt (.prepareStatement (sql/connection)
                                            "update xxb_bubble_refs
                 set sort_order = :1, update_date = current_timestamp
               where domain = :2
                and  from_uuid = :3
                and  to_uuid = :4
                and  type = :5")]
          (doto stmt
            (.setString            1 order)
            (.setString            2 (:domain bubble))
            (.setString            3 (:uuid bubble))
            (.setString            4 refuuid)
            (.setString            5 type)
            (.execute))
          )
        (with-open [stmt (.prepareStatement (sql/connection) ; use do-prepared instead?
                                            "insert into xxb_bubble_refs
               (domain, from_uuid, to_uuid, type, sort_order, creation_date, update_date)
               values (:1, :2, :3, :4, :5, current_timestamp, current_timestamp)")]
          (doto stmt
            (.setString            1 (:domain bubble))
            (.setString            2 (:uuid bubble))
            (.setString            3 refuuid)
            (.setString            4 type)
            (.setString            5 order)
            (.execute))
          ))))
;;;;;;;;;;;;;;;
  (remove-ref [this bubble refuuid type]
    (sql/do-prepared
     "delete from xxb_bubble_refs
         where domain=:1 and from_uuid=:2 and to_uuid=:3 and type=:4"
     [(:domain bubble) (:uuid bubble) refuuid type]))
;;;;;;;;;;;;;;;
  (get-refs
    [this bubble type params]           ; FIXME: params
    (if (some #{type} [:kids :typees])
      (let [sqlparams [(:domain bubble) (:uuid bubble)]
            sql "select uuid from xxb_bubbles where domain=:1"
            sql (str sql
                     (cond
                      (= type :kids) " and parent=:2"
                      (= type :typees) " and type=:3"
                      ))
            sql (str sql " order by uuid")
            qwr (apply vector (cons sql sqlparams))]
        (map :uuid
             (sql/with-query-results rs qwr
               (doall rs))))  
      (let [sqlparams [(:domain bubble) (:uuid bubble) type]
            sql "select to_uuid as uuid from xxb_bubble_refs where domain=:1 and from_uuid=:2 and type=:3"
            sql (str sql " order by sort_order")
            qwr (apply vector (cons sql sqlparams))]
        (map :uuid
             (sql/with-query-results rs qwr
               (doall rs))))))
;;;;;;;;;;;;;;;
  (fetch-by-name [this domain name]
    (sql/with-query-results rs
      ["select * from xxb_bubbles
              where domain = :1 and name = :2 for update" ; lock until end of commit cycle (=request)
       domain name]
      (if-let [r (first rs)]
        (proto/db-row-to-bubble r))))
;;;;;;;;;;;;;;;
  (fetch-all [this domain]
    (sql/with-query-results rs
      ["select * from xxb_bubbles
        where domain = :1
         and rownum <= :2"
       domain 413] ; FIXME: 413 to point out that this needs to be fixed.. some day
      (doall (map proto/short-bubble rs))))
;;;;;;;;;;;;;;;
  (fetch-all-by-type [this domain type which]
    (let [code-cond (case which
                      :code "code is not null"
                      :data "code is null"
                      :all  "1=1"
                      "1=2")]
      (if type
        (sql/with-query-results rs
          [(str "select uuid
         from xxb_bubbles
         where domain = :1
         and type = :2
         and " code-cond)
           domain type]
          (doall (map :uuid rs)))
        (sql/with-query-results rs
          [(str "select uuid
         from xxb_bubbles
         where domain = :1
         and type is null
         and " code-cond)
           domain]
          (doall (map :uuid rs))))))
;;;;;;;;;;;;;;;
  (backend [this bubble message params]
    ;; old way, sent 3 params
    ;; (with-open [stmt (.prepareCall
    ;;                       (sql/connection)
    ;;                       "begin :1 := xxb_backend_pkg.dispatcher(:2,:3,:4,:5,:6); end;")]
    ;;   (doto stmt
    ;;     (.setString    2 (:uuid bubble))
    ;;     (.setString    3 message)
    ;;     (.setString    4 (get params 0)) ; this is less than optimal...
    ;;     (.setString    5 (get params 1))
    ;;     (.setString    6 (get params 2))
    ;;     (.registerOutParameter 1 java.sql.Types/VARCHAR)
    ;;     (.execute))      
    ;;   (.getString stmt 1))
    ;; new 2012-01-05: send params as ONE clob
    (with-open [stmt (.prepareCall
                      (sql/connection)
                      "begin :1 := xxb_backend_pkg.dispatcher(:2,:3,:4,:5); end;")]
      (doto stmt
        (.setString    2 (:domain bubble))
        (.setString    3 (:uuid bubble))
        (.setString    4 message)
        (.setString    5 (proto/str-nn params)) ; now sending all som Clojure. This works up to 32k.
        (.registerOutParameter 1 java.sql.Types/VARCHAR)
        (.execute))
      (.getString stmt 1))))
  
