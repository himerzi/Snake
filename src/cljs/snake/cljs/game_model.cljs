(ns snake.cljs.game-model
  (:require [cljs.core.async :as a]
            [chord.client :refer [ws-ch]]
            [cljs.reader :refer [read-string]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn ws-url [path]
  (str "ws://" (.-host js/location) path))

(defn watch-state! [server-conn !game]
  (go-loop []
    (when-let [game-state (read-string (:message (a/<! server-conn)))]
      (reset! !game game-state)
      (recur))))

(defn send-commands! [server-conn command-ch]
  (go-loop []
    (when-let [command (a/<! command-ch)]
      (a/>! server-conn (pr-str command))
      (recur))))

(defn wire-up-model! [!game command-ch]
  (go
   (doto (a/<! (ws-ch (ws-url "/snake")))
     (watch-state! !game)
     (send-commands! command-ch))))
