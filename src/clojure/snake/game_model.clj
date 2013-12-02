(ns snake.game-model
  (:require [clojure.core.async :as a :refer [go go-loop] ] 
            [snake.board :as b]))

(defn new-apple []
  (repeatedly 2 #(rand-int b/board-size)))

(defn new-game []
  (let [middle (/ b/board-size 2)]
    {:snake (list [middle middle] [(inc middle) middle])
     :apple (new-apple)
     :direction :left}))

(def movement-vector
  {:left [-1 0]
   :up [0 -1]
   :right [1 0]
   :down [0 1]})

(defn move-snake [{:keys [snake direction] :as game}]
  (let [[head & tail] snake]
    (assoc game
      :snake (cons (map (comp #(mod % b/board-size)
                              +)
                        head
                        (movement-vector direction))
          
                   (cons head (butlast tail)))
      :last-tail (last tail))))

(defn check-apple-collision [{:keys [snake apple last-tail] :as game}]
  (let [[head & _] snake]
    (cond-> game
      (= head apple) (-> (assoc :apple (new-apple))
                         (update-in [:snake] concat [last-tail])))))

(defn apply-tick [game]
  (-> game
      move-snake
      check-apple-collision))

(defn repeatedly-tick! [!game]
  (go-loop []
    (a/<! (a/timeout 100))
    (swap! !game apply-tick)
    (recur)))

(def valid-directions
  {:left #{:up :down}
   :right #{:up :down}
   :up #{:left :right}
   :down #{:left :right}})

(defn valid-direction? [old new]
  ((valid-directions old) new))

(defn apply-command [{:keys [direction] :as game} command]
  (if-let [new-direction (valid-direction? direction command)]
    (assoc game :direction new-direction)
    game))

(defn apply-commands! [!game command-ch exit-ch]
  (go-loop []
    (if-let [{:keys [message]} (a/<! command-ch)]
      (let [command (read-string message)]
        (swap! !game apply-command command)
        (recur))
      (a/close! exit-ch))))

(defn send-state! [!game snake-ch exit-ch]
  (go-loop []
    (let [[v c] (a/alts! [exit-ch (a/timeout 100)] :priority true)]
      (when-not (= c exit-ch)
        (a/>! snake-ch (pr-str @!game))
        (recur)))))

(defn wire-up-model! [!game snake-ch]
  (let [exit-ch (a/chan)]
    (doto !game
      (reset! (new-game))
      (repeatedly-tick!)
      (apply-commands! snake-ch exit-ch)
      (send-state! snake-ch exit-ch))))
