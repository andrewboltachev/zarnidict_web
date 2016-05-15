(ns om-ui.react)


(defn reactClass [params]
  (let [inject-this-if-fn (fn [x]
          (if (fn? x)
            (fn [& args]
              (this-as
                this
                (apply x this args)
                )
              )
            x
            )
          )]
    (js/React.createClass
      (clj->js
        (zipmap
          (keys params)
          (map inject-this-if-fn (vals params))
          )
        )
      )
    )
  )

(defn reactElement [name-or-class props & children]
  (apply js/React.createElement name-or-class (clj->js props) (clj->js children))
  )


