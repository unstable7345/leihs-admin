(ns leihs.admin.resources.inventory-pools.inventory-pool.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.bidi :refer [match-route]]
   [clojure.core.match :refer [match]]
   [clojure.set :as set]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [paths]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! insert! query update!]
    :rename {query jdbc-query,
             delete! jdbc-delete!
             update! jdbc-update!
             insert! jdbc-insert!}]
   [taoensso.timbre :refer [spy]]))

(def base-fields #{:id
                   :is_active
                   :contact
                   :description
                   :email
                   :name
                   :shortname})

(def extra-fields #{:automatic_suspension
                    :automatic_suspension_reason
                    :borrow_maximum_reservation_duration
                    :borrow_reservation_advance_days
                    :default_contract_note
                    :deliver_received_order_emails
                    :email_signature
                    :notification_email
                    :print_contracts
                    :required_purpose})

(def create-fields base-fields)
(def get-fields (set/union base-fields extra-fields))
(def patch-fields get-fields)

(defn inventory-pool
  [{{inventory-pool-id :inventory-pool-id} :route-params tx :tx :as request}]
  {:body (-> (apply sql/select get-fields)
             (sql/from :inventory-pools)
             (sql/where [:= :id inventory-pool-id])
             sql-format
             (->> (jdbc-query tx))
             first)})

(defn create-inventory-pool [{tx :tx data :body :as request}]
  (if-let [inventory-pool (jdbc-insert! tx :inventory_pools
                                        (select-keys data create-fields))]
    {:status 201, :body inventory-pool}
    {:status 422
     :body "No inventory-pool has been created."}))

(defn validate-deactivation [tx inventory-pool-id]
  (when (-> (sql/select
             [[:exists
               (-> (sql/select 1)
                   (sql/from :items)
                   (sql/where [:or [:= :inventory_pool_id inventory-pool-id]
                               [:= :owner_id inventory-pool-id]])
                   (sql/where [:is-null :retired]))]])
            sql-format
            (->> (jdbc-query tx))
            first :exists)
    (throw (ex-info "Inventory pool cannot be deactivated because it still has unretired items."
                    {:status 422})))
  (when (-> (sql/select
             [[:exists
               (-> (sql/select 1)
                   (sql/from :reservations)
                   (sql/where [:= :inventory_pool_id inventory-pool-id])
                   (sql/where [:in :status ["submitted" "approved" "signed"]]))]])
            sql-format
            (->> (jdbc-query tx))
            first :exists)
    (throw (ex-info "Inventory pool cannot be deactivated because it still has active reservations."
                    {:status 422}))))

(comment
  (require '[leihs.core.db :as db])
  (let [tx (db/get-ds)
        inventory-pool-id #uuid "a7dc0cbd-9692-4fa7-bc9b-67ee6707aa24"]
    ; (-> (sql/select
    ;       [[:exists
    ;         (-> (sql/select 1) 
    ;             (sql/from :reservations)
    ;             (sql/where [:= :inventory_pool_id inventory-pool-id])
    ;             (sql/where [:in :status ["submitted" "approved" "signed"]]))]])
    ;     sql-format
    ;     (->> (jdbc-query tx))
    ;     first :exists)
    ; (-> (sql/select
    ;       [[:exists
    ;         (-> (sql/select 1) 
    ;             (sql/from :items)
    ;             (sql/where [:or [:= :inventory_pool_id inventory-pool-id]
    ;                         [:= :owner_id inventory-pool-id]])
    ;             (sql/where [:is-null :retired]))]])
    ;     sql-format
    ;     (->> (jdbc-query tx))
    ;     first :exists)
    (validate-deactivation tx inventory-pool-id)))

;; Trimmed and in lower case, nil when empty; 422 when it is not an address.
(defn- notification-email [value]
  (when-let [email (some-> value clojure.core/str str/trim str/lower-case not-empty)]
    (when-not (re-matches #"[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}" email)
      (throw (ex-info (clojure.core/str "The notification email must be an email address: " email)
                      {:status 422})))
    email))

(defn patch-inventory-pool
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx data :body :as request}]
  (when (->> ["SELECT true AS exists FROM inventory_pools WHERE id = ?" inventory-pool-id]
             (jdbc-query tx)
             first :exists)
    (let [patch-data (cond-> (select-keys data patch-fields)
                       (contains? data :notification_email)
                       (update :notification_email notification-email))]
      (when-not (:is_active patch-data)
        (validate-deactivation tx inventory-pool-id))
      (jdbc-update! tx :inventory_pools patch-data
                    ["id = ?" inventory-pool-id]))
    {:status 204}))

(defn delete-inventory-pool [{tx :tx {inventory-pool-id :inventory-pool-id} :route-params}]
  (assert inventory-pool-id)
  (if (= 1 (::jdbc/update-count
            (jdbc-delete! tx :inventory_pools ["id = ?" inventory-pool-id])))
    {:status 204}
    {:status 404 :body "Delete inventory-pool failed without error."}))

(def routes
  (fn [request]
    (case (:request-method request)
      :get (inventory-pool request)
      :delete (delete-inventory-pool request)
      :patch (patch-inventory-pool request))))
