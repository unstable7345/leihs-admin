(ns leihs.admin.resources.inventory-pools.inventory-pool.hourly-reservations.main
  (:require
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.inventory-pools.inventory-pool.hourly-reservations.core :as core]
   [leihs.admin.resources.inventory-pools.inventory-pool.hourly-reservations.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn component []
  [:<>
   [routing/hidden-state-component
    {:did-mount core/fetch}]

   (if-not @core/data*
     [wait-component]
     (let [{:keys [hourly_reservations start_time end_time]} @core/data*]
       [:div#hourly-reservations
        [:h3 "Reservations by the Hour"]
        [table/container
         {:borders false
          :header [:tr [:th "Property"] [:th "Value"]]
          :body [:<>
                 [:tr [:td "Offered"] [:td (if hourly_reservations "Yes" "No")]]
                 (when hourly_reservations
                   [:<>
                    [:tr [:td "From"] [:td start_time]]
                    [:tr [:td "To"] [:td end_time]]])]}]
        [:div.mb-3
         [:i (if hourly_reservations
               (str "Borrowers choose the time of pickup on the first day and of return on the last day, "
                    "between " start_time " and " end_time " in steps of 30 minutes, or whole days. "
                    "Only members of an entitlement group of the pool can reserve, at most as many hours "
                    "as the highest limit of their groups.")
               "Items are only reserved for whole days.")]]
        [:div.mb-3 [edit/button]]
        [edit/dialog]]))])
