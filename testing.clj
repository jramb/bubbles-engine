; Bubble uuidcode-RRrhDl5CWlnjXCpHhHwJEf
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-RRrhDl5CWlnjXCpHhHwJEf",
 :type nil}

; Bubble uuidref-jSAfxSjwzMJ6dMLUhL5kvF
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-jSAfxSjwzMJ6dMLUhL5kvF",
 :type nil}

; Bubble uuidzu8Yp6YEISkzmamLIOq9k5
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "zu8Yp6YEISkzmamLIOq9k5",
 :type nil}

; Bubble uuidcode-vj4kEYA1nmFcbEhUxqxTNS
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-vj4kEYA1nmFcbEhUxqxTNS",
 :type nil}

; Bubble uuidyBApaagNj38XjazjfJgy7w
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "yBApaagNj38XjazjfJgy7w",
 :type nil}

; Bubble uuid8hELW0MhwGUa3X5y61es7p
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "8hELW0MhwGUa3X5y61es7p",
 :type nil}

; Bubble uuidE7qFcLsv7IBNp1THOk5Xcp
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "E7qFcLsv7IBNp1THOk5Xcp",
 :type nil}

; Bubble uuidpIySWPKc2aUjsj5aIDBw9v
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "pIySWPKc2aUjsj5aIDBw9v",
 :type nil}

; Bubble uuidFGpGixLn3hNAP3sRSbT2tD
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "FGpGixLn3hNAP3sRSbT2tD",
 :type nil}

; Bubble uuidref-OowH6QaLcsekzsrwGxzDhR
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-OowH6QaLcsekzsrwGxzDhR",
 :type nil}

; Bubble uuidref-EaTjzWxYE30RfgIYw6RuyG
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-EaTjzWxYE30RfgIYw6RuyG",
 :type nil}

; Bubble uuidi7EG6mpCJb9J6vJOeuUAA3
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "i7EG6mpCJb9J6vJOeuUAA3",
 :type nil}

; Bubble uuidBa5GeZOKelELDYLgTwf4Fs
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "Ba5GeZOKelELDYLgTwf4Fs",
 :type nil}

; Bubble uuideThMNw54KUVakOInljMWVN
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "eThMNw54KUVakOInljMWVN",
 :type nil}

; Bubble uuidDGKk1IcSGBioIfoP44P8GY
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "DGKk1IcSGBioIfoP44P8GY",
 :type nil}

; Bubble uuidref-FNEjw3eXpAPYBmErv9Su0T
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-FNEjw3eXpAPYBmErv9Su0T",
 :type nil}

; Bubble uuidcode-3QEAH2OlEfj8JD7Rl3cv33
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-3QEAH2OlEfj8JD7Rl3cv33",
 :type nil}

; Bubble uuidref-T4e5wwQ8OjUifLt8wdUslv
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-T4e5wwQ8OjUifLt8wdUslv",
 :type nil}

; Bubble uuidcode-JjLmdyeh1VhSKck3kE7ClL
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-JjLmdyeh1VhSKck3kE7ClL",
 :type nil}

; Bubble uuid7gWJiJWY0m6CjnaNLQBkI1
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "7gWJiJWY0m6CjnaNLQBkI1",
 :type nil}

; Bubble uuidcode-OGkuZXS6TDlMeZZ7tQL5ql
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-OGkuZXS6TDlMeZZ7tQL5ql",
 :type nil}

; Bubble uuidref-yBZq8f9y3LVVCGPvFmt47o
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-yBZq8f9y3LVVCGPvFmt47o",
 :type nil}

; Bubble uuidiO93vevwhugWYLIgepZKwS
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "iO93vevwhugWYLIgepZKwS",
 :type nil}

; Bubble uuidYsNFhBwnQTfVnnX2S9iBWf
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "YsNFhBwnQTfVnnX2S9iBWf",
 :type nil}

; Bubble uuidi8QHWHrJg2vQ8Qrlmx6AXf
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "i8QHWHrJg2vQ8Qrlmx6AXf",
 :type nil}

; Bubble uuidjcbnVBK6rDOsfCzwb1WG8D
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "jcbnVBK6rDOsfCzwb1WG8D",
 :type nil}

; Bubble uuidcode-Nx9K2qRwuwGenQGBxYj7oP
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-Nx9K2qRwuwGenQGBxYj7oP",
 :type nil}

; Bubble uuidcode-uvIgXyKKMoSprUEzYG8GWU
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-uvIgXyKKMoSprUEzYG8GWU",
 :type nil}

; Bubble uuidref-8APCeRxiUtACII0gVsV9hw
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-8APCeRxiUtACII0gVsV9hw",
 :type nil}

; Bubble uuidIRiGKwLHnczFQFNFh2lrsg
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "IRiGKwLHnczFQFNFh2lrsg",
 :type nil}

; Bubble uuidref-YVp3P604R0aTLc1ZSWKelI
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-YVp3P604R0aTLc1ZSWKelI",
 :type nil}

; Bubble uuidwnP1Wy65ccCqie2lrqUIPq
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "wnP1Wy65ccCqie2lrqUIPq",
 :type nil}

; Bubble uuidyvJ0nBtw7mNrq1th1Xl57J
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "yvJ0nBtw7mNrq1th1Xl57J",
 :type nil}

; Bubble uuidcode-xckvnBQiQKFY0IZwmzivAB
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-xckvnBQiQKFY0IZwmzivAB",
 :type nil}

; Bubble uuidref-pUEv2k3UAknJckymzpKjBL
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-pUEv2k3UAknJckymzpKjBL",
 :type nil}

; Bubble uuidzz0LE5sdyphFEBqVFBlWlo
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "zz0LE5sdyphFEBqVFBlWlo",
 :type nil}

; Bubble uuidHPkkQh5W7JBH7QDvwNvirs
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "HPkkQh5W7JBH7QDvwNvirs",
 :type nil}

; Bubble uuidcode-r9wXsgYz7BXqrfboZFdoYs
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-r9wXsgYz7BXqrfboZFdoYs",
 :type nil}

; Bubble uuidref-BgCjxtuA5NeAlaPPX23Lwb
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-BgCjxtuA5NeAlaPPX23Lwb",
 :type nil}

; Bubble uuidfeEBywZ3HiVnfxBRr7479O
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "feEBywZ3HiVnfxBRr7479O",
 :type nil}

; Bubble uuidRJhD02vzEbFDsQ5m14jVAv
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "RJhD02vzEbFDsQ5m14jVAv",
 :type nil}

; Bubble uuidref-ztyNpVb9AU1NY65f84yqBo
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-ztyNpVb9AU1NY65f84yqBo",
 :type nil}

; Bubble uuidP3QvfOxmb8mzh1apcYOPRZ
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "P3QvfOxmb8mzh1apcYOPRZ",
 :type nil}

; Bubble uuidM1zUgosXykBfD7bHfJ08TH
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "M1zUgosXykBfD7bHfJ08TH",
 :type nil}

; Bubble uuidref-29ra7i4Oz2RNe5bIEmEHSW
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-29ra7i4Oz2RNe5bIEmEHSW",
 :type nil}

; Bubble uuidcode-wO95RFp1EK0iICkiyTWHDY
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-wO95RFp1EK0iICkiyTWHDY",
 :type nil}

; Bubble uuidrAqOMVuxtNLfyHghW45XXz
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "rAqOMVuxtNLfyHghW45XXz",
 :type nil}

; Bubble uuidS0siQ0jO5McBv3Lsr2Fzmz
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "S0siQ0jO5McBv3Lsr2Fzmz",
 :type nil}

; Bubble uuidcode-xiHiFO3iKGvmPbNb3OI3NH
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-xiHiFO3iKGvmPbNb3OI3NH",
 :type nil}

; Bubble uuidref-RL9iv29eBmdOmdgMkzQerM
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-RL9iv29eBmdOmdgMkzQerM",
 :type nil}

; Bubble uuidcode-wOEJYnJF2q8Dfmyhrqdp3c
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-wOEJYnJF2q8Dfmyhrqdp3c",
 :type nil}

; Bubble uuidref-kqRjvToEwpGX8QUKrEfmGl
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-kqRjvToEwpGX8QUKrEfmGl",
 :type nil}

; Bubble uuideT1LK7OYu0XDkHCPTX5suK
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "eT1LK7OYu0XDkHCPTX5suK",
 :type nil}

; Bubble uuidwg5f4wENgHVizo8Xx89pw0
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "wg5f4wENgHVizo8Xx89pw0",
 :type nil}

; Bubble uuidfehnjh18F0ypkyRpeOJCRg
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "fehnjh18F0ypkyRpeOJCRg",
 :type nil}

; Bubble uuidcode-DkJBwRAgDWUmwWgt6dktis
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-DkJBwRAgDWUmwWgt6dktis",
 :type nil}

; Bubble uuidref-g8qZ4FIDo7P03H0e6cSX7r
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-g8qZ4FIDo7P03H0e6cSX7r",
 :type nil}

; Bubble uuidqpfEr8G3WTvIqMEdgl6ACR
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "qpfEr8G3WTvIqMEdgl6ACR",
 :type nil}

; Bubble uuidO3R0xl60dSMi08GNlv2C2q
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "O3R0xl60dSMi08GNlv2C2q",
 :type nil}

; Bubble uuidU0nTH7zpfrFhwXVQQ8ds1Y
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "U0nTH7zpfrFhwXVQQ8ds1Y",
 :type nil}

; Bubble uuidcode-05FPGtFyRv0ldMmxg3n9te
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-05FPGtFyRv0ldMmxg3n9te",
 :type nil}

; Bubble uuidref-MyeLVVLU5zICm1RigSMGvj
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-MyeLVVLU5zICm1RigSMGvj",
 :type nil}

; Bubble uuidZqEBI6wsMCdR8VUTQrOMel
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "ZqEBI6wsMCdR8VUTQrOMel",
 :type nil}

; Bubble uuido4j3SczdeVLcxxQSsouzsF
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "o4j3SczdeVLcxxQSsouzsF",
 :type nil}

; Bubble uuidcode-5J575jAUudtbzsV0X3LIC7
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-5J575jAUudtbzsV0X3LIC7",
 :type nil}

; Bubble uuidWfVFlE7Sz1h3JvIZnybg1Q
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "WfVFlE7Sz1h3JvIZnybg1Q",
 :type nil}

; Bubble uuidYOcyCLsP9FnuXhADJz7Krb
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "YOcyCLsP9FnuXhADJz7Krb",
 :type nil}

; Bubble uuidTS9bxhwsMmkfKDS8kHOjOJ
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "TS9bxhwsMmkfKDS8kHOjOJ",
 :type nil}

; Bubble uuidcode-NNsS1psWz9DpfDAEUGeSAm
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-NNsS1psWz9DpfDAEUGeSAm",
 :type nil}

; Bubble uuidref-zdqulMkjAoNqJfMTCjU8l4
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-zdqulMkjAoNqJfMTCjU8l4",
 :type nil}

; Bubble uuid9nurwxWXFIHlHnaExSb50x
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "9nurwxWXFIHlHnaExSb50x",
 :type nil}

; Bubble uuidcode-cmkcqw7cZxdGPOwwi8Z463
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"backend"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend b "ECHO" "made contact!"))),
   "params"
   (fn
    [b]
    (assoc-in
     b
     [:state :backend]
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]})))),
   "init" (fn [b] (assoc-in b [:state :backend] "not set"))},
  :extract
  {"test01"
   (fn
    [b]
    {:body
     (bubbles.tools/backend
      b
      "TEST001"
      (bubbles.tools/as-json
       {:p1 "made contact!", :p2 [true false 2 "hej"]}))})}},
 :uuid "code-cmkcqw7cZxdGPOwwi8Z463",
 :type nil}

; Bubble uuidref-ikER13BfOQU1z95tuZPBDf
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"newkid"
   (fn
    [b]
    (let
     [kid-uuid (str "kid-" (bubbles.tools/gen-uuid))]
     (new-bubble b {:uuid kid-uuid, :type (:type b)})
     (bubbles.tools/add-ref b kid-uuid "test-wrong" "0")
     (bubbles.tools/remove-ref b kid-uuid "test-wrong")
     (bubbles.tools/add-ref b kid-uuid "test" "1")
     (bubbles.tools/add-ref b kid-uuid "test" "2")))},
  :extract
  {"numkids"
   (fn [b] {:body (count (bubbles.tools/get-refs b "test"))})}},
 :uuid "ref-ikER13BfOQU1z95tuZPBDf",
 :type nil}

; Bubble uuidhI432nHB7pmkh1SxVlBzcd
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "hI432nHB7pmkh1SxVlBzcd",
 :type nil}

; Bubble uuidOkiGuEYZ2isfqLJ2v7DO2V
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true))},
  :filter
  {"done"
   (fn
    [b]
    (if
     (>= (get-in b [:state :counter] 0) 10)
     (assoc b :deleted true))),
   "updinc"
   (fn
    [b]
    (assoc-in b [:state :updcnt] (inc (get-in b [:state :updcnt] 0)))),
   "prevbub" (fn [b] (assoc-in b [:state :oldstate] (:old b)))},
  :extract
  {"counter" (fn [b] {:body (get-in [b :state :counter] -1)})}},
 :uuid "OkiGuEYZ2isfqLJ2v7DO2V",
 :type nil}

; Bubble uuidu7OhB1NdyXJ1JYIYjz1MDm
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:extract
  {"simple-xml"
   (fn [b] {:content-type "text/xml", :body "<h1>Hello</h1>"}),
   "plain-text"
   (fn [b] {:body "This is plain text", :content-type "text/plain"})}},
 :uuid "u7OhB1NdyXJ1JYIYjz1MDm",
 :type nil}

; Bubble uuidQxxRjyYgE7gr8ivZY7gRh8
{:parent nil,
 :domain "TEST",
 :name nil,
 :code
 {:message
  {"inc1"
   (fn
    [b]
    (assoc-in
     b
     [:state :counter]
     (+
      (get-in b [:state :counter] 0)
      (get-in b [:params :incval] 1)))),
   "init" (fn [b] (assoc-in b [:state :inited] true)),
   "error" (fn [b] (throw (Exception. "Wanted Exception.")))},
  :extract
  {"counter" (fn [b] {:body (get-in b [:state :counter] -1)}),
   "size" (fn [b] {:body (count (str b))}),
   "fullstate" (fn [b] {:body (b :state)})}},
 :uuid "QxxRjyYgE7gr8ivZY7gRh8",
 :type nil}

