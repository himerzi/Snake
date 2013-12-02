(ns snake.handler
  (:require [ring.util.response :refer [response]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [hiccup.page :refer [html5 include-css include-js]]
            [frodo :refer [repl-connect-js]]
            [chord.http-kit :refer [with-channel]]
            [snake.game-model :refer [wire-up-model!]]))

(defn page-frame []
  (html5
   [:head
    [:title "snake - CLJS Single Page Web Application"]
    (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js")
    (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
    (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")

    (include-js "/js/snake.js")]
   [:body
    [:div.container
     [:div#content]
     [:script (repl-connect-js)]]]))

(defn snake-websocket [req]
  (with-channel req snake-ch
    (doto (atom nil)
      (wire-up-model! snake-ch))))

(defroutes app-routes
  (GET "/" [] (response (page-frame)))
  (GET "/snake" [] snake-websocket)
  (resources "/js" {:root "js"}))

(def app 
  (-> #'app-routes
      api))
