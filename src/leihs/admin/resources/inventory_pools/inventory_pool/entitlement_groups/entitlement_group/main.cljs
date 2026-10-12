(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core :as entitlement-group :refer [header tabs]]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main]
   [leihs.admin.state :as state]
   [react-bootstrap :as BS :refer [Button]]))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @entitlement-group/data*))]]]))

(defn default-value-component [value]
  [:span (str value)])

(def table-conf
  [{:key :name
    :label "Name"}
   {:key :is_verification_required
    :label "Verification required"}
   {:key :max_reservation_hours
    :label "Max. hours per reservation (by the hour)"
    :value-component (fn [value] [:span (if value (str value) "No limit")])}
   {:key :entitlements_count
    :label "Number of models"}
   {:key :users_count
    :label "Number of users"}
   {:key :direct_users_count
    :label "Number of direct users"}
   {:key :groups_count
    :label "Number of groups"}])

(defn edit-group-button []
  [:> Button
   {:variant "primary"
    :href (str "/manage/"
               @inventory-pool/id* "/groups/"
               @entitlement-group/id* "/edit")}
   "Edit"])

(defn overview-table []
  [:section
   (when-let [data @entitlement-group/data*]
     [:div
      [table/container {:className "entitlement-group-overview-table"
                        :header [:tr
                                 [:th "Property"]
                                 [:th "Value"]]
                        :body (for [{key :key label :label value-component :value-component
                                     :or {value-component default-value-component}} table-conf]
                                [:tr {:key key}
                                 [:td.label label]
                                 [:td.value [value-component (-> data key)]]])}]])])

(defn page []
  [:article.entitlement-group
   [header]
   [tabs]
   (when (and @inventory-pool/id* @entitlement-group/id*)
     [:<>
      [overview-table]
      [edit-group-button]])
   [debug-component]])
