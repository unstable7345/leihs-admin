(ns leihs.admin.resources.inventory-pools.inventory-pool.time-slots.main
  (:require
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.inventory-pools.inventory-pool.time-slots.core :as core]
   [leihs.admin.resources.inventory-pools.inventory-pool.time-slots.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn component []
  [:<>
   [routing/hidden-state-component
    {:did-mount core/fetch}]

   (if-not @core/data*
     [wait-component]
     [:div#time-slots
      [:h3 "Time Slots"]
      (if (empty? @core/data*)
        [:p "No time slots: items are only reserved for whole days."]
        [table/container
         {:borders false
          :header [:tr
                   [:th "Name"]
                   [:th "From"]
                   [:th "To"]]
          :body (doall (for [slot @core/data*]
                         [:tr {:key (:id slot)}
                          [:td (:name slot)]
                          [:td (:start_time slot)]
                          [:td (:end_time slot)]]))}])
      [:div.mb-3 [:i "Borrowers can reserve an item for one of these time slots of a single day, "
                  "so that it can be lent to somebody else in another time slot of the same day."]]
      [:div.mb-3 [edit/button]]
      [edit/dialog]])])
