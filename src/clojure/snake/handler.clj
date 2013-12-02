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
    [:title "Snake - Likely Clojure School"]
    (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js")
    (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
    (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")

    (include-js "/js/snake.js")]
   [:body
    [:div.container
     [:div#content]
     [:script (repl-connect-js)]]]))

(let [client-joined! (wire-up-model!)]
  (defn snake-websocket [req]
    (with-channel req client-conn
      (client-joined! client-conn))))

(defroutes app-routes
  (GET "/" [] (response (page-frame)))
  (GET "/snake" [] snake-websocket)
  (resources "/js" {:root "js"}))

(def app 
  (-> #'app-routes
      api))
