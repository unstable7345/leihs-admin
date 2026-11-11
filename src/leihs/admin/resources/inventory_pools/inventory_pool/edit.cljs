(ns leihs.admin.resources.inventory-pools.inventory-pool.edit
  (:require
   [cljs.core.async :as async]
   [clojure.core.async :refer [<! go]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as core]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn patch []
  (let [route (path :inventory-pool
                    {:inventory-pool-id @core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params @data*
                :chan (async/chan)}
               http-client/request
               :chan <!
               http-client/filter-success!)
          (swap! core/cache* assoc @core/path* @data*)
          (search-params/delete-from-url "action")))))

(defn form [& {:keys [is-editing]
               :or {is-editing false}}]
  [:div.inventory-pool.mt-3
   [:div.mb-3
    [form-components/switch-component data* [:is_active]
     :disabled (not @current-user/admin?*)
     :label "Active"]]
   [:div
    [form-components/input-component data* [:name]
     :label "Name"
     :required true]]
   [:div
    [form-components/input-component data* [:shortname]
     :label "Short name"
     :disabled is-editing
     :required true]]
   [form-components/input-component data* [:contact]
    :label "Contact"
    :element :textarea
    :rows 3]
   [form-components/input-component data* [:description]
    :label "Description"
    :element :textarea
    :rows 10]
   [:div
    [form-components/input-component data* [:email]
     :label "Email"
     :type :email
     :required true]]
   [form-components/input-component data* [:notification_email]
    :label "Notification Email"
    :type :email
    :hint "Receives the \"received order\" notifications. Empty: the Email above."]
   [form-components/input-component data* [:email_signature]
    :label "Email Signature"
    :element :textarea
    :rows 5]
   [form-components/input-component data* [:default_contract_note]
    :label "Default Contract Note"
    :element :textarea
    :rows 5]
   [:div.mb-3
    [form-components/switch-component data* [:print_contracts]
     :label "Print Contracts"]]
   [:div.mb-3
    [form-components/switch-component data* [:automatic_suspension]
     :label "Automatic Suspension"]]
   (when (:automatic_suspension @data*)
     [form-components/input-component data* [:automatic_suspension_reason]
      :label "Automatic Suspension Reason"
      :element :textarea
      :rows 5])
   [:div.mb-3
    [form-components/switch-component data* [:required_purpose]
     :label "Hand Over Purpose"]]
   [:div.mb-3
    [form-components/switch-component data* [:deliver_received_order_emails]
     :label "Deliver Received Order Emails"]]
   [:div.mb-3
    [form-components/input-component data* [:borrow_reservation_advance_days]
     :label "Borrow: Reservation Advance Days"
     :type :number
     :min 0]]
   [:div.mb-3
    [form-components/input-component data* [:borrow_maximum_reservation_duration]
     :label "Borrow: Maximum Reservation Duration"
     :type :number
     :min 1]]])

(def open*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit"))))

(defn dialog []
  [:> Modal {:size "lg"
             :centered true
             :show @open*}
   [:> Modal.Header {:closeButton true
                     :onHide #(search-params/delete-from-url "action")}
    [:> Modal.Title "Edit Inventory Pool"]]
   [:> Modal.Body
    [form {:is-editing true}]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url "action")}
     "Cancel"]
    [:> Button {:on-click #(patch)}
     "Save"]]])

(defn button []
  (when (auth/allowed? [pool-auth/pool-inventory-manager?
                        auth/admin-scopes?])
    [:<>
     [:> Button
      {:className ""
       :on-click #(search-params/append-to-url
                   {:action "edit"})}
      "Edit"]]))
