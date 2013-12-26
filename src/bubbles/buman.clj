(ns bubbles.buman
  ^{:author "Jörg Ramb <jorg@jramb.com>"
    :doc "Graphical Interface to Bubbles Engine"
    :url "https://github.com/jramb/bubbles-engine"}
  (:gen-class)
  (:require [clojure.tools.logging :as log])
  (:require [clojure.inspector :as inspector])
  (:require [cheshire.core :as json])
  (:require [bubbles.bucl :as bucl])
  (:require [clojure.pprint :as pp])
  )
;; 2010-11-12 by Jörg Ramb for the Bubbles project.
;;
;; Good stuff: http://download.oracle.com/javase/tutorial/uiswing/components/index.html


(def setup
  (atom {:buse "http://localhost:8080"
         :domain "DEV"
         :use-json true
         :request-client {
           :user-agent "BUMAN/1.0"
           :connection-timeout 10
           :request-timeout 1000
           :idle-timeout 100
          }
         }))


(import '(javax.swing
           JLabel JButton JPanel JFrame UIManager JSplitPane JTextPane
           JMenu JMenuBar JMenuItem ListSelectionModel
           WindowConstants JTabbedPane
           JTable JScrollPane JTree KeyStroke
           )
        '(javax.swing.table  DefaultTableModel)
        '(javax.swing.tree   TreeNode DefaultMutableTreeNode TreeModel TreeSelectionModel TreeCellRenderer)
        '(java.awt
           BorderLayout)
        '(java.awt.event
           KeyEvent ActionEvent)
        )

; macros {{{
(defmacro on-action [component event & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [~event] ~@body))))

(defmacro on-tree-selected [component event & body]
  `(. ~component addTreeSelectionListener
      (proxy [javax.swing.event.TreeSelectionListener] []
        (valueChanged [~event] ~@body))))

(defmacro on-tree-will-expand [component event & body]
  `(. ~component addTreeWillExpandListener
      (proxy [javax.swing.event.TreeWillExpandListener] []
        (treeWillCollapse [~event]) ; do nothing, needs to be specified otherwise collapse does not work
        (treeWillExpand [~event] ~@body)))) ; throw a javax.swing.tree.ExpandVetoException to prevent

(defmacro on-table-changed [component event & body]
  `(. ~component addTableModelListener
      (proxy [javax.swing.event.TableModelListener] []
        (tableChanged [~event] ~@body))))

(defmacro on-list-selected [component event & body]
  `(. ~component addListSelectionListener
      (proxy [javax.swing.event.ListSelectionListener] []
        (valueChanged [~event] ~@body))))

(defmacro log-exceptions [& body]
  `(try ~@body
      (catch Exception e# (log/error (str e#)))
      (catch Error e# (log/error (str e#)))
     ))
;}}}


(defn pprint-str [m]
  (if (:use-json @setup)
    (json/generate-string m {:pretty true})
    (with-out-str
      (pp/pprint m))))

(defn ping-buse [buse update-label]
  (log-exceptions
    (.setText update-label (format "Fetching %s" (bucl/ping-url)))
    (binding [bucl/*buse* buse]
      (let [p (bucl/ping)]
        (.setText update-label (format "Connected to %s: DB=%s, local time=%s, Bubbles=%d" buse (:name p) (:now p) (int (:num_bubbles p)))))
    )))


(defn fetch-all-bubbles [buse domain]
  (binding [bucl/*domain* domain
            bucl/*buse* buse]
    (bucl/fetch-all-bubbles)))


(defn fetch-bubble [buse domain uuid]
  (binding [bucl/*domain* domain
            bucl/*buse* buse]
    (log-exceptions
      (bucl/fetch-bubble uuid))))




(defn table-row-selected [event ui-objects]
  (when-not (.getValueIsAdjusting event)
    (let [event (bean event)
          source (bean (:source event))
          model (:model ui-objects) ; table (view) and model use different numbering!
          table (:table ui-objects)
          selected-index (:anchorSelectionIndex source)
          model-index (.convertRowIndexToModel table selected-index)
          bubble-uuid (.getValueAt model model-index 0) ; .getValueAt also works with table (view), but indexes might differ!
          ]
      (let [bubble (fetch-bubble (:buse @setup) (:domain @setup) bubble-uuid)]
        (doto (:bubble-inspector ui-objects)
          (.setModel (inspector/tree-model bubble))
          ;(.setCellRenderer (cell-renderer))
          )
        (.setText (:bubble-clojure ui-objects)
          (str "<pre>"
               (with-out-str
                 (pp/pprint bubble))
               "</pre>"))
        (.setText (:bubble-json ui-objects)
          (str "<pre>"
               (json/generate-string bubble {:pretty true})
               "</pre>"))
          ))))


(defn init-table
  "Initializes a JTable and returns it."
  [ui-objects]
  (let [model (doto (DefaultTableModel.)
                (.addColumn "UUID")
                (.addColumn "Name")
                (.addColumn "Type")
                (.addColumn "Created")
                (.addColumn "Updated")
                ;(.addRow (to-array ["1" "2" "3"]))
                )
        table (JTable. model)]
    (doto table
            ;(to-array-2d [["hej" "hop" "x"] ["UUID" "Name" "y"]])
            ;(to-array ["UUID" "Name" "Type"]))
      (.setAutoCreateRowSorter true)
      (.setFillsViewportHeight true)
      )
    (doto (.getSelectionModel table)
        (.setSelectionMode ListSelectionModel/SINGLE_SELECTION)
        (on-list-selected event
          ; need to add model and table to ui-objects here, since later we have a different ui-objects...!
          (table-row-selected event (assoc ui-objects :model model :table table)
          )))
    table
    ))


(defn purge-model [model]
  (.setRowCount model 0); deletes all rows
  )


(defn reload-table [buse domain table]
  (let [model (.getModel table)]
    (log-exceptions
      (purge-model model)
      (let [bubbles (fetch-all-bubbles buse domain)]
        (log/debug (str "Counted bubbles=" (count bubbles)))
        (dorun (map
                 (fn [b]
                   (let [{:keys [uuid name creation_date update_date type]} b]
                     (.addRow model (to-array [uuid name type creation_date update_date]))
            ))
             bubbles))))))


(defn node-will-expand [path ui-objects]
  (.setText (:footer ui-objects) (str path))
  )

(defn node-selected [path ui-objects]
  (condp = (alength path) ; path is an Object[]
    ;; 2: buse-level
    2 (do
        (swap! setup assoc :buse (aget path 1)); reset! buse (aget path 1))
        (ping-buse (:buse @setup) (:footer ui-objects)))
    ;; 3: domain-level
    3 (do
        ;(reset! buse (aget path 1))
        ;(reset! domain (aget path 2))
        (swap! setup assoc :buse (aget path 1) :domain (aget path 2))
        (reload-table (:buse @setup) (:domain @setup) (:table ui-objects)))
    (.setText (:footer ui-objects) (str (count path) "->" path))
  ))

(defn init-tree
  "Initializes a new tree and sets it to the given container"
  [container ui-objects]
  (let [top (doto (DefaultMutableTreeNode. (:buse @setup))
              )
        ;;; TreeSelectionListener
        ;;; obs: addTreeExpansionListener, addTreeWillExpandListener?
        root (doto (DefaultMutableTreeNode. "Instances")
              (.add top))
        tree (doto (JTree. root)
               (on-tree-will-expand event
                (node-will-expand (.getPath (.getPath event)) ui-objects)
                )
               (on-tree-selected event
                (node-selected (.getPath (.getPath event)) ui-objects)
                )) ]
    (.setSelectionMode (.getSelectionModel tree) TreeSelectionModel/SINGLE_TREE_SELECTION)
    (.add top (doto (DefaultMutableTreeNode. "DEV")
                (.add (DefaultMutableTreeNode. "toucher"))
                (.add (DefaultMutableTreeNode. "toucherX"))
                ))
    (dorun (for [domain ["TEST" "DUMMY"]]
      (.add top (DefaultMutableTreeNode. domain))))
    (.setViewportView container tree)
    root))


(defn inspector-treeOLD []
  (doto (JTree. (java.util.Hashtable.))
     (comment override public String convertValueToText (Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus))

                           ))

(defn nice-key
  "If the value is a :keyword then return the name of it. Not really correct, but nicer."
  [v]
  (pr-str (if (keyword? v) (name v) (str v))))

(defn render-tree-value
  "Returns the str for the given value in the tree."
  [value leaf]
  (str ;value "->" leaf ".."
    (cond
     (map? value)
        (if leaf value "{}")
     (instance? clojure.lang.MapEntry value)
        (if leaf
          (str (nice-key (first value)) " = " (pr-str (second value)))
          (nice-key (first value)))
      :else (nice-key value)
      )))


(defn inspector-tree []
  (proxy [javax.swing.JTree] [(java.util.Hashtable.)]
    (convertValueToText [value selected expanded leaf row hasFocus]
      (render-tree-value value leaf))))


(defn buman-app []
  (let [;label (JLabel. "Counter: 0")
        footer (JLabel. "Buse Manager")
        bubble-inspector (inspector-tree)
        bubble-details-clojure (doto (JTextPane.)
                         (.setContentType "text/html")
                         )
        bubble-details-json (doto (JTextPane.)
                         (.setContentType "text/html")
                         )
        ui-objects {:bubble-json bubble-details-json
                    :bubble-clojure bubble-details-clojure
                    :bubble-inspector bubble-inspector
                    :footer footer}
        table (init-table ui-objects)
        ui-objects (assoc ui-objects :table table)
        table-container (JScrollPane. table)
        ;button (doto (JButton. "Add 1") (on-action evnt (.setText label (str "Counter: " (swap! counter inc)))))
        tree-container (JScrollPane.)
        tree (init-tree tree-container ui-objects)
        mi-open (doto (JMenuItem. "Open")
                  (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_O ActionEvent/ALT_MASK))
                  (on-action evnt
                     (init-tree tree-container ui-objects)))
        mi-ping (doto (JMenuItem. "Ping")
                  (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_P ActionEvent/ALT_MASK))
                  (on-action evnt
                     (ping-buse (:buse @setup) footer)))
        mi-load (doto (JMenuItem. "Reload bubbles")
                  (.setAccelerator (KeyStroke/getKeyStroke KeyEvent/VK_R ActionEvent/ALT_MASK))
                        (on-action evnt
                          (reload-table (:buse @setup) (:domain @setup) table)))
        menu  (doto (JMenu. "Action")
                (.setMnemonic KeyEvent/VK_A)
                (.add mi-ping)
                (.add mi-open)
                (.add mi-load)
                (.addSeparator)
                (.add (JMenuItem. "Exit" KeyEvent/VK_X))
                )
        menuBar  (doto (JMenuBar.)
                   (.add menu))
        ;bubble-container (doto (JPanel.) (.add bubble-details-json))
        tab-pane (doto (JTabbedPane. JTabbedPane/BOTTOM)
                   (.addTab "Inspector" bubble-inspector)
                   (.addTab "JSON" (JScrollPane. bubble-details-json))
                   (.addTab "Source" (JScrollPane. bubble-details-clojure))
                   )
        split-panel (JSplitPane.
                      JSplitPane/HORIZONTAL_SPLIT
                      tree-container (JSplitPane.
                                       JSplitPane/VERTICAL_SPLIT
                                       table-container tab-pane))
        panel (doto (JPanel.)
                (.setOpaque true)
                (.setLayout (BorderLayout.))
                (.add menuBar BorderLayout/PAGE_START)
                ; NORTH, SOUTH, EAST, WEST, CENTER, PAGE_START, PAGE_END, LINE_START, LINE_END
                ;(.add label BorderLayout/LINE_START)
                ;(.add tree-container BorderLayout/LINE_START)
                ;(.add table-container BorderLayout/CENTER)
                (.add split-panel BorderLayout/CENTER)
                ;(.add button BorderLayout/EAST)
                (.add footer BorderLayout/PAGE_END)
                )]
    (doto (JFrame. "BUMAN - Buse Manager")
      (.setContentPane panel)
      (.setSize 800 600)
      (.setVisible true)
      ;(.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setDefaultCloseOperation WindowConstants/EXIT_ON_CLOSE)
      )
    (log/info "App started")))


(defn -main [& args]
  (comment
    (System/setProperty "http.proxySet" "true")
    (System/setProperty "http.proxyHost" "localhost")
    (System/setProperty "http.proxyPort" "8888"))
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
  (buman-app))

(comment
  good-commands
  (swap! setup assoc :buse "http://localhost:8080")
  (-main)
  (-main "http://localhost:8080"))
