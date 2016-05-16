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
                   (dom/li #js {:data-id id}
                           text
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
   (assoc v i2 (get v i1) i1 (get v i2)))


(def LayoutWithDragAndDrop
  (
   (js/DragDropContext js/HTML5Backend)
  (reactClass
    {:moveCard (fn [this dragIndex hoverIndex]
                 (println "moveCard" dragIndex hoverIndex)
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

(jsonp data-ch
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
                                    
     :render (fn [this]
                  (let [{:keys [arguments1 arguments2] :as data} (-> (.. this -props) (js->clj :keywordize-keys true) :data)]







                  (layout1 {:heading (:issue data)}
                (dom/div #js {:className "col-md-6"}
                (dom/h4 #js {:style #js {:marginBottom 30}} "Rank each of the arguments below relating to this proposal:")
(apply dom/ul #js {:id "sortable1"}
                        (map-indexed (fn [& [i {:keys [id text]}]]
                               (reactElement
                                 Card
                                 {:key id
                                  :index id
                                  :id id
                                  :text text
                                  :moveCard (.. this -moveCard)
                                  }
                                 )
                               )
                             arguments1
                             )
                         )
                         )
                (dom/div #js {:className "col-md-6 "}

(dom/div #js {:className ""
              :style #js {
                      :backgroundColor "#434343"
                          :padding 15
                          :marginBottom 15
                      }}                
(dom/div #js {:className ""
              :style #js {
                          :color "white"
                          :fontSize 20
                          :marginBottom 30
                      }}                
(dom/span nil "Drag good arguments here:")
(dom/br nil)
(dom/span nil "(put stronger arguments higher up)")
         )

(apply dom/ul #js {:id "sortable2"}
                        (map (fn [{:keys [id text]}]
                               (dom/li #js {:data-id id}
                                       text
                                )
                               )
                             arguments2
                             )
                         )
(dom/div #js {:className "clearfix"})
                         )
                         
(dom/div #js {:className ""
              :style #js {
                      :backgroundColor "#f4cccc"
                          :padding 15
                      }}                

(dom/div #js {:className ""
              :style #js {
                          :color "black"
                          :fontSize 20
                          :marginBottom 30
                      }}                
(dom/span nil "Drag bad or irrelevant arguments here")
(dom/span #js {:className "glyphicon glyphicon-trash"
               :style #js {:paddingLeft 15}})
(dom/ul #js {:id "sortable3"}
                        
                         )
         )



(dom/div #js {:className "clearfix"})
                         )
                


               ))))
     }
    ))
  )

(def jsonp-ch (chan))

(go-loop [old-value nil]
         (let [[ch url params :as value] (<! jsonp-ch)]
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
                                 (when (not= last-data v)
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
                                   (assoc state :data (js/JSON.parse v))
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
                  (= url "/screen-4")
       (apply layout1 {:heading (:issue data)}


                (dom/div #js {:className ""}
                (dom/h4 #js {:style #js {:marginBottom 30}} "Rate each of the arguments below relating to this proposal:")
                         )
                (map (fn [{:keys [id text value] :as argument}]
                (dom/div #js {:className "row"
                              :style #js {:marginBottom 30}}
                         (dom/div #js {:className "col-md-4"}
                         #_(dom/textarea #js {:className "form-control" :disabled true
                                            :value text
                                            })
                         (dom/div #js {:className "well"
                                            } text)
                         )
                (dom/div #js {:className "col-md-2"}
                         (dom/select #js {:className "form-control" :disabled true}
                                  (dom/option #js {:value true} "Supporting")
                                  )
                         )
                (dom/div #js {:className "col-md-6"}
                         (apply dom/div #js {:className "row"
                                       :style #js {}}
                                  (map
                                    (fn [[value1 option]]
                                      (let [htmlId (str id "_" value1)]
                                    (dom/div #js {:className "col-md-2"}
                                    (dom/label #js {:className "" :htmlFor htmlId} option)
                                    (dom/div #js {:className ""}
                                             (dom/input #js {:type "radio" :name htmlId :id htmlId ; TODO
                                                             :checked (= value value1)
                                                             :onChange (fn [e]
                                                                         (jsonp data-ch
                                                                           "/api-view"
                                                                           {:url "/screen-4"
                                                                            :action "set"
                                                                            :params {:id id :value value1}}
                                                                           )
                                                                         false
                                                                         )
                                                             }
                                             )
                                             ))
                                    ))
                                scale)
                                  )
                         ))) (:arguments data))
                         )
                                

                  (= url "/screen-5")
                  (reactElement
                    LayoutWithDragAndDrop
                    {
                     :data data
                     }
                    )

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
                               text
                           (cljs.reader/read-string
                           (or body "") ; FIXME "1st match" — what is it?
                             )
                               is-composite (contains? text :original)
                               original (if is-composite (:original text) text)
                               examples (when is-composite (:examples text))

                               a examples
                               ]
                           (when body
                           (apply dom/ul #js {
                                              :style #js {
                                                          :list-style "none"
                                                          :padding 0
                                                          }
                                              }
                                  (map
                                    (fn [line]
                                      ; ...
                                      (cond
                                        ; TODO
                                        :else
                                        (let [{:keys [mhr aut rus]} line
                                              span1 (fn [props & children] (apply dom/span
                                                                                  (clj->js (merge (js->clj props) {:style {:lineHeight "27px"
                                                                                                                           :padding 5 :borderRadius 5}})) children))]
                                      (dom/li  #js {
                                              :style #js {
                                                          :margin 0
                                                          }
                                              }
                                              (dom/div #js {:className "well well-sm"}
                                              (span1 #js {:className " bg-danger"} (apply str mhr))
                                              (when aut (span1 #js {:className "bg-warning"} (apply str aut)))
                                              (span1 #js {:className "bg-info"} (apply str rus))
                                                       )
                                              )
                                      )))
                                     a)
                                   ))
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
                                                                    (-> data :article :id)
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
