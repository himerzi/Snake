(ns snake.game-model
  (:require [clojure.core.async :as a :refer [go go-loop] ] 
            [snake.board :as b]))

(defn new-apple []
  (repeatedly 2 #(rand-int b/board-size)))

(defn new-game []
  (let [middle (/ b/board-size 2)]
    {:snake (list [middle middle] [(inc middle) middle])
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

(defn move-snakes [game]
  (update-in game [:clients] (fn [clients]
                               (->> (for [[user-id client] clients]
                                      [user-id (move-snake client)])
                                    (into {})))))

(defn check-apple-collisions [{:keys [snake apple last-tail] :as game}]
  (let [[head & _] snake]
    (cond-> game
      (= head apple) (-> (assoc :apple (new-apple))
                         (update-in [:snake] concat [last-tail])))))


(defn send-state! [{:keys [client-conns clients apples]}]
  (doseq [[user-id conn] client-conns]
    (a/put! conn (pr-str {:clients clients
                          :my-id user-id
                          :apples apples}))))

(defn apply-tick [game]
  (-> game
      move-snakes
      (doto send-state!)))

(defn repeatedly-tick! [!game]
  (go-loop []
    (a/<! (a/timeout 200))
    (swap! !game apply-tick)
    (recur)))

(def valid-directions
  {:left #{:up :down}
   :right #{:up :down}
   :up #{:left :right}
   :down #{:left :right}})

(defn valid-direction? [old new]
  ((valid-directions old) new))

(defn apply-command [game user-id command]
  (let [direction (get-in game [:clients user-id :direction])]
    (if-let [new-direction (valid-direction? direction command)]
      (assoc-in game [:clients user-id :direction] new-direction)
      game)))

(defn remove-client [game user-id client-conn]
  (-> game
      (update-in [:clients] dissoc user-id)
      (update-in [:client-conns] dissoc user-id)))

(defn apply-commands! [!game user-id client-conn]
  (go-loop []
    (if-let [{:keys [message]} (a/<! client-conn)]
      (let [command (read-string message)]
        (swap! !game apply-command user-id command)
        (recur))
      
      (swap! !game remove-client user-id client-conn))))

(defn add-client [game user-id client-conn]
  (-> game
      (assoc-in [:clients user-id] (new-game))
      (assoc-in [:client-conns user-id] client-conn)))

(defn wire-up-model! []
  (let [!game (doto (atom {:apples (set (repeatedly 10 new-apple))})
                (repeatedly-tick!))]
    (def !test-game !game)

    (fn client-joined! [client-conn]
      (let [user-id (str (java.util.UUID/randomUUID))
            exit-ch (a/chan)]
        (doto !game
          (swap! add-client user-id client-conn)
          (apply-commands! user-id client-conn))))))
