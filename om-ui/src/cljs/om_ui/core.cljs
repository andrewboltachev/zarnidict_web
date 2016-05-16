(ns om-ui.core
  (:require-macros 
            [cljs.core.async.macros :refer [go go-loop]]
    )
  (:require [cljs.core.async :as async :refer [<! >! put! chan close!]]
            [goog.dom :as gdom]
            ;[om.next :as om :refer-macros [defui]]
            ;[om.dom :as dom]
            [om-ui.dom :as dom]
            [clojure.string :as string]

            [pushy.core :as pushy]
            )
  (:use [om-ui.react :only [reactClass reactElement]])
  (:import [goog Uri]
           [goog.net Jsonp])
  )

(enable-console-print!)

(println "Hello world!")

(defn parse-url [h]
  "#/foo/bar/?a=b&c=d -> {:path \"/foo/bar\", :params {:a \"b\", :c \"d\"}}"
  (let [
      [path params] (string/split h #"\u003f")
      params (if (some? params)
               (into {} (map (comp
                    (fn [[k v]] [(keyword (js/decodeURIComponent k))
                                 (if v
                                   (js/decodeURIComponent v)
                                   v
                                   )
                                 ])
                    #(string/split % "=")) (string/split params "&")))

               )
      ]
    {:path path
     :params params}
    )
  )

(defn jsonp
  ([uri params] (jsonp (chan) uri params))
  ([c uri params]
   (js/myApp.showPleaseWait)
   (println "jsonp" uri params)
  (js/jQuery.ajax
    #js {:method "post"
         :url uri
         :data (clj->js params)
         :success (fn [data]
                    (let [data (js->clj data :keywordize-keys true)]
                      (put! c data)
                      )
                    )
         }

    )

   c
     ))


; UI building blocks


(defn paginator [{:keys [page_number next_page_number previous_page_number first_page_number last_page_number] :as data}]
  (println "data is" (keys data))
  (dom/div #js {:style
                #js {
                     :marginTop 40
                     :marginLeft "auto"
                     :marginRight "auto"
                     :textAlign "center"
                     }}
  (dom/div
   #js
   {:className "btn-toolbar1", :role "toolbar", :aria-label "..."}
   (apply dom/div
    #js
    {:className "btn-group", :role "group", :aria-label "..."}
     (map
    (fn [[title number]]
      (dom/a #js {:className "btn btn-default"
                       :disabled (nil? number)
                  :href (str (:url-path data) (if (= 1 number) "" (str "?page=" number)))
                       ;:click
                  #_(fn [e]
                                (set! js/loaction (str (:url-path data) (if (= 1 number) "" ("?page=" number))))
                                )
                       }
             title
             ))
       [
        ["<<" first_page_number]
        ["<" previous_page_number]
        [page_number nil]
        [">" next_page_number]
        [">>" last_page_number]
        ]
             )
     ))
           )
  )

(defn column1 [attrs & children]
  ; TODO: attrs
(dom/div #js {:className "col-md-4"}
            (apply dom/div #js {:className "well well-sm"
                          :style #js {:minHeight 700} ; TODO more responsove
                          }
                     children
  )))




(defn layout1 [{:keys [heading toolbar] :as options} & children]
(dom/div nil ; main wraper
             (dom/div #js {
                           :style #js {
                              :marginBottom 40
                                   }
                           }
    (dom/div #js {:className "container"
                  
                  :style #js {:paddingTop 20 ; + 20 of h1
                              :paddingBottom 33 ; + 10 of h1
                              }
                  }
                    (dom/div
                      #js
                      {:className "row"}
                      (dom/div #js {:className "col-md-6"}
                                (dom/h2 nil heading))
                      (dom/div #js {:className "col-md-6"}
                                toolbar)
                      
                        )
             )
                      (dom/div #js {:style #js {
                                    
                              :borderTop "1px solid grey"
                              :borderBottom "1px solid lightgrey"
                                                }
                                    }
                      ))
             ; 2nd container
    (dom/div #js {:className "container"}
  
  (apply dom/div
   #js
   {:className "row"}
    children
    )
             )
          )
  )



;; URL helper functions

(def url-ch (chan))

(defn pushy-dispatch-fn [new-url]
  (when new-url
    (put! url-ch new-url)
    )
  )

(defn pushy-match-fn [new-url]
  new-url
  )

(def history
  (pushy/pushy pushy-dispatch-fn pushy-match-fn)
  )


(defn reactUpdateState [this update-fn]
(.setState
                             this
                             (clj->js
                               (update-fn
                                 (or (js->clj (.. this -props -state) :keywordize-keys true) {})
                                 )
                               )
                             )
  )


(def data-ch (chan))


; draggable item
(def cardSource #js {
    :beginDrag (fn [props]
                     #js {
                          :id (.. props -id)
                          :index (.. props -index)
                          }
                     )
})


(def cardTarget
  #js {
       :hover (fn
                [props monitor component]

                (let [
                      dragIndex (.. (.getItem monitor) -index)
                      hoverIndex (.. props -index)
                      ]
                  (when-not (= dragIndex hoverIndex)
                    ; ...
                    (let [
                          hoverBoundingRect (.. (js/ReactDOM.findDOMNode component) getBoundingClientRect)
                          hoverMiddleY (/ (- (.. hoverBoundingRect -bottom) (.. hoverBoundingRect -top)) 2)
                          clientOffset (.. monitor getClientOffset)
                          hoverClientY (- (.. clientOffset -y) (.. hoverBoundingRect -top))
                          ]
                        (when-not
                          (or
                            (and
                              (< dragIndex hoverIndex)
                              (< hoverClientY hoverMiddleY))
                            (and
                              (> dragIndex hoverIndex)
                              (> hoverClientY hoverMiddleY))
                            )
                          ; ...
                          (.moveCard props dragIndex hoverIndex)
                          (set!
                            (.-index (.getItem monitor))
                            hoverIndex
                            )
                          )
                      )
                    )
                  )
                )
})

(def Card
  (reduce
    (fn [a b]
      (b a)
      )
    (reactClass
      {:render (fn [this]
                 (let [{:keys [id text connectDropTarget connectDragSource]} (js->clj (.. this -props) :keywordize-keys true)]
                   (->
                   (apply dom/li #js {
                                :style #js {
                                            :margin 0
                                            }
                                }
                           (.. this -props -children)
                    )

                     connectDropTarget
                     connectDragSource)
                   )
                 )}
      )
    [(js/DropTarget "card" cardTarget (fn [connect] #js {:connectDropTarget
                                                        (.dropTarget connect)
                                                        }))
    (js/DragSource "card" cardSource (fn [connect monitor] #js {:connectDragSource
                                                        (.dragSource connect)
                                                        :isDragging
                                                        (.isDragging monitor)
                                                        }))]
    )
  )

(defn positions
  [pred coll]
  (keep-indexed (fn [idx x]
                  (when (pred x)
                    idx))
                coll))

(defn first-pos [el coll]
  (first (positions (partial = el) coll))
  )


(defn swap [v i1 i2]
  (println "i1" i1)
   (assoc v i2 (get v i1) i1 (get v i2)))


(def LayoutWithDragAndDrop
  (
   (js/DragDropContext js/HTML5Backend)
  (reactClass
    {:moveCard (fn [this dragIndex hoverIndex]
                 (let [v (swap
                                       (->
                                         (js->clj (.. this -props) :keywordize-keys true)
                                               :data :a
                                               ) dragIndex hoverIndex
                                       )]
                 (println "moveCard" v)
                   (jsonp data-ch
                                                                           "/api-view"
                                                                           {:url js/location.pathname ; TODO !!!
                                                                            :action "set"
                                                                            :payload (prn-str v)
                                                                            }
                                                                           )
                   )
                 ; ...


                 (comment
                 #_(.setState this

                            )
                           (let [ids (fn [idx]
                      (let [v 
                 (.map (js/$ (str "#sortable" idx " li")) (fn [_ x] (.attr (js/$ x) "data-id")))]
                        (js/console.log v)
                      v
                      ))
ids1 (js->clj (.toArray (ids 1)))
ids1 (mapv int ids1)
ids1 (swap ids1 (first-pos dragIndex ids1)
                           (first-pos hoverIndex ids1))
_ (println ids1)
]

#_(jsonp data-ch
                                                                           "/api-view"
                                                                           {:url "/screen-5"
                                                                            :action "set"
                                                                            :payload
                                                                            (js/JSON.stringify (clj->js {
                                       :arguments1 (clj->js ids1)
                                       :arguments2 #js []
                                       }))
                                                                            }
                                                                           )
        ))
                 )
                                    
     :render (fn [this]
                  (let [{:keys [a] :as data} (-> (.. this -props) (js->clj :keywordize-keys true) :data)]







(apply dom/ul #js {
                                              :style #js {
                                                          :listStyle "none"
                                                          :padding 0
                                                          }
                                              }
                               
                                  (map-indexed (fn [i line]
                                      ; ...
                                      (let [

                                              span1 (fn [props & children] (apply dom/span
                                                                                  (clj->js (merge (js->clj props) {:style {:lineHeight "27px"
                                                                                                                           :padding 5 :borderRadius 5}})) children))]

(reactElement
                                 Card
                                 {:key i
                                  :index i
                                  :id i
                                  ;:data item
                                  :moveCard (.. this -moveCard)
                                  }
                                              [(dom/div #js {:className "well well-sm"}
                                      (cond
                                        (contains? line :type)
                                        (apply span1 nil
                                               (map (fn [x]
                                                                 (if (string? x) (span1 #js {
                                                                                  :className ({"pre" "bg-warning"} (:value line))
                                                                                  } x)
                                                                   (span1 #js {
                                                                                  :className ({"p" "bg-info"} (:tag x))
                                                                                  } (-> x :value first))
                                                                   )
                                                      )
                                                 (:payload line)
                                                               )
                                               )

                                        :else
                                        (let [{:keys [mhr aut rus]} line
                                              ]
                                          
                                              [(span1 #js {:className " bg-danger"} (apply str mhr))
                                              (when aut (span1 #js {:className "bg-warning"} (apply str aut)))
                                              (span1 #js {:className "bg-info"} (apply str rus))]
                                                       )
                                              )
                                      )]
     )))
                                     a)
                                   )








                  ))
     }
    ))
  )

(def jsonp-ch (chan))

(go-loop [old-value nil]
         (let [[ch url params :as value] (<! jsonp-ch)
               ]
           (when (not= old-value value)
             (jsonp ch url params)
             )
           (recur value)
           )
         )

(def Root
  (reactClass
    {:getInitialState (fn [this]
                       )
     :componentDidMount (fn [this]
                     (go-loop
                       [last-data nil]
                       (let [[v port] (alts! [url-ch data-ch])]
                         ;(println "v" v)
                         (println "port" port)
                         (cond (= port url-ch)
                               (do
                                 (println "url changed to" v)
                       (when v
                         (let [{:keys [path params]} (parse-url v)
                               ]
                           (reactUpdateState
                             this
                             (fn [state]
                               (println "state change" (select-keys state [:url :url-params]) {:url path :url-params params})
                               (let [existing-state (select-keys state [:url :url-params])]
                               (if-not (or (= existing-state
                                          {:url path :url-params params}
                                          ) (empty? existing-state))
                                 1 1
                                 ; ..
                                 )
                                 (when (and (not= last-data v) (not (contains? last-data :url_to_set)))
                                   (println "last-data" last-data v)
                               (put! jsonp-ch [data-ch "/api-view"
                                                     {:url v
                                                      :url-path path ; TODO use this!
                                                      :url-params params
                                                      :action "get"
                                                      }]
                                                     ))
                               (->
                                 state
                                 (assoc :url path)
                                 (assoc :url-params params)
                                              ))
                               )
                             )
                           )))
                               (= port data-ch)
                         (do
                               (reactUpdateState
                                 this
                                 (fn [state]
                                   (let [data (js->clj (js/JSON.parse v) :keywordize-keys true)
                                         url_to_set (:url_to_set data)
                                         data (dissoc data :url_to_set)
                                         ]
                                     (when url_to_set
                                       (println "will set url" url_to_set)
                                       (pushy/set-token! history url_to_set)
                                       )
                                     (assoc state :data (clj->js data))
                                     )
                                   )
                                 )
                           ;(setup-sortable this)
                               (js/myApp.hidePleaseWait)
                           )
                               )
                           (recur (if (= port data-ch) v last-data))
                         )
                       )
                     (put! url-ch (str js/location.pathname js/location.search))
                     (pushy/start! history)
                     )
     :componentDidUpdate (fn [this]
                           ;(setup-sortable this)
                           )
     :render
     (fn [t]
       (let [scale []
             
             {:keys [url data] :as state} (js->clj (.. t -state) :keywordize-keys true)]

(dom/div nil
     (println "url" url)
                (cond

                  (re-matches #"/article/\d+" (or url ""))
                  (layout1 {:heading (:name data)
                            :toolbar
                            (dom/div
   #js
   {:className "btn-toolbar pull-right", :role "toolbar", :aria-label "..."}
   (apply dom/div
    #js
    {:className "btn-group", :role "group", :aria-label "..."}
(dom/a #js {:className "btn btn-default"
                  :href (str "/" (:dictionary_id data))
                       }
             "To dictionary"
             )
     (map
    (fn [[title href]]
      (dom/a #js {:className "btn btn-default"
                       :disabled (nil? href)
                  :href href
                       }
             title
             ))
                            (let [
                                  
        btn-fn (fn [k verb]
                            
                            (when-let [article (-> (get data k))] [(str verb ": " (-> article :name))
                                               (str "/article/" (-> article :id))
                                               ]))
                                  ]
       (filter identity [
        (btn-fn :prev "Previous")
        (btn-fn :next "Next")
        ])
             ))
     ))
                            }


                (dom/div #js {:className "col-md-8"}
                         (let [body (-> data :article :body)
f1 (fn [body]
                            ; ...
                            (doall (filter #(and (map? %) (= (:type %) :InputChar)) (tree-seq #(or (sequential? %) (map? %)) #((if (map? %) vals identity) %) body)))
                            )
                               text
                           (try
                             (cljs.reader/read-string
                           (or body "") ; FIXME "1st match" — what is it?
                             )
                             (catch js/Exception e
                               []
                               ))
                               is-composite (contains? text :original)
                               original (if is-composite (:original text) text)
                               examples (when is-composite (:examples text))
                               original (if is-composite (-> original f1) original)

                               original (map (fn [x]
                                               x
                                               )
                                             original)

                               a (concat original examples)
                               _ (prn original)
                               ]
                           (when body
                             ; ...
                             (reactElement LayoutWithDragAndDrop {:data {:a a}})
                           )
                           )
                                
                  )
                (dom/div #js {:className "col-md-4"}
                         (dom/h3 nil "Revisions:")
                         (apply dom/ul nil
                                 (map
                                   (fn [{:keys [id active user date_added]}]
                                     (let [text (str date_added " by " user)]
                                       (dom/li nil
                                     (if active
                                       (dom/span nil
                                                 text
                                                 )
                                             (dom/a #js {:href (str "/article/" (-> data :id) "?revision="
                                                                    id
                                                                    )} text)))
                                       )
                                     )
                                   (:list data))
                         )
                                
                  ))

                  (re-matches #"/\d+" (or url ""))
                  (layout1 {:heading (:name data)}
                (dom/div #js {:className "col-md-12"}
                         (apply dom/ul nil
                                 (map (fn [{:keys [id name]}] (dom/li nil (dom/a #js {:href (str "/article/" id)} name))) (:list data))
                         )
                         (paginator data)
                                
                  ))

                  (= url "/")
       (layout1 {:heading "Index"}
                ;(partition 3 3 []
                (dom/div #js {:className "col-md-12"}
                           ;(prn-str (:echo data))
                         (apply dom/ul nil
                                 (map (fn [{:keys [id name]}] (dom/li nil (dom/a #js {:href (str "/" id)} name))) (:list data))
                         )
                                
                  ))
                  :else
(layout1 {:heading "Not found"}
                (dom/div #js {:className "col-md-12"}
                         (dom/a #js {:className ""
                                     :href "/"}
                                "Go to home page"
                                )
                  ))

                  )
                )
       ))
       
     }
    )
  )

(println "foo")
(js/ReactDOM.render
  (reactElement
    Root
    {}
    (dom/div nil "a")
    (reactElement "div" nil "b")
    )
  (gdom/getElement "app")
  )
(println "end!")
