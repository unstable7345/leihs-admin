(ns leihs.admin.resources.inventory-pools.inventory-pool.time-slots.edit
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [com.rpl.specter :as s]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.time-slots.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as BS :refer [Button Form Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn prepare-for-patch [data]
  (->> data
       (map #(if (:new %) (dissoc % :new :id) %))))

(defn patch []
  (let [route (path :inventory-pool-time-slots
                    {:inventory-pool-id (:id @pool-core/data*)})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params (prepare-for-patch @data*)
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (core/fetch)
          (search-params/delete-from-url "action")))))

(defn add-new-time-slot-comp []
  (let [template {:inventory_pool_id (:id @pool-core/data*)}
        new-slot (reagent/atom template)]
    (fn []
      (let [{:keys [start_time end_time]} @new-slot
            end-not-after-start? (and start_time end_time (<= end_time start_time))]
        [:form.form-inline.align-items-end
         {:on-submit (fn [e]
                       (.preventDefault e)
                       (swap! data* conj (assoc @new-slot
                                                :new true
                                                :id (str (random-uuid))))
                       (reset! new-slot template))}
         [:div.form-group.mb-2.mr-2 {:style {:width "35%"}}
          [:label {:class-name "font-weight-bold mb-2" :for "time-slot-name"} "Name"]
          [:input.form-control.w-100 {:type "text" :placeholder "Name"
                                      :id "time-slot-name"
                                      :value (:name @new-slot)
                                      :required true
                                      :on-change #(swap! new-slot assoc :name (-> % .-target .-value))}]]
         [:div.form-group.mb-2.mr-2.flex-column.align-items-start {:style {:width "20%"}}
          [:label {:class-name "font-weight-bold mb-2" :for "time-slot-start"} "From"]
          [:input.form-control.w-100 {:type "time"
                                      :id "time-slot-start"
                                      :value start_time
                                      :required true
                                      :on-change #(swap! new-slot assoc :start_time (-> % .-target .-value))}]]
         [:div.form-group.mb-2.mr-2.flex-column.align-items-start {:style {:width "20%"}}
          [:label {:class-name "font-weight-bold mb-2" :for "time-slot-end"} "To"]
          [:input.form-control.w-100 {:type "time"
                                      :id "time-slot-end"
                                      :value end_time
                                      :required true
                                      :on-change #(swap! new-slot assoc :end_time (-> % .-target .-value))}]]
         [:> Button {:type "submit"
                     :className "btn-info mb-2 ml-3"
                     :disabled end-not-after-start?}
          "Add"]
         (when end-not-after-start?
           [:div.text-danger.w-100 "\"To\" must be after \"From\"."])]))))

(defn time-slot-row-comp [slot]
  [:<>
   [:td (cond->> (:name slot) (:delete slot) (vector :s))]
   [:td (cond->> (:start_time slot) (:delete slot) (vector :s))]
   [:td (cond->> (:end_time slot) (:delete slot) (vector :s))]
   [:td
    (let [specter-path [s/ALL #(= (:id %) (:id slot))]]
      (if (:delete slot)
        [:> Button
         {:onClick (fn [_] (swap! data* (fn [d] (s/transform specter-path #(dissoc % :delete) d))))
          :variant "outline-secondary" :size "sm"}
         "Restore"]
        [:> Button
         {:onClick (fn [_]
                     (swap! data*
                            (fn [d]
                              (if (:new slot)
                                (s/setval specter-path s/NONE d)
                                (s/transform specter-path #(assoc % :delete true) d)))))
          :variant "outline-danger" :size "sm"}
         "Delete"]))]])

(defn form []
  (if-not @pool-core/data*
    [wait-component]
    [:<>
     [add-new-time-slot-comp]
     [:> Form {:id "time-slots-form"
               :on-submit (fn [e]
                            (.preventDefault e)
                            (patch))}
      [table/container
       {:borders false
        :header [:tr
                 [:th {:style {:width "35%"}}]
                 [:th {:style {:width "20%"}}]
                 [:th {:style {:width "20%"}}]
                 [:th {:style {:width "15%"}}]]
        :body
        (doall (for [slot (sort-by :start_time @data*)]
                 [:tr {:key (:id slot)
                       :class (when (:delete slot) "table-danger")}
                  [time-slot-row-comp slot]]))}]]]))

(def open*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit-time-slots"))))

(defn dialog []
  [:> Modal {:size "lg"
             :scrollable true
             :centered true
             :show @open*}
   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-from-url "action")}
    [:> Modal.Title "Edit Time Slots"]]
   [:> Modal.Body
    [form]
    [:div.mb-3 [:i "Existing reservations keep the time they were booked for. "
                "To change a time slot, delete it and add a new one."]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "time-slots-form"}
     "Save"]]])

(defn button []
  (when (auth/allowed? [pool-auth/pool-inventory-manager?
                        auth/admin-scopes?])
    [:> Button
     {:on-click #(search-params/append-to-url {:action "edit-time-slots"})}
     "Edit"]))
