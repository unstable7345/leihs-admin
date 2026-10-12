(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main :as entitlement-groups]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn query [inventory-pool-id entitlement-group-id]
  (-> entitlement-groups/base-query
      ;; the limit of hours only matters in a pool with reservations by the hour
      (sql/select [(-> (sql/select :hourly_reservations)
                       (sql/from :inventory_pools)
                       (sql/where [:= :inventory_pools.id :entitlement_groups.inventory_pool_id]))
                   :pool_hourly_reservations])
      (sql/where
       [:= :entitlement_groups.inventory_pool_id inventory-pool-id])
      (sql/where [:= :entitlement_groups.id entitlement-group-id])))

(defn entitlement-group
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id :as route-params} :route-params
    tx :tx :as request}]
  (if-let [eg (->> (query inventory-pool-id entitlement-group-id)
                   sql-format
                   (jdbc-query tx)
                   first)]
    {:body eg}
    {:status 404}))

(defn routes [request]
  (case (:request-method request)
    :get (entitlement-group request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
