(ns leihs.admin.resources.inventory-pools.inventory-pool.opening-times.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.holidays.core :as holidays-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.holidays.main :as holidays]
   [leihs.admin.resources.inventory-pools.inventory-pool.time-slots.main :as time-slots]
   [leihs.admin.resources.inventory-pools.inventory-pool.workdays.core :as workdays-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.workdays.main :as workdays]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:<>
     [:div.inventory-pool-workdays-debug
      [:hr]
      [:div.inventory-pool-workdays-data
       [:h3 "@workdays/data*"]
       [:pre (with-out-str (pprint @workdays-core/data*))]]]
     [:div.inventory-pool-holidays-debug
      [:hr]
      [:div.inventory-pool-holidays-data
       [:h3 "@holidays/data*"]
       [:pre (with-out-str (pprint @holidays-core/data*))]]]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(pool-core/fetch)}]

   [:article.inventory-pool-opening-times
    [pool-core/header]
    [pool-core/tabs]
    [:div.row
     [:div.col-12.col-xl-6 [workdays/component]]
     [:div.col-12.col-xl-6.mt-4.mt-xl-0 [holidays/component]]]
    [:div.row.mt-4
     [:div.col-12.col-xl-6 [time-slots/component]]]
    [debug-component]]])
