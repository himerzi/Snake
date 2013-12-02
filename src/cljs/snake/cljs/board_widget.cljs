(ns snake.cljs.board-widget
  (:require [snake.cljs.board :as b]
            [cljs.core.async :as a]
            [dommy.core :as d]
            [goog.events.KeyCodes :as kc])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go]]))

(defprotocol BoardComponent
  (board->node [_])
  (clear-board! [_])
  (render-cell! [_ cell color])
  (render-cells! [_ cells color])
  (command-ch [_])
  (focus! [_]))

(def key->command
  {kc/UP :up
   kc/DOWN :down
   kc/LEFT :left
   kc/RIGHT :right})

(defn color-cell! [$canvas [x y] color]
  (let [ctx (.getContext $canvas "2d")]
    (set! (.-fillStyle ctx) color)
    (doto ctx
      (.fillRect (* x b/block-size-px)
                 (* y b/block-size-px)
                 b/block-size-px
                 b/block-size-px))))

(defn clear-canvas! [$canvas]
  (let [ctx (.getContext $canvas "2d")]
    (set! (.-fillStyle ctx) "white")
    (doto ctx
      (.fillRect 0 0
                 (* b/board-size b/block-size-px)
                 (* b/board-size b/block-size-px)))))

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
      (clear-board! [_]
        (clear-canvas! $canvas))
      (render-cell! [_ cell color]
        (color-cell! $canvas cell color))
      (render-cells! [_ cells color]
        (doseq [cell cells]
          (color-cell! $canvas cell color)))
      (command-ch [_]
        (let [ch (a/chan)]
          (d/listen! $canvas :keydown
            (fn [e]
              (when-let [command (key->command (.-keyCode e))]
                (a/put! ch command)
                (.preventDefault e))))
          ch))
      (focus! [_]
        (go
         (a/<! (a/timeout 200))
         (.focus $canvas))))))

(defn watch-game! [board !game]
  (add-watch !game ::renderer
             (fn [_ _ _ {:keys [my-id clients apples]}]
               (clear-board! board)
               (render-cells! board
                              (-> clients
                                  (dissoc my-id)
                                  vals
                                  (->> (mapcat :snake)))
                              "black")
               (render-cells! board (get-in clients [my-id :snake]) "blue")
               (render-cells! board apples "#a00"))))

(defn bind-commands! [board model-command-ch]
  (a/pipe (command-ch board) model-command-ch))

(defn make-board-widget [!game model-command-ch]
  (let [board (doto (canvas-board-component)
                (watch-game! !game)
                (bind-commands! model-command-ch)
                (focus!))]
    (board->node board)))
