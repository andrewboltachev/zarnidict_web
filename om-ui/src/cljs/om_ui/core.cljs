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

(defn column1 [attrs & children]
  ; TODO: attrs
(dom/div #js {:className "col-md-4"}
            (apply dom/div #js {:className "well well-sm"
                          :style #js {:minHeight 700} ; TODO more responsove
                          }
                     children
  )))




(defn layout1 [{:keys [heading] :as options} & children]
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
                      (dom/div #js {:className "col-md-12"}
                                (dom/h2 nil heading))
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

(def Root
  (reactClass
    {:componentDidMount (fn [this]
                     (go-loop
                       []
                       (let [[v port] (alts! [url-ch data-ch])]
                         (println "v" v)
                         (println "port" port)
                         (cond (= port url-ch)
                       (when v
                         (let []
                           (reactUpdateState
                             this
                             (fn [state]
                               (jsonp data-ch "/api-view"
                                                     {:url v
                                                      :action "get"
                                                      :params []
                                                      }
                                                     )
                               (assoc state :url v)
                                              )
                               )
                             )
                           )
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
                           (recur)
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
       (let [scale (js->clj js/SCREEN4ARGUMENT_CHOICES)
             
             {:keys [url data] :as state} (js->clj (.. t -state) :keywordize-keys true)]

(dom/div nil
(dom/button #js {:style #js {:position "fixed"
:top 30
:right 30} :className "btn btn-warning"
:onClick (fn [_]
(jsonp data-ch
                                                                           "/api-view"
                                                                           {:url url
                                                                            :action "reset"
                                                                            }
                                                                           )
)
} "Reset")
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

                  :else
       (layout1 {:heading "Index"}
                         (apply dom/ul nil
                                 (map (fn [[href title]] (dom/li nil (dom/a #js {:href href} title))) [["/screen-4" "Screen 4"] ["/screen-5" "Screen 5"]])
                         )
                                
                  )
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
