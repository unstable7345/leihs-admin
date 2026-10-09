(ns leihs.admin.resources.inventory-pools.inventory-pool.time-slots.core
  (:require
   [cljs.core.async :as async :refer [go <!]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data* (some->
                     {:chan (async/chan)
                      :url (path :inventory-pool-time-slots
                                 (-> @routing/state* :route-params))}
                     http-client/request :chan <!
                     http-client/filter-success! :body))))
