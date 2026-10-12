(ns leihs.admin.resources.inventory-pools.inventory-pool.hourly-reservations.edit
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.hourly-reservations.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as BS :refer [Button Form Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn patch []
  (let [route (path :inventory-pool-hourly-reservations
                    {:inventory-pool-id (:id @pool-core/data*)})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params @data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (core/fetch)
          (search-params/delete-from-url "action")))))

(def half-hours
  (for [h (range 24) m ["00" "30"]]
    (str (when (< h 10) "0") h ":" m)))

(defn time-select [id label k]
  [:div.form-group.mb-3 {:style {:width "40%"}}
   [:label {:class-name "font-weight-bold mb-2" :for id} label]
   [:select.form-control {:id id
                          :value (or (get @data* k) "")
                          :required true
                          :on-change #(swap! data* assoc k (-> % .-target .-value))}
    [:option {:value ""} ""]
    (for [t half-hours] [:option {:key t :value t} t])]])

(defn form []
  (if-not @data*
    [wait-component]
    (let [{:keys [hourly_reservations start_time end_time]} @data*
          end-not-after-start? (and start_time end_time (<= end_time start_time))]
      [:> Form {:id "hourly-reservations-form"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (when-not end-not-after-start? (patch)))}
       [:div.form-check.mb-3
        [:input.form-check-input {:type "checkbox"
                                  :id "hourly-reservations"
                                  :checked (boolean hourly_reservations)
                                  :on-change #(swap! data* assoc :hourly_reservations (-> % .-target .-checked))}]
        [:label.form-check-label {:for "hourly-reservations"} "Offer reservations by the hour"]]
       (when hourly_reservations
         [:<>
          [:div.d-flex.gap-3
           [time-select "hourly-reservations-start" "From" :start_time]
           [time-select "hourly-reservations-end" "To" :end_time]]
          (when end-not-after-start?
            [:div.text-danger.mb-3 "\"To\" must be after \"From\"."])])])))

(def open*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit-hourly-reservations"))))

(defn dialog []
  [:> Modal {:size "lg"
             :centered true
             :show @open*}
   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-from-url "action")}
    [:> Modal.Title "Edit Reservations by the Hour"]]
   [:> Modal.Body
    [form]
    [:div.mb-3 [:i "Existing reservations keep their times. The limit of hours is set per "
                "entitlement group (in the lending section, Groups)."]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "hourly-reservations-form"}
     "Save"]]])

(defn button []
  (when (auth/allowed? [pool-auth/pool-inventory-manager?
                        auth/admin-scopes?])
    [:> Button
     {:on-click #(search-params/append-to-url {:action "edit-hourly-reservations"})}
     "Edit"]))
