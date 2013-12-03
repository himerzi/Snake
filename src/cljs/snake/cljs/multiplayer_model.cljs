(ns snake.cljs.multiplayer-model
  (:require [cljs.core.async :as a]
            [chord.client :refer [ws-ch]]
            [cljs.reader :refer [read-string]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn ws-url [path]
  (str "ws://" (.-host js/location) path))

(defn watch-state! [server-conn !game]
  ;; TODO every time we receive a message from the server, update the game state
  
  )

(defn send-commands! [server-conn command-ch]
  ;; TODO send our commands to the server
  
  )

(defn wire-up-model! [!game command-ch]
  (go
   (doto (a/<! (ws-ch (ws-url "/snake")))
     (watch-state! !game)
     (send-commands! command-ch))))
