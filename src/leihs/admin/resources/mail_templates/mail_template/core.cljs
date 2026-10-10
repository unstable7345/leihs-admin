(ns leihs.admin.resources.mail-templates.mail-template.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Col Form Row]]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom :refer [reaction]]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :mail-template-id presence)
                ":mail-template-id")))

(defonce path*
  (reaction
   (path :mail-template {:mail-template-id @id*})))

(defonce cache* (reagent/atom nil))
(defonce data*
  (reaction (get @cache* @path*)))

(defn fetch []
  (http-client/route-cached-fetch cache* {:route @path*}))

(def template-variables-for-order-component
  [:ul
   [:li [:code :comment]],
   [:li [:code :email_signature]],
   [:li [:code :inventory_pool.name]],
   [:li [:code :inventory_pool.contact]],
   [:li [:code :inventory_pool.description]],
   [:li [:code :inventory_pool.workdays]],
   [:li [:code :inventory_pool.holidays]],
   [:li [:code :inventory_pool.email_signature]],
   [:li [:code :order_url]]
   [:li [:code :purpose]],
   [:li [:code :reservations]
    [:ul
     [:li [:code :l.end_date]]
     [:li [:code :l.model_name]],
     [:li [:code :l.quantity]],
     [:li [:code :l.start_date]]
     [:li [:code :l.start_time] ", " [:code :l.end_time] ", " [:code :l.time_slot]
      " (time slot reservations only, e.g. 10:00–12:00; empty for whole days)"]]],
   [:li [:code :user.name]]])

(def template-variables-for-user-component
  [:ul
   [:li [:code :due_date]],
   [:li [:code :email_signature]],
   [:li [:code :inventory_pool.name]],
   [:li [:code :inventory_pool.contact]],
   [:li [:code :inventory_pool.description]],
   [:li [:code :inventory_pool.workdays]],
   [:li [:code :inventory_pool.holidays]],
   [:li [:code :inventory_pool.email_signature]],
   [:li [:code :quantity]],
   [:li [:code :reservations]
    [:ul
     [:li [:code :l.end_date]]
     [:li [:code :l.item_inventory_code]]
     [:li [:code :l.model_name]],
     [:li [:code :l.quantity]],
     [:li [:code :l.start_date]]
     [:li [:code :l.start_time] ", " [:code :l.end_time] ", " [:code :l.time_slot]
      " (time slot reservations only, e.g. 10:00–12:00; empty for whole days)"]]],
   [:li [:code :user.name]]])

(defn template-variables []
  (case (:type @data* "order")
    "order" template-variables-for-order-component
    "user" template-variables-for-user-component
    nil))

(defn form [action data*]
  [:> Form {:id "mail-template-form"
            :on-submit (fn [e] (.preventDefault e) (action))}
   [:> Row {:className "form-group"}
    [:> Col {:md 12 :lg 8}
     [:> Form.Group {:control-id "subject"}
      [:> Form.Label [:strong "Subject"]]
      [:input.form-control
       {:id "subject"
        :required true
        :value (or (:subject @data*) "")
        :onChange (fn [e] (swap! data* assoc :subject (-> e .-target .-value)))}]]]
    [:> Col [:strong "No available subject variables"]]]
   [:> Row
    [:> Col {:md 12 :lg 8}
     [:> Form.Group {:control-id "body"}
      [:> Form.Label [:strong "Body"]]
      [:textarea.form-control
       {:id "body"
        :rows 30
        :required true
        :value (or (:body @data*) "")
        :onChange (fn [e] (swap! data* assoc :body (-> e .-target .-value)))}]]]
    [:> Col
     [:div [:strong "Body variables"]
      (template-variables)]]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.mail-template-debug
     [:hr]
     [:div.mail-template-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))
