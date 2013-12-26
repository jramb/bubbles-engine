(ns bubbles.store.postgresql
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Postgres DB backend for Bubbles Engine"
    :url "https://github.com/jramb/bubbles-engine"}
  (:gen-class)
  (:require [clojure.java.jdbc :as sql])
  (:require [bubbles.store.protocol :as proto]))


#_(sql/with-connection "postgresql://localhost:5432/bubbles"
  (sql/create-table :xxb_bubble_refs
                    [:domain        "varchar(32)" "not null"]
                    [:from_uuid     "varchar(64)" "not null"]
                    [:to_uuid       "varchar(64)" "not null"]
                    [:type          "varchar(32)" "null"]
                    [:sort_order    "varchar(64)"] ; TODO: check this!
                    [:creation_date "timestamp" "not null"]
                    [:update_date   "timestamp" "not null"]
                    ))

(deftype Standard []
  proto/Store
  (startup [this]
    (sql/do-commands
     ;; Create xxb_bubbles
     "create table if not exists xxb_bubbles
( uuid          varchar(64) not null primary key
, domain        varchar(32) not null
, name          varchar(128) null
, type          varchar(64) null -- foreign key to xxb_bubbles
, version       numeric not null
, state         text NULL
, code          text NULL
, parent        varchar(64)  -- foreign key to xxb_bubbles
, deleted_flag  varchar(1) default 'N' not null -- Normal, Deleted
, creation_date timestamp not null
, update_date   timestamp not null
  );"
  ;; table xxb_bubbles_history
"create table if not exists xxb_bubbles_history
(LIKE xxb_bubbles);"                       ;including ...?
  ;; view xxb_ping
"create or replace view xxb_ping as select cast('PostgreSQL' as varchar) as name
--, round(date_part('epoch',current_timestamp)*1000) as now
, current_timestamp as now
, (select count(*) from xxb_bubbles) as num_bubbles;"

"create table if not exists xxb_bubble_refs
( domain         varchar(32) not null
                 , from_uuid      varchar(64) not null
                 , to_uuid        varchar(64) not null
                 , type           varchar(32) null
                 , sort_order     varchar(64) -- TODO: check this!
                 , creation_date  timestamp not null
                 , update_date    timestamp not null) ;"
 ; TODO: indices!
))
  (ping [this]
    (sql/with-query-results rs
      ["select name, now, num_bubbles from xxb_ping"]
      (first rs)))
  ;;;;;;;;;;;;;;;
  (fetch [this domain uuid]
      (sql/with-query-results rs
            ["select * from xxb_bubbles
              where domain = ? and uuid = ? for update" ; lock until end of commit cycle (=request)
             domain uuid]
            (if-let [r (first rs)]
              (proto/db-row-to-bubble r))))
  ;;;;;;;;;;;;;;;
  (create [this bubble done?]
    (with-open [stmt (.prepareStatement (sql/connection)
        "insert into xxb_bubbles
        (uuid, domain, name, type, state, code, parent, creation_date, update_date, version, deleted_flag)
        values (?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, ?, ?)")]
      (doto stmt
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
           select * from xxb_bubbles where domain=? and uuid=?")]
          (doto stmt
            (.setString 1 (bubble :domain))
            (.setString 2 (bubble :uuid))
            (.execute)
            )))
      (with-open [stmt (.prepareStatement (sql/connection)
          "update xxb_bubbles
             set state = ?
               , code = ?
               , parent = ?
               , type = ?
               , version = ?
               , deleted_flag = ?
               , update_date = current_timestamp
               where domain = ? and uuid = ?")]
        (doto stmt
          ;(.registerOutParameter 1 java.sql.Types/VARCHAR)
          (.setString            1 (proto/str-nn (new-bubble :state)))
          (.setString            2 (proto/str-nn (new-bubble :code)))
          (.setString            3 (new-bubble :parent))
          (.setString            4 (new-bubble :type))
          (.setInt               5 (int (new-bubble :version)))
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
         where domain=? and from_uuid=? and to_uuid=? and type=?"
         (:domain bubble) (:uuid bubble) refuuid type]
        (if (first rs) ; check if ref exists
          (with-open [stmt (.prepareStatement (sql/connection)
              "update xxb_bubble_refs
                 set sort_order = ?, update_date = current_timestamp
               where domain = ?
                and  from_uuid = ?
                and  to_uuid = ?
                and  type = ?")]
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
               values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)")]
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
         where domain=? and from_uuid=? and to_uuid=? and type=?"
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
              where domain = ? and name = ? for update"  ; lock until end of commit cycle (=request)
       domain name]
      (if-let [r (first rs)]
        (proto/db-row-to-bubble r))))
  ;;;;;;;;;;;;;;;

  (fetch-all [this domain]
    (sql/with-query-results rs
      ["select * from xxb_bubbles
        where domain = ?
         limit ?"
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
         where domain = ?
         and type = ?
         and " code-cond)
           domain type]
          (doall (map :uuid rs)))
        (sql/with-query-results rs
          [(str "select uuid
         from xxb_bubbles
         where domain = ?
         and type is null
         and " code-cond)
           domain]
          (doall (map :uuid rs))))))
  ;;;;;;;;;;;;;;;

  (backend [this bubble message params]
    (sql/with-query-results rs
      [ "select xxb_backend_dispatcher(?,?,?,?)"
        (:domain bubble)
        (:uuid bubble)
        message
        (proto/str-nn params)]
      (val (first (first rs))))))

