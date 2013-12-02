(ns snake.cljs.game-model
  (:require [cljs.core.async :as a]
            [snake.cljs.board :as b])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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

(defn apply-commands! [!game command-ch]
  (go-loop []
    (when-let [command (a/<! command-ch)]
      (swap! !game apply-command command)
      (recur))))

(defn wire-up-model! [!game command-ch]
  (doto !game
    (reset! (new-game))
    (repeatedly-tick!)
    (apply-commands! command-ch)))
