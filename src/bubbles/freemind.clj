(ns bubbles.freemind
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Freemind functionality for Bubbles"
    :url "https://github.com/jramb/bubbles-engine"}
  (:require [bubbles.bust :as bust])
  ;;(:require [clojure.xml :as xml])
  (:require [hiccup.core :as h])
  ;;(:require [hiccup.form-helpers :as hf])
  ;;(:require [hiccup.page-helpers :as hp])
  ;; reading XML requires some libs
  (:require [clojure.xml :as xml])
  (:require [clojure.zip :as zip])
  (:require [clojure.tools.logging :as log])
  ;;
  (:require [clojure.contrib.zip-filter.xml :as zf]) ;moved to clojure/data.zip in 1.3
  (:require [clojure.contrib.duck-streams :as duck])
  (:require [clojure.pprint :as pp])
  (:require [clojure.contrib.prxml :as prxml]) ;contrib .prxml it was alternative, will be data.xml in 1.3?
  )

(defn- pretty-print [sx]
  (when sx
    (with-out-str (pp/pprint sx))))

(comment
  ;; Problem: clojure.xml generates attributes with single quote, not double quote (which is ok, but freemind chokes...)
  ;; &#xa; means CR in long node text when freenode saves!
  )

;;;;;; start freemind to bubbles
(comment
  (def x (xml/parse (java.io.File. "/tmp/test.mm")))
  (second
   (freemind-to-bubbles x))
  (freemind-to-bubbles x)
  (def zipper (zip/xml-zip x))
  zipper
  (zf/xml-> zipper zip/root)
  (= zipper (zf/xml-> zipper zip/root)))

(defn read-freenode-string [str]
  (if str
    (read-string str)))

(def code-map {:extract "Extract"
               :message "Message"
               :filter  "Filter"})

(defn parse-code
  [n]
  (if-let [c (zf/xml1-> n :node [(zf/attr= :TEXT "Code")])]
    (into {}
          (for [[k v] code-map]
            {k
             (into {}
                   (for [fun (zf/xml-> c :node [(zf/attr= :TEXT v)] :node)]
                     (let [name (zf/xml1-> fun (zf/attr :TEXT))
                           code (zf/xml1-> fun :node (zf/attr :TEXT))]
                       {name (read-freenode-string code)})))}))))

(defn node-to-bubble
  "Takes a location n which is a node and returns a (hierarchic!) sequence of bubbles"
  [n domain type]
  #_(lazy-seq) ;; FIXME?
  (let [id    (zf/attr n :TEXT)
        code  (parse-code n)
        state-str (zf/xml1-> n :node [(zf/attr= :TEXT "State")] :node (zf/attr :TEXT))
        state (read-freenode-string state-str)]
    (cons {:uuid id
           :domain domain
           :type type
           ;; :name ???
           ;; :parent ???
           ;; :deleted-flag ???
           :state state
           :code code}                  ;should we use inheritance for type?
          (map #(node-to-bubble % domain id)
               (zf/xml-> n :node [(zf/attr= :STYLE "bubble")]))
          #_(for [sn (zf/xml-> n :node [(zf/attr= :STYLE "bubble")])]
            (node-to-bubble sn domain id)))))


(defn freemind-to-bubbles
  "This loads the freemind map and returns a sequence of nodes.
   x is expected to be a parsed XML structure, starting with freenodes 'map'"
  [x]
  (let [z (zip/xml-zip x)           ;make a zipper
        dom-loc (zf/xml1-> z :node) ;get the top node (should be only one)
        domain  (zf/xml1-> dom-loc :attribute (zf/attr :VALUE))
        ]
    (assert domain)
    (for [n (zf/xml-> dom-loc :node [(zf/attr= :STYLE "bubble")])]
      (node-to-bubble n domain nil))))

(defn upload-domain
  "Req contains"
  [domain file]
  (let [bubbles (freemind-to-bubbles (xml/parse file))
        num (count (flatten bubbles))]
    (log/info (pretty-print bubbles))
    (bust/mass-load-bubbles bubbles domain)
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (str "Bubbles loaded: " num)}))

;;;;;;;; end freemind to bubbles

(defn freemind [body]
  ;; <!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
  [:map 
   {:version "0.9.0"}
   [:comment! "To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net"]
   [:attribute_registry {:SHOW_ATTRIBUTES "hide"}]
   body])

(defn attribute [[k v]]
  [:attribute {:NAME k :VALUE v}])

(defn- not-empty? [x]
  ;; should we use "not-empty" instead?
  (not (empty? x)))

(defn now []
  (.getTime (java.util.Date.)))

(defn node
  "Returns a Freemind node."
  ([text] (node text nil nil))
  ([text body] (node text body nil))
  ([text body options]
     (when (not-empty? text)
       [:node
        (merge {:CREATED    (or (:created options) (now))
                ;; :ID         (:id options)
                :FOLDED     (if body "true" "false")
                :MODIFIED   (now);;(or (:modified options) (now))
                :POSITION   "right"
                :TEXT       (str text)
                }
               options) body])))

(defn node-if-body
  "Generates a node only if the node has a body"
  ([text] nil)
  ([text body] (node-if-body text body nil))
  ([text body options]
     (when (not-empty? body)
       (node text body options))))

(defn arrow
  [parent type]
  (if parent (log/debug (str parent "-" type)))
  (if (and type parent (not= parent type))
    (log/spy [:arrowlink {:DESTINATION type}])))

(defn node-from-map
  "Generates a simple, non-recursive node from a map."
  [m]
  (for [[k v] m]
    (node (str k) (node (pretty-print v)))))

(defn code-node
  "code is the code-map of the bubbe, consisting of :message, :filter, :extract"
  [code]
  (when code
    (map (fn [[k v]]
           (node-if-body v (node-from-map (k code))))
         code-map)
    #_(list                             ; or like this...
     (node-if-body "Extract" (node-from-map  (:extract code)))
     (node-if-body "Message" (node-from-map  (:message code)))
     (node-if-body "Filter"  (node-from-map  (:filter code))))))


(def bubble-as-node)                    ;forward def!

(defn list-bubbles [domain type which]
  (map #(bubble-as-node domain %)
       (bust/db-get-bubbles-of-type domain type which)))

(defn bubble-as-node [domain uuid]
  (let [b (bust/db-retrieve-bubble {:uuid uuid :domain domain})]
    ;;(log/spy (:parent b)) (log/spy (:type b))
    (node (:uuid b)
          (cons (attribute ["type" "bubble"])
                (list
                 (arrow (:parent b) (:type b))
                 (node-if-body "State" (node (pretty-print (:state b)))
                               {:STYLE "fork"})
                 (node-if-body "Code" (code-node (:code b))
                               {:STYLE "fork"})
                 (list-bubbles domain (:uuid b) :all)))
          {:STYLE "bubble"
           ;; FIXME: MODIFIED + CREATED
           :ID (:uuid b)})))

(defn freemap-domain [domain]
  (freemind
   (node domain
         (cons (attribute ["domain" domain])
                 (list-bubbles domain nil :all))
         {:FOLDED "false"})))

;;; This is called from buse/domain-extract ("code-mm")
;;; curl http://localhost:8090/d/ICHNAEA/x/code-mm
(defn render-domain [domain]
  (with-out-str ;; FIXME!!! THIS IS MEGA-BAD
    (binding [prxml/*prxml-indent* 1]
      (bust/with-bust
        (prxml/prxml (freemap-domain domain))))))


#_(defmacro redir "Redirects output of body to filename"
  [filename & body]
  `(with-open [w# (duck/writer ~filename)]
     (binding [*out* w#] ~@body)))


(defn html-page [url title]
  (h/html
   ;; it seems that this doctype ruins the full window feature...?
   ;;(hp/doctype :html5)                  ;  :html4   :xhtml-strict   :xhtml-transitional   :html5
   [:html
    [:head [:title (or title "Bubbles")]
     [:style {:type "text/css"} "body {margin: 0px;}"  ;"body { margin-left:0px; margin-right:0px; margin-top:0px; margin-bottom:0px }"
      ]]
    [:body
     [:applet {:code "freemind.main.FreeMindApplet.class"
               :archive "/freemindbrowser.jar" :width "100%" :height "100%"}
      [:param {:name "type" :value "application/x-java-applet;version=1.4"}]
      [:param {:name "scriptable" :value "false"}]
      [:param {:name "modes" :value "freemind.modes.browsemode.BrowseMode"}]
      [:param {:name "browsemode_initial_map" :value (str "./" url)}] ;; leading "." important, assume url starts with "/"
      [:param {:name "initial_mode" :value "Browse"}]
      [:param {:name "selection_method" :value "selection_method_direct"}]]]]))



(comment
  (bust/with-bust
    (bust/db-retrieve-bubble {:uuid "ichnaea-start-utv-navigate" :domain "ICHNAEA"}))
  
  (bust/with-bust
    (bust/get-code-bubbles "ICHNAEA" "ichnaea-start-utv-navigate"))
  
  (bust/with-bust
    (bust/get-code-bubbles "ICHNAEA" nil))

  (bust/gen-uuid)
  
  @bust/db-connection
  ;; tests
  (prxml/prxml [:map {:version "0.9.0"} nil])
  (with-out-str)
  (bubbble-as-node "ichnaea-start-utv-navigate")
  (binding [prxml/*prxml-indent* 1]
    (prxml/prxml (freemind nil)))
  (redir "freetest.mm"
    (render-domain "ICHNAEA"))
  (redir "freetest.mm"
         (binding [prxml/*prxml-indent* 1]
           (bust/with-bust
             (prxml/prxml (freemap-domain "ICHNAEA")))))
  (node "Hejsan")
  (node "Hejsan" nil {:id 5})
  (node "Hejsan" [(node "Hepp") (node "hopp")]
        {:id 5})
  (node "Hej" (node "Hopp" nil :id 5))
  (binding [prxml/*prxml-indent* 1]
        (prxml/prxml
         (freemind (node "Hejsan" (node "hopp" nil {:id 5})
                          (map #(node (str "hepp: " %) nil {:id %}) (range 10))
                         )))))

(comment schema
;;   <?xml version="1.0" encoding="UTF-8"?>
;; <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">

;;  <xs:element name='Parameters'>
;;   <xs:complexType>
;; 	  <!--Is the time management plugin.-->
;;    <xs:attribute name='REMINDUSERAT' type='xs:integer' use='optional'/>
;;   </xs:complexType>
;;  </xs:element>
;;  <!--Used for node notes.-->
;;  <xs:element name='text'>
;;   <xs:complexType/>
;;  </xs:element>

;;  <xs:element name='arrowlink'>
;;   <xs:complexType>
;;    <xs:attribute name='COLOR' type='xs:string' use='optional'/>
;;    <xs:attribute name='DESTINATION' type='xs:string' use='required'/>
;;    <xs:attribute name='ENDARROW' type='xs:string' use='optional'/>
;;    <xs:attribute name='ENDINCLINATION' type='xs:string' use='optional'/>
;;    <xs:attribute name='ID' type='xs:string' use='optional'/>
;;    <xs:attribute name='STARTARROW' type='xs:string' use='optional'/>
;;    <xs:attribute name='STARTINCLINATION' type='xs:string' use='optional'/>
;;   </xs:complexType>
;;  </xs:element>

;;  <xs:element name='cloud'>
;;   <xs:complexType>
;;    <xs:attribute name='COLOR' type='xs:string' use='optional'/>
;;   </xs:complexType>
;;  </xs:element>

;;  <xs:element name='edge'>
;;   <xs:complexType>
;;    <xs:attribute name='COLOR' type='xs:string' use='optional'/>
;;    <xs:attribute name='STYLE' type='xs:string' use='optional'/>
;;    <xs:attribute name='WIDTH' type='xs:string' use='optional'/>
;;   </xs:complexType>
;;  </xs:element>

;;  <xs:element name='font'>
;;   <xs:complexType>
;;    <xs:attribute name='BOLD' use='optional'>
;;     <xs:simpleType>
;;      <xs:restriction base='xs:string'>
;;       <xs:enumeration value='true'/>
;;      </xs:restriction>
;;     </xs:simpleType>
;;    </xs:attribute>
;;    <xs:attribute name='ITALIC' use='optional'>
;;     <xs:simpleType>
;;      <xs:restriction base='xs:string'>
;;       <xs:enumeration value='true'/>
;;       <xs:enumeration value='false'/>
;;      </xs:restriction>
;;     </xs:simpleType>
;;    </xs:attribute>
;;    <xs:attribute name='NAME' type='xs:string' use='required'/>
;;    <xs:attribute name='SIZE' use='required' type='xs:integer'/>
;;   </xs:complexType>
;;  </xs:element>

;;  <xs:element name='hook'>
;;   <xs:complexType>
;;    <xs:sequence>
;;     <xs:element ref='Parameters' minOccurs='0' maxOccurs='1'/>
;;     <xs:element ref='text' minOccurs='0' maxOccurs='1'/>
;;    </xs:sequence>
;;    <xs:attribute name='NAME' type='xs:string' use='required'/>
;;   </xs:complexType>
;;  </xs:element>

;;  <xs:element name='icon'>
;;   <xs:complexType>
;;    <xs:attribute name='BUILTIN' type='xs:string' use='required'/>
;;   </xs:complexType>
;;  </xs:element>

;;  <xs:element name='map'>
;;   <xs:complexType>
;;    <xs:sequence>
;;     <xs:element ref='node'/>
;;    </xs:sequence>
;;    <xs:attribute name='version' type='xs:string' use='required'/>
;;   </xs:complexType>
;;  </xs:element>

;;  <xs:element name='node'>
;;   <xs:complexType>
;;    <xs:choice minOccurs='0' maxOccurs='unbounded'>
;;     <xs:element ref='arrowlink'/>
;;     <xs:element ref='cloud'/>
;;     <xs:element ref='edge'/>
;;     <xs:element ref='font'/>
;;     <xs:element ref='hook'/>
;;     <xs:element ref='icon'/>
;;     <xs:element ref='node'/>
;;    </xs:choice>
;;    <xs:attribute name='BACKGROUND_COLOR' type='xs:string' use='optional'/>
;;    <xs:attribute name='COLOR' type='xs:string' use='optional'/>
;;    <xs:attribute name='FOLDED' use='optional'>
;;     <xs:simpleType>
;;      <xs:restriction base='xs:string'>
;;       <xs:enumeration value='true'/>
;;       <xs:enumeration value='false'/>
;;      </xs:restriction>
;;     </xs:simpleType>
;;    </xs:attribute>
;;    <xs:attribute name='ID' type='xs:ID' use='optional'/>
;;    <xs:attribute name='LINK' type='xs:string' use='optional'/>
;;    <xs:attribute name='POSITION' use='optional'>
;;     <xs:simpleType>
;;      <xs:restriction base='xs:string'>
;;       <xs:enumeration value='left'/>
;;       <xs:enumeration value='right'/>
;;      </xs:restriction>
;;     </xs:simpleType>
;;    </xs:attribute>
;;    <xs:attribute name='STYLE' type='xs:string' use='optional'/>
;;    <xs:attribute name='TEXT' type='xs:string' use='required'/>
;;    <xs:attribute name='CREATED' type='xs:integer' use='optional'/>
;;    <xs:attribute name='MODIFIED' type='xs:integer' use='optional'/>
;;    <xs:attribute name='HGAP' type='xs:integer' use='optional'/>
;;    <xs:attribute name='VGAP' type='xs:integer' use='optional'/>
;;    <xs:attribute name='VSHIFT' type='xs:integer' use='optional'/>
;;    <xs:attribute name='ENCRYPTED_CONTENT' type='xs:string' use='optional'/>
;;   </xs:complexType>
;;  </xs:element>
;; </xs:schema>
)
