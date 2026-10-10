(ns leihs.admin.resources.inventory-pools.inventory-pool.time-slots.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! query]
    :rename {delete! jdbc-delete! query jdbc-query}]))

(defn get-time-slots
  [{{inventory-pool-id :inventory-pool-id} :route-params tx :tx}]
  {:body (-> (sql/select :id
                         :name
                         :inventory_pool_id
                         [[:to_char :start_time "HH24:MI"] :start_time]
                         [[:to_char :end_time "HH24:MI"] :end_time])
             (sql/from :time_slots)
             (sql/where [:= :inventory_pool_id inventory-pool-id])
             (sql/order-by :position :start_time :name)
             sql-format
             (->> (jdbc-query tx)))})

(defn- assert-no-overlaps!
  "Overlapping time slots would make it unclear which reservations compete
  for the same items."
  [tx inventory-pool-id]
  (when-let [{:keys [name1 name2]}
             (-> (sql/select [:a.name :name1] [:b.name :name2])
                 (sql/from [:time_slots :a])
                 (sql/join [:time_slots :b]
                           [:and
                            [:= :a.inventory_pool_id :b.inventory_pool_id]
                            [:< :a.id :b.id]
                            [:< :a.start_time :b.end_time]
                            [:< :b.start_time :a.end_time]])
                 (sql/where [:= :a.inventory_pool_id inventory-pool-id])
                 (sql/limit 1)
                 sql-format
                 (->> (jdbc-query tx))
                 first)]
    (throw (ex-info (str "The time slots \"" name1 "\" and \"" name2 "\" overlap.")
                    {:status 422}))))

(defn patch-time-slots
  "Like the holidays: existing time slots can be deleted and new ones added.
  An existing one is changed by deleting it and adding a new one. Reservations
  keep the time window they were booked for."
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx data :body}]
  (let [slots (->> data
                   (map #(update % :inventory_pool_id uuid))
                   (filter #(= (:inventory_pool_id %) inventory-pool-id)))]
    (doseq [slot (filter #(and (:id %) (:delete %)) slots)]
      (jdbc-delete! tx :time_slots {:id (uuid (:id slot))
                                    :inventory_pool_id inventory-pool-id}))
    (doseq [slot (remove #(or (:id %) (:delete %)) slots)]
      (-> (sql/insert-into :time_slots)
          (sql/values [{:inventory_pool_id inventory-pool-id
                        :name (:name slot)
                        :start_time [:cast (:start_time slot) :time]
                        :end_time [:cast (:end_time slot) :time]}])
          sql-format
          (->> (jdbc/execute! tx))))
    (assert-no-overlaps! tx inventory-pool-id)
    ;; position follows the start time
    (-> (sql/update :time_slots)
        (sql/set {:position [:raw "ordered.position"]})
        (sql/from [(-> (sql/select :id [[:raw "row_number() over (order by start_time, name)"] :position])
                       (sql/from :time_slots)
                       (sql/where [:= :inventory_pool_id inventory-pool-id]))
                   :ordered])
        (sql/where [:= :time_slots.id :ordered.id])
        sql-format
        (->> (jdbc/execute! tx))))
  {:status 204})

(defn routes [request]
  (case (:request-method request)
    :get (get-time-slots request)
    :patch (patch-time-slots request)))
