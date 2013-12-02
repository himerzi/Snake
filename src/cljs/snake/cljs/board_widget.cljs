(ns snake.cljs.board-widget
  (:require-macros [dommy.macros :refer [node sel1]]))

(defprotocol BoardComponent
  (board->node [_]))

(defn canvas-board-component []
  (let [$canvas (node [:div {:style {:margin-top "5em"}}
                       [:canvas {:height 400 :width 400}]])]
    (reify BoardComponent
      (board->node [_] $canvas))))

(defn make-board-widget []
  (let [board (canvas-board-component)]
    (board->node board)))
