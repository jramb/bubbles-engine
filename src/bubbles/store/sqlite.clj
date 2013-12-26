(ns bubbles.store.sqlite
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "SQLlite DB backend for the Bubble Store"
    :url "https://github.com/jramb/bubbles-engine"}
  (:gen-class)
  (:require [clojure.java.jdbc :as sql])
  (:require [clojure.tools.logging :as log])
  (:require [bubbles.store.protocol :as proto]))


(deftype Standard []
  proto/Store
  (startup [this]
    (sql/do-commands
     ;; Create xxb_bubbles
     "create table if not exists xxb_bubbles
( uuid varchar2(64) not null
, domain        varchar2(32) not null
, name          varchar2(128) null
, type          varchar2(64) null -- foreign key to xxb_bubbles
, version       integer --varchar2(64) not null 
, state         clob NULL
, code          clob NULL
, parent        varchar2(64)  -- foreign key to xxb_bubbles
, deleted_flag  varchar2(1) default 'N' not null -- Normal, Deleted
, creation_date datetime not null
, update_date   datetime not null
, --constraint xxb_bubbles_pk
  primary key ( uuid ) --enable
);"
     ;; Create xxb_bubbles_history
     "create table if not exists xxb_bubbles_history
as select * from xxb_bubbles where 1=2;"
     ;; Create refs table
     "create table if not exists xxb_bubble_refs
( domain         varchar2(32) not null
, from_uuid      varchar2(64) not null
, to_uuid        varchar2(64) not null
, type           varchar2(32) null
, sort_order     varchar2(64) -- TODO: check this!
, creation_date  datetime not null
, update_date    datetime not null);"
     ;; "drop view if exists xxb_ping;"
     "create view if not exists xxb_ping
as select 'sqlite' as name, current_timestamp as now,
(select count(*) from xxb_bubbles) as num_bubbles;"))
  (ping [this]
        (sql/with-query-results rs
           ["select name as name, now, num_bubbles from xxb_ping"]
           (let [r (first rs)]
             (assoc r :name
                    (str (:name r) "-" (if (org.sqlite.SQLiteJDBCLoader/isNativeMode)
                                         "native" "java"))))))
  ;;;;;;;;;;;;;;;
  (fetch [this domain uuid]
      (sql/with-query-results rs
            ["select * from xxb_bubbles
              where domain = :1 and uuid = :2"
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
          )))
  ;;;;;;;;;;;;;;;
  (add-ref [this bubble refuuid type order]
    (sql/with-query-results rs
        ["select sort_order from xxb_bubble_refs
         where domain=:1 and from_uuid=:2 and to_uuid=:3 and type=:4"
         (:domain bubble) (:uuid bubble) refuuid type]
        (if (first rs) ; check if ref exists
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
 [this bubble type params]              ; FIXME: params
 (if (some #{type} [:kids :typees])
   (let [sqlparams [(:domain bubble) (:uuid bubble)]
         sql "select uuid from xxb_bubbles where domain=?"
         sql (str sql
                  (cond
                   (= type :kids) " and parent=?"
                   (= type :typees) " and type=?"
                   ))
         sql (str sql " order by uuid")
         qwr (apply vector (cons sql sqlparams))]
     (map :uuid
          (sql/with-query-results rs qwr
            (doall rs))))  
   (let [sqlparams [(:domain bubble) (:uuid bubble) type]
         sql "select to_uuid as uuid from xxb_bubble_refs where domain=? and from_uuid=? and type=?"
         sql (str sql " order by sort_order")
         qwr (apply vector (cons sql sqlparams))]
     (map :uuid
          (sql/with-query-results rs qwr
            (doall rs))))))
  ;;;;;;;;;;;;;;;
  (fetch-by-name [this domain name]
    (sql/with-query-results rs
            ["select * from xxb_bubbles
              where domain = :1 and name = :2"
             domain name]
            (if-let [r (first rs)]
              (proto/db-row-to-bubble r))))
  ;;;;;;;;;;;;;;;
  (fetch-all [this domain]
    (sql/with-query-results rs
      ["select * from xxb_bubbles
        where domain = :1
        limit :2"
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
  (backend [this bubble message params]
    ;; FIXME, how would that look like?
    )
  )

