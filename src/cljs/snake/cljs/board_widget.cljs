(ns snake.cljs.board-widget
  (:require [snake.board :as b]
            [cljs.core.async :as a]
            [dommy.core :as d]
            [goog.events.KeyCodes :as kc])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go]]))

(defprotocol BoardComponent
  (board->node [_])
  (focus! [_])
  (draw-point! [_ [x y]])
  (draw-snake! [_ coll])
  (clear-canvas! [_])
  (commands-channel [_])
  ;; TODO what else does the board need to do?
  
  )



(def key->command
  {kc/UP :up
   kc/DOWN :down
   kc/LEFT :left
   kc/RIGHT :right})

(defn canvas-board-component []
  (let [canvas-size (* b/block-size-px b/board-size)
        $canvas (node [:canvas {:height canvas-size
                                :width canvas-size
                                :style {:border "1px solid black"}
                                :tabindex 0}])]
    (reify BoardComponent
      (board->node [_]
        (node
         [:div {:style {:margin-top "5em"}}
          $canvas]))
      (focus! [_]
        (go
         (a/<! (a/timeout 200))
         (.focus $canvas)))
      
      (draw-point! [_ [x y]]
        (let [context (.getContext $canvas "2d")]
          (.fillRect context (* 10 x) (* 10 y) 10 10)))

      (draw-snake! [_ coll]
        (doseq [coord coll]
          (js/console.log coord)
          (draw-point! _ coord)))
      
      (commands-channel [_]
        (let [out (a/chan)]
          (d/listen! $canvas "keydown"
                     (fn [e]
                       (a/put! out (key->command (.-keyCode e)))))
          out))
      
      (clear-canvas! [_]
        (let [context (.getContext $canvas "2d")]
          (.clearRect context 0 0 canvas-size canvas-size))))))

(defn watch-game! [board !game]
  (add-watch !game :game-watch (fn watch-fun
                          [key !game old-state new-state]
                          (clear-canvas! board)
                          (doseq [[client-id {:keys [snake]}] (:clients new-state)]
                            (draw-snake! board snake))
                          (draw-snake! board (:apples new-state))
                          )))


(defn bind-commands! [board model-command-ch]
  
  (a/pipe (commands-channel board) model-command-ch)
  )

(defn make-board-widget [!game model-command-ch]
  (let [board (doto (canvas-board-component)
                (watch-game! !game)
                (bind-commands! model-command-ch)
                (focus!))]    
    (board->node board)))
