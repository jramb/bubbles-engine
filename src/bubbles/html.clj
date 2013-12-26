(ns bubbles.html
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "HTML functions"
    :url "https://github.com/jramb/bubbles-engine"}
  (:require [hiccup.core :as h])
  ;;(:use [net.cgrand.enlive-html :as enlive :only [deftemplate at html-resource]])
  (:require [bubbles.bust :as bust])    ;hmmmm FIXME?
  (:require [bubbles.setup :as setup])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.pprint :as pp])
  (:import  [java.net URLEncoder]) 
  (:require [hiccup.form :as hf])
  (:require [hiccup.page :as hp]))

;; https://github.com/weavejester/hiccup
;; (html [:div#foo.bar.baz "bang"]) --> "<div id='foo' class='bar baz'>bang</div>'

(def *swatch* :d)
(def *divider* nil) ; defaults to *swatch*

(defn mkdata
  "Produces a hash-map with :data-XXX entries (appends 'data-').
   E.g. (mkdata :a 1 :b 2) ;--> {:data-a 1, :data-b 2}"
  [& data]
  (apply merge
         (for [[k v] (partition 2 data)]
           {(keyword (str "data-" (name k))) v})))

(defn location-url
  [location]
  (when-let [{{:keys [latitude accuracy longitude]} :coords} location]
    (when (and latitude longitude)
      (format "http://maps.google.com/maps?q=%s,%s&radius=%s" latitude longitude accuracy))))
;; (def params {:location {:timestamp "2012-02-10T12:32:00.660Z", :coords {:heading nil, :altitude nil, :latitude 59.40084353333334, :accuracy 52, :longitude 17.94738108888889, :speed nil, :altitudeAccuracy nil}}, :kommentar ""})
;; (location-url (:location params))
;; (location-url nil)
;; (location-url "no such")

(defn qrcode
  "http://code.google.com/apis/chart/infographics/docs/qr_codes.html"
  [txt]
  (str "https://chart.googleapis.com/chart?"
       "chs=200x200"                    ;size
       "&choe=UTF-8"                    ;encoding
       "&cht=qr"                        ;type
       "&chl=" (URLEncoder/encode txt)  ;what
       ))



(defn- footer
  []
  [:div (mkdata :role "footer" :theme *swatch*)
   [:div (mkdata :role "navbar") ;;:iconpos "left"
    [:ul
     (when (get-in bust/*request* [:session :debug])
       [:li [:a.bscan     {:href "javascript:bubbles.thispage()" :data-icon "grid"} "Scan"]]
       [:li [:a.bdebug    {:href "#debug" :data-icon "info"} "Debug"]])
     #_[:li [:a.binfo     {:href "http://www.navigateconsulting.se" :data-icon "info"} "Info"]]
     [:li [:a.blocation {:href "javascript:bubbles.getlocation()" :data-icon "home"} "Location"]]
     [:li [:a.blogin    {:href (format "javascript:bubbles.gologin('%s')"
                                       (get-in bust/*request* [:params :domain]))
                         :data-icon "arrow-r"}
           (let [you (get-in bust/*request* [:session :id :name])]
             (or you "Login"))]]]]])


(defn pp-clojure [sx]
  (when sx
    [:pre (with-out-str (pp/pprint sx))]))


(defn link
  "Produces a <a href...>text</a> link"
  [target text & extra]
  [:a (merge {:href target} (apply mkdata extra)) text])


(defn symbol-to-js
  [s]
  (if s
    (str \' (name s) \') "null"))

(defn message
  "target: '.' = current bubble/domain. '..'= domain (when on bubble)"
  [target text msg form when-ok & extra]
  (let [form (symbol-to-js form)
        when-ok (symbol-to-js when-ok)]
    [:a (merge {:href (str "javascript:bubbles.rpc('../" target "','" msg "'," form "," when-ok ");")}
               (mkdata :ajax "false")
               (apply mkdata extra)) text]))


(defn listview
  "Produces a listview from a seq. Strings become list-dividers by magic."
  [list & extra]
  [:ul (merge
        (mkdata :role "listview" :inset :true :dividertheme (or *divider* *swatch*))
        (apply mkdata extra))
   (map (fn [x] [:li (when (string? x) (mkdata :role "list-divider")) x]) list)])


(defn page
  "Renders a single view unit"
  [title id & content]
  [:div (assoc (mkdata :role "page" :theme *swatch* :add-back-btn :true) :id id)
   [:div (mkdata :role :header :theme *swatch*)
    [:h1 title]]
   [:div (mkdata :role "content")
    content]
   (footer)])


(defn js-src [url]
  [:script {:type "text/javascript" :src url}])

(defn js [body]
  [:script {:type "text/javascript"} body])


(defn javascript-logic
  []
  (list
   ;;removing this for testing;;(js-src "/js/json2.js")             ;DO we really need this?
   (js-src "/js/bubbles.js")
   ;; (if true                             ; FIXME (by setup?)
   ;;   [:script {:src "https://browse rid.org/include.js"}] ; this takes some second!
   ;;   )
   ;; this was a try to use ClojureScript instead. Not worth it as per now...
   ;; (list (js-src "/js/out/goog/base.js")
   ;;         (js-src "/js/bubbles.js")
   ;;         (js "goog.require('bubbles');"))
   ))


(defn app
  "Renders an app (collection of jquerymobile pages). After title an optional parameter map is allowed."
  [title & pages]
  (let [frst    (first pages)
        options (when (map? frst) frst)
        pages   (if (map? frst) (rest pages) pages)]
    {;;:content-type "quatsch/sosse;charset=ISO-8845-1" ;this is overwritten by the [:headers "Content-Type"]!
     :headers {"Content-Type" "text/html;charset=UTF-8"};; works!
     :body (h/html
            (hp/doctype :html5)
            [:html
             [:head
              ;; [:meta {:http-equiv "content-type" :content "text/html; charset=utf-8"}]
              [:title (or title "Bubbles")]
              [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
              [:link {:rel "stylesheet" :href "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css"}]
              [:script {:src "http://code.jquery.com/jquery-1.7.1.min.js"}]
              [:script {:src "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js"}]
              (javascript-logic)
              ]
             [:body pages]
             (if (get-in bust/*request* [:session :debug])
               (page "Debug" :debug
                     [:h3 "Request:"]
                     [:pre (pp-clojure bust/*request*)]))]
            (if-let [login (get-in bust/*request* [:session :id :name])]
              (js (format "bubbles.setident(\"%s\");" login))))}))


(defn form
  [name & contents]
  [:form {:id name :name name}
   contents])

(defn- fieldcontain
  [& contents]
  [:div {:data-role "fieldcontain"}
   contents])

(defn- label
  [name text]
  [:label {:for name} (str text ":")])

(defn input
  ([name] (input name ""))
  ([name text] (input name text :text))
  ([name text type] (input name text type ""))
  ([name text type currval]
     (fieldcontain
      (label name text)
      [:input {:type type :id name :name name :value currval :placeholder text}])))

(defn flip
  [name text vals]
  (fieldcontain
   (label name text)
   [:select {:name name :id name :data-role "slider"}
    (for [[k v] vals]
      [:option {:value k} v])]))

;; (-> {:a 1 :b 2}
;;     (update-in [:a] inc 5)
;;     (assoc-in [:c] "New"))

(defn domain-home
  [req]
  (let [{:keys [body params]} req]
    (let [domain (:domain params)
          title (str "Home of " domain)]
      (app title {:debug true}
           [:div
            (page title  :main
                  "Welcome to the home of " domain
                  (listview
                   [ "Maintenance"
                     (link "inspect" "Inspect domain" :ajax "false")
                     (link "code-mm" "Download" :ajax "false")
                     (link :#upload "Upload")
                     ]))
            (page "Upload" :upload
                  [:form {:method "POST" :enctype "multipart/form-data"
                          :action "upload" :data-ajax "false"
                          }
                   [:div                ; just to divide this from the button
                    (mkdata {:role "collapsible"})
                    (hf/label :uploadFile "Bubble file: ")
                    (hf/file-upload :uploadFile)
                    (hf/hidden-field :format "freemind")
                    (hf/hidden-field :msg    "code-upload")]
                   [:input {:type "submit" :value "Upload" :data-inline "true"}]])]))))

(defn not-admin [req]
  (let [{:keys [params]} req
        domain (:domain params)
        title (str "Home of " domain)]
    (app title {:debug false}
         (page title :main
               "Admin only"))))

(defn testing
  [req]
  (let [{:keys [body params]} req]
    (let [domain (:domain params)
          title (str "Home of " domain)]
      (app title {:debug true}
            (page title  :main
                  "Welcome to the home of " domain
                  #_(listview
                   [ "Maintenance"
                     (link "inspect" "Inspect domain" :ajax "false")
                     (link "code-mm" "Download" :ajax "false")
                     (link :#upload "Upload")
                     ])
                  (listview
                   [ "Testing"
                     (link :#nice "Nice view")
                     (link :#raw "Raw")
                     (link :#poster "Post message")
                     "Other"
                     (link :#callhelp "Call help!" :rel :dialog)
                     ;;"Testtest"
                     ;;[:div "Hepp"]
                     ])
                  (link :#nice "go nice view" :role "button" :inline "true" :theme :b)
                  (link :#nice "go nice dialog" :role "button" :inline "true" :rel "dialog"))
            (page "Raw request" :raw
                  [:pre (pp-clojure req)])
            (page "Post message" :poster
                  [:div "This will " [:strong "post"] " a message!"]
                  (form :myForm
                        (input :yourname "Ditt namn")
                        (flip :yourchoice "Hej" {:yes "Ja" :no "Nej"})
                        (hf/hidden-field :test1 "Hej")
                        (hf/hidden-field :test2 "Hopp"))
                  (message "QrgP3bJ4TlWP4PDVht8CuR" "Do RPC" "inc1" :#myForm :#postOk :role :button :inline :true :theme :e)
                  (message "QrgP3bJ4TlWP4PDVht8CuR" "Wrong msg" "nosuch" :#myForm :#postOk :role :button :inline :true :theme :e)
                  (message "xxxxxx" "Invalid" "inc1" :#myForm :#postOk :role :button :inline :true :theme :e)
                  (message "QrgP3bJ4TlWP4PDVht8CuR" "ok, no whenok" "inc1" :#myForm nil :role :button :inline :true :theme :e))
            (page "Post done" :postOk
                  "The post was successfull")
            (page "another" :nice "Nothing yet"
                  [:div {:data-role "controlgroup" :data-type "horizontal"}
                   (link :#main "Go back" :role :button :rel :back)])
            (page "Upload" :upload
                  [:form {:method "POST" :enctype "multipart/form-data"
                          :action "upload" :data-ajax "false"
                          }
                   [:div                ; just to divide this from the button
                    (mkdata {:role "collapsible"})
                    (hf/label :uploadFile "Bubble file:")
                    (hf/file-upload :uploadFile)
                    (hf/hidden-field :format "freemind")
                    (hf/hidden-field :msg    "code-upload")]
                   [:input {:type "submit" :value "Upload" :data-inline "true"}]])
            (page "Call help" :callhelp
                  "Do you really want to call for help?"
                  (link "tel:+46706605876" "Yes, call Jörg" :role :button :inline :true)
                  (link :#main "Back" :role :button :rel :back :inline :true))
            ))))

;;(def lg-html (enlive/html-resource "html/login.html"))
;;(enlive/select lg-html [:a#loginBtn])
;;(enlive/select lg-html [:div])

;;(def login nil)
;; Sorry, enlive is nothing for me...
#_(enlive/deftemplate loginOLD "html/login.html"
  [req]
  [:a#loginBtn] (enlive/set-attr :href (format "javascript:bubbles.login('%s')"
                                               (get-in req [:params :return])))
  [:a#pwloginBtn] (enlive/set-attr :href (format "javascript:bubbles.pwlogin('%s','%s')"
                                                 (get-in req [:params :return])
                                                 (get-in req [:params :domain])))
  [:a#abortBtn] (enlive/set-attr :href (get-in req [:params :return]))
  ;; [:pre#debug]  (enlive/content (with-out-str (pp/pprint req)))
  )


(defn login [req]
  (let [params (:params req)
        return-url (:return params)
        domain (:domain params)]
    (h/html
     (hp/doctype :html5)
     [:html
      [:head
       [:title "Login"]
       [:meta {:content "width=device-width, initial-scale=1" :name "viewport"}]
       [:link {:rel "stylesheet" :href "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css"}]
       [:script {:src "http://code.jquery.com/jquery-1.7.1.min.js"}]
       [:script {:src "http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js"}]
       (js-src "/js/bubbles.js")
       (js-src "https://login.persona.org/include.js")] ;; was: "https://browse rid.org/include.js"
      [:body
       [:div {:data-add-back-btn "true" :data-role "page" :data-theme *swatch* :id "login"}
        [:div {:data-role "header" :data-theme *swatch*}
         [:h1 "Login"]]
        [:h3 "Choose login method"]
        [:h4 "Mozilla Persona"]
        (link (format "javascript:bubbles.login('%s')" return-url) "Using Mozilla Persona"
              :role "button" :theme "e")
        [:h4 "Password login"]
        [:form {:method "POST" :action "/validate" :id "passwordForm"}
         [:div {:data-role "fieldcontain"}
          (input :username "Username") ; <input type="text" name="username" id="username" value="" placeholder="Username" data-mini="true"/>
          (input :password "Password" :password) ; <input type="password" name="password" id="password" value="" placeholder="Password"/>
          (link (format "javascript:bubbles.pwlogin('%s','%s')" return-url domain)
                "Password login" :role "button" :id "pwloginBtn")
          ]]
        (link return-url "Back" :role "button" :inline "true")
        [:pre {:id "debug"}]]]])))

