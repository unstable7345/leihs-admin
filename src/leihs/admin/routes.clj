(ns leihs.admin.routes
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set]
   [leihs.admin.html :as html]
   [leihs.admin.paths :refer [path paths]]
   [leihs.admin.resources.audits.changes.change.main :as audited-change]
   [leihs.admin.resources.audits.changes.main :as audited-changes]
   [leihs.admin.resources.audits.requests.main :as audited-requests]
   [leihs.admin.resources.audits.requests.request.main :as audited-request]
   [leihs.admin.resources.buildings.building.main :as building]
   [leihs.admin.resources.buildings.main :as buildings]
   [leihs.admin.resources.categories.category.main :as category]
   [leihs.admin.resources.categories.category.models.main :as category-models]
   [leihs.admin.resources.categories.main :as categories]
   [leihs.admin.resources.groups.group.main :as group]
   [leihs.admin.resources.groups.group.users.main :as group-users]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.initial-admin.back :as initial-admin]
   [leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.main :as inventory-field-inventory-pools]
   [leihs.admin.resources.inventory-fields.inventory-field.main :as inventory-field]
   [leihs.admin.resources.inventory-fields.main :as inventory-fields]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.groups.main :as pool-delegation-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.main :as pool-delegation]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.suspension.main :as inventory-pool-delegation-suspension]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.users.main :as pool-delegation-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.main :as pool-delegations]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main :as entitlement-group]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main :as entitlement-group-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main :as entitlement-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement_groups.entitlement_group.groups.main :as entitlement-group-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.fields.main :as inventory-pool-fields]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.main :as inventory-pool-group-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.main :as inventory-pool-groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.holidays.main :as inventory-pool-holidays]
   [leihs.admin.resources.inventory-pools.inventory-pool.mail-templates.mail-template.main :as inventory-pool-mail-template]
   [leihs.admin.resources.inventory-pools.inventory-pool.mail-templates.main :as inventory-pool-mail-templates]
   [leihs.admin.resources.inventory-pools.inventory-pool.main :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.hourly-reservations.main :as inventory-pool-hourly-reservations]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as inventory-pool-users]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main :as inventory-pool-user-direct-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main :as inventory-pool-user-groups-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.main :as inventory-pool-user]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main :as inventory-pool-user-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main :as inventory-pool-user-suspension]
   [leihs.admin.resources.inventory-pools.inventory-pool.workdays.main :as inventory-pool-workdays]
   [leihs.admin.resources.inventory-pools.main :as inventory-pools]
   [leihs.admin.resources.mail-templates.mail-template.main :as mail-template]
   [leihs.admin.resources.mail-templates.main :as mail-templates]
   [leihs.admin.resources.rooms.main :as rooms]
   [leihs.admin.resources.rooms.room.main :as room]
   [leihs.admin.resources.settings.languages.main :as languages-settings]
   [leihs.admin.resources.settings.misc.main :as misc-settings]
   [leihs.admin.resources.settings.smtp.main :as smtp-settings]
   [leihs.admin.resources.settings.smtp.ms365 :as smtp-ms365]
   [leihs.admin.resources.settings.smtp.ms365-mailboxes.main :as ms365-mailboxes]
   [leihs.admin.resources.settings.smtp.ms365-mailboxes.ms365-mailbox.main :as ms365-mailbox]
   [leihs.admin.resources.settings.syssec.main :as syssec-settings]
   [leihs.admin.resources.statistics.contracts :as statistics-contracts]
   [leihs.admin.resources.statistics.items :as statistics-items]
   [leihs.admin.resources.statistics.models :as statistics-models]
   [leihs.admin.resources.statistics.pools :as statistics-pools]
   [leihs.admin.resources.statistics.users :as statistics-users]
   [leihs.admin.resources.suppliers.main :as suppliers]
   [leihs.admin.resources.suppliers.supplier.items :as supplier-items]
   [leihs.admin.resources.suppliers.supplier.main :as supplier]
   [leihs.admin.resources.system.authentication-systems.authentication-system.groups.main :as authentication-system-groups]
   [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
   [leihs.admin.resources.system.authentication-systems.authentication-system.users.main :as authentication-system-users]
   [leihs.admin.resources.system.authentication-systems.main :as authentication-systems]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.resources.users.user.api-tokens.api-token.main :as user-api-token]
   [leihs.admin.resources.users.user.api-tokens.main :as user-api-tokens]
   [leihs.admin.resources.users.user.inventory-pools :as user-inventory-pools]
   [leihs.admin.resources.users.user.main :as user]
   [leihs.admin.resources.users.user.password-reset.main :as user-password-reset]
   [leihs.admin.utils.params :as params]
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword presence]]
   [leihs.core.db :as db]
   [leihs.core.http-cache-buster2 :as cache-buster :refer [wrap-resource]]
   [leihs.core.json :as json]
   [leihs.core.json-protocol]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.ring-exception :as ring-exception]
   [leihs.core.routes :as core-routes :refer [all-granted]]
   [leihs.core.routing.back :as routing]
   [leihs.core.routing.dispatch-content-type :as dispatch-content-type]
   [leihs.core.settings :as settings]
   [leihs.core.status :as status]
   [logbug.debug :refer [I>]]
   [logbug.ring :refer [wrap-handler-with-logging]]
   [ring.middleware.accept]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies]
   [ring.middleware.json]
   [ring.middleware.params]
   [ring.util.response :refer [redirect]]))

(declare redirect-to-root-handler)

(def no-spa-handler-keys
  (clojure.set/union
   core-routes/no-spa-handler-keys
   #{:redirect-to-root
     :not-found
     :smtp-ms365-callback}))

(defn own-user-admin-scopes? [request]
  (and (= (-> request :authenticated-entity :id)
          (-> request :route-params :user-id))
       (auth/admin-scopes? request)))

(def resolve-table
  (merge core-routes/resolve-table
         {:audited-changes {:handler audited-changes/routes
                            :authorizers [auth/system-admin-scopes?]}

          :audited-changes-meta {:handler audited-changes/routes
                                 :authorizers [auth/system-admin-scopes?]}

          :audited-change {:handler audited-change/routes
                           :authorizers [auth/system-admin-scopes?]}

          :audited-requests {:handler audited-requests/routes
                             :authorizers [auth/system-admin-scopes?]}

          :audited-request {:handler audited-request/routes
                            :authorizers [auth/system-admin-scopes?]}

          :authentication-system {:handler authentication-system/routes
                                  :authorizers [auth/system-admin-scopes?]}
          :authentication-system-group {:handler authentication-system-groups/routes
                                        :authorizers [auth/admin-scopes?]}
          :authentication-system-groups {:handler authentication-system-groups/routes
                                         :authorizers [auth/admin-scopes?]}
          :authentication-system-user {:handler authentication-system-users/routes
                                       :authorizers [auth/admin-scopes?]}
          :authentication-system-users {:handler authentication-system-users/routes
                                        :authorizers [auth/admin-scopes?]}
          :authentication-systems {:handler authentication-systems/routes
                                   :authorizers [auth/system-admin-scopes?]}
          :building {:handler building/routes :authorizers [auth/admin-scopes?]}
          :buildings {:handler buildings/routes :authorizers [auth/admin-scopes?]}
          :category {:handler category/routes :authorizers [auth/admin-scopes?]}
          :category-models {:handler category-models/routes :authorizers [auth/admin-scopes?]}
          :categories {:handler categories/routes :authorizers [auth/admin-scopes?]}
          :inventory-field {:handler inventory-field/routes :authorizers [auth/admin-scopes?]}
          :inventory-fields {:handler inventory-fields/routes :authorizers [auth/admin-scopes?]}
          :inventory-fields-groups {:handler inventory-fields/groups-route :authorizers [auth/admin-scopes?]}
          :inventory-field-inventory-pools {:handler inventory-field-inventory-pools/routes :authorizers [auth/admin-scopes?]}
          :group {:handler group/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :group-inventory-pools-roles {:handler group/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :group-user {:handler group-users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :group-users {:handler group-users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :groups {:handler groups/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :initial-admin {:handler initial-admin/routes :authorizers [all-granted]}
          :inventory-pool {:handler inventory-pool/routes
                           :authorizers [auth/admin-scopes?
                                         pool-auth/pool-inventory-manager?
                                         pool-auth/pool-lending-manager-and-http-safe?]}
          :inventory-pool-delegation {:handler pool-delegation/routes
                                      :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-group {:handler pool-delegation-groups/routes
                                            :authorizers [auth/admin-scopes?
                                                          pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-groups {:handler pool-delegation-groups/routes
                                             :authorizers [auth/admin-scopes?
                                                           pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-suspension {:handler inventory-pool-delegation-suspension/routes
                                                 :authorizers [auth/admin-scopes?
                                                               pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-user {:handler pool-delegation-users/routes
                                           :authorizers [auth/admin-scopes?
                                                         pool-auth/pool-lending-manager?]}
          :inventory-pool-delegation-users {:handler pool-delegation-users/routes
                                            :authorizers [auth/admin-scopes?
                                                          pool-auth/pool-lending-manager?]}
          :inventory-pool-delegations {:handler pool-delegations/routes
                                       :authorizers [auth/admin-scopes?
                                                     pool-auth/pool-lending-manager?]}

          :inventory-pool-entitlement-group {:handler entitlement-group/routes
                                             :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-direct-user {:handler entitlement-group-users/routes
                                                         :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-group {:handler entitlement-group-groups/routes
                                                   :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-groups {:handler entitlement-group-groups/routes
                                                    :authorizers [auth/admin-scopes?  pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-group-users {:handler entitlement-group-users/routes
                                                   :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-groups {:handler entitlement-groups/routes
                                              :authorizers [auth/admin-scopes?  pool-auth/pool-lending-manager?]}
          :inventory-pool-entitlement-groups-group {:handler entitlement-groups/routes
                                                    :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}

          :inventory-pool-fields {:handler inventory-pool-fields/routes
                                  :authorizers [auth/admin-scopes?
                                                pool-auth/pool-inventory-manager?
                                                pool-auth/pool-lending-manager-and-http-safe?]}
          :inventory-pool-group-roles {:handler inventory-pool-group-roles/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-groups {:handler inventory-pool-groups/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-holidays {:handler inventory-pool-holidays/routes
                                    :authorizers [auth/admin-scopes?
                                                  pool-auth/pool-inventory-manager?
                                                  pool-auth/pool-lending-manager-and-http-safe?]}
          :inventory-pool-hourly-reservations {:handler inventory-pool-hourly-reservations/routes
                                               :authorizers [auth/admin-scopes?
                                                             pool-auth/pool-inventory-manager?
                                                             pool-auth/pool-lending-manager-and-http-safe?]}
          :inventory-pool-mail-template {:handler inventory-pool-mail-template/routes
                                         :authorizers [auth/admin-scopes?, pool-auth/pool-inventory-manager?]}
          :inventory-pool-mail-templates {:handler inventory-pool-mail-templates/routes
                                          :authorizers [auth/admin-scopes?, pool-auth/pool-inventory-manager?]}
          :inventory-pool-user-direct-roles {:handler inventory-pool-user-direct-roles/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user-groups-roles {:handler inventory-pool-user-groups-roles/groups-roles :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user-roles {:handler inventory-pool-user-roles/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user {:handler inventory-pool-user/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-user-suspension {:handler inventory-pool-user-suspension/routes :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-users {:handler inventory-pool-users/users :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]}
          :inventory-pool-workdays {:handler inventory-pool-workdays/routes
                                    :authorizers [auth/admin-scopes?
                                                  pool-auth/pool-inventory-manager?
                                                  pool-auth/pool-lending-manager-and-http-safe?]}
          :inventory-pools {:handler inventory-pools/routes
                            :authorizers [auth/admin-scopes?
                                          pool-auth/some-lending-manager-and-http-safe?]}
          :not-found {:handler html/not-found-handler :authorizers [all-granted]}
          :redirect-to-root {:handler redirect-to-root-handler :authorizers [all-granted]}

          :mail-template {:handler mail-template/routes :authorizers [auth/admin-scopes?]}
          :mail-templates {:handler mail-templates/routes :authorizers [auth/admin-scopes?]}
          :room {:handler room/routes :authorizers [auth/admin-scopes?]}
          :rooms {:handler rooms/routes :authorizers [auth/admin-scopes?]}
          :statistics-contracts {:handler statistics-contracts/routes :authorizers [auth/admin-scopes?]}
          :statistics-items {:handler statistics-items/routes :authorizers [auth/admin-scopes?]}
          :statistics-models {:handler statistics-models/routes :authorizers [auth/admin-scopes?]}
          :statistics-pools {:handler statistics-pools/routes :authorizers [auth/admin-scopes?]}
          :statistics-users {:handler statistics-users/routes :authorizers [auth/admin-scopes?]}

          :supplier {:handler supplier/routes :authorizers [auth/admin-scopes?]}
          :supplier-items {:handler supplier-items/routes :authorizers [auth/admin-scopes?]}
          :suppliers {:handler suppliers/routes :authorizers [auth/admin-scopes?]}

          :languages-settings {:handler languages-settings/routes
                               :authorizers [auth/admin-scopes? pool-auth/some-inventory-manager-and-http-safe?]}
          :misc-settings {:handler misc-settings/routes
                          :authorizers [auth/admin-scopes?]}
          :smtp-settings {:handler smtp-settings/routes
                          :authorizers [auth/system-admin-scopes?]}
          :smtp-emails {:handler smtp-settings/get-emails
                        :authorizers [auth/system-admin-scopes?]}
          :smtp-test-email {:handler smtp-settings/send-test-email
                            :authorizers [auth/system-admin-scopes?]}
          :smtp-ms365-callback {:handler smtp-ms365/callback
                                :authorizers [auth/system-admin-scopes?]}
          :smtp-ms365-mailboxes {:handler ms365-mailboxes/routes
                                 :authorizers [auth/system-admin-scopes?]}
          :smtp-ms365-mailbox {:handler ms365-mailbox/routes
                               :authorizers [auth/system-admin-scopes?]}
          :syssec-settings {:handler syssec-settings/routes
                            :authorizers [auth/system-admin-scopes?]}

          :user {:handler user/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :user-api-token {:handler user-api-token/routes :authorizers [auth/system-admin-scopes? own-user-admin-scopes?]}
          :user-api-tokens {:handler user-api-tokens/routes :authorizers [auth/system-admin-scopes? own-user-admin-scopes?]}
          :user-password-reset {:handler user-password-reset/routes
                                :authorizers [auth/admin-hierarchy?]}
          :user-inventory-pools {:handler user-inventory-pools/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :user-transfer-data {:handler user/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}
          :users {:handler users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}

          :users-choose {:handler users/routes :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn redirect-to-root-handler [request]
  (redirect (path :root)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn browser-request-matches-javascript? [request]
  "Returns true if the accepted type is javascript or
  if the :uri ends with .js. Note that browsers do not
  use the proper accept type for javascript script tags."
  (boolean (or (= (-> request :accept :mime) :javascript)
               (re-find #".+\.js$" (or (-> request :uri presence) "")))))

(defn wrap-dispatch-content-type
  ([handler]
   (fn [request]
     (wrap-dispatch-content-type handler request)))
  ([handler request]
   (cond
     ; accept json always goes to the backend handlers, i.e. the normal routing
     (= (-> request :accept :mime) :json) (or (handler request)
                                              (throw (ex-info "This resource does not provide a json response."
                                                              {:status 406})))
     ; accept HTML and GET (or HEAD) wants allmost always the frontend
     (and (= (-> request :accept :mime) :html)
          (#{:get :head} (:request-method request))
          (not (no-spa-handler-keys (:handler-key request)))
          (not (browser-request-matches-javascript? request))) (html/html-handler request)
     ; other request might need to go the backend and return frontend nevertheless
     :else (let [response (handler request)]
             (if (and (nil? response)
                      ; TODO we might not need the following after we check (?nil response)
                      (not (no-spa-handler-keys (:handler-key request)))
                      (not (#{:post :put :patch :delete} (:request-method request)))
                      (= (-> request :accept :mime) :html)
                      (not (browser-request-matches-javascript? request)))
               (html/html-handler request)
               response)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canonicalize-params-map [params & {:keys [parse-json?]
                                         :or {parse-json? true}}]
  (if-not (map? params)
    params
    (->> params
         (map (fn [[k v]]
                [(keyword k)
                 (if parse-json? (json/try-parse-json v) v)]))
         (into {}))))

(defn wrap-empty [handler]
  (fn [request]
    (or (handler request)
        {:status 404})))

(defn init [options]
  (routing/init paths resolve-table)
  (-> ; I> wrap-handler-with-logging
   routing/dispatch-to-handler
   (auth/wrap resolve-table)
   params/wrap-decode-and-cast-to-uuids
   wrap-dispatch-content-type
   ring-audits/wrap
   anti-csrf/wrap
   (auth/wrap-authenticate options)
   ring.middleware.cookies/wrap-cookies
   wrap-empty
   settings/wrap
   db/wrap-tx
   (status/wrap (path :status))
   ring.middleware.json/wrap-json-response
   (ring.middleware.json/wrap-json-body {:keywords? true})
   dispatch-content-type/wrap-accept
   routing/wrap-add-vary-header
   routing/wrap-resolve-handler
   routing/wrap-canonicalize-params-maps
   ring.middleware.params/wrap-params
   wrap-content-type
   (wrap-resource
    "public" {:allow-symlinks? true
              :cache-bust-paths ["/admin/ui/admin-ui.css"
                                 "/admin/ui/admin-ui.min.css"
                                 "/admin/js/main.js"]
              :never-expire-paths [#".*fontawesome-[^\/]*\d+\.\d+\.\d+\/.*"
                                   #".+_[0-9a-f]{40}\..+"]
              :enabled? true})
   ring-exception/wrap))

;#### debug ###################################################################

;(debug/debug-ns *ns*)
