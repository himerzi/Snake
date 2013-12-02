(ns snake.cljs.home
  (:require [dommy.core :as d]
            [snake.cljs.board-widget :refer [make-board-widget]])
  (:require-macros [dommy.macros :refer [node sel1]]))

(defn watch-hash! [!hash]
  (add-watch !hash :home-page
             (fn [_ _ _ hash]
               (when (= "#/" hash)
                 (d/replace-contents! (sel1 :#content)
                                      (make-board-widget))))))
