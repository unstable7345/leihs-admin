(ns leihs.admin.resources.inventory-pools.inventory-pool.hourly-reservations.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn- get-settings [tx inventory-pool-id]
  (-> (sql/select :hourly_reservations
                  [[:to_char :hourly_reservations_start_time "HH24:MI"] :start_time]
                  [[:to_char :hourly_reservations_end_time "HH24:MI"] :end_time])
      (sql/from :inventory_pools)
      (sql/where [:= :id inventory-pool-id])
      sql-format
      (->> (jdbc-query tx))
      first))

(defn- half-hour? [time]
  (boolean (re-matches #"\d{2}:(00|30)" (str time))))

(defn patch-settings
  "Reservations by the hour: whether the pool offers them and its hours, in
  steps of 30 minutes. Existing reservations keep their times."
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx {:keys [hourly_reservations start_time end_time]} :body}]
  (when (or hourly_reservations start_time end_time)
    (when-not (and start_time end_time)
      (throw (ex-info "The start and the end of the hours are required." {:status 422})))
    (when-not (and (half-hour? start_time) (half-hour? end_time))
      (throw (ex-info "The hours have to be given in steps of 30 minutes (HH:00 or HH:30)."
                      {:status 422})))
    (when-not (neg? (compare start_time end_time))
      (throw (ex-info "The end has to be after the start." {:status 422}))))
  (-> (sql/update :inventory_pools)
      (sql/set {:hourly_reservations (boolean hourly_reservations)
                :hourly_reservations_start_time (when start_time [:cast start_time :time])
                :hourly_reservations_end_time (when end_time [:cast end_time :time])})
      (sql/where [:= :id inventory-pool-id])
      sql-format
      (->> (jdbc/execute! tx)))
  ;; the audit of changes requires a status
  {:status 200 :body (get-settings tx inventory-pool-id)})

(defn routes [{{inventory-pool-id :inventory-pool-id} :route-params tx :tx :as request}]
  (case (:request-method request)
    :get {:body (get-settings tx inventory-pool-id)}
    :patch (patch-settings request)))
