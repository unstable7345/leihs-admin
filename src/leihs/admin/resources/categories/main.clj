(ns leihs.admin.resources.categories.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.categories.category.main :as category]
   [leihs.core.core :refer [presence]]
   [leihs.core.db :as db]
   [leihs.core.resources.categories.filter :as filter]
   [leihs.core.resources.categories.tree :refer [tree]]
   [next.jdbc.sql :refer [insert! query]
    :rename {query jdbc-query, insert! jdbc-insert!}]
   [taoensso.timbre :refer [debug spy]]))

(defn term-filter [tree request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (filter/deep-filter #(re-matches (re-pattern (str "(?i).*" term ".*"))
                                     (:name %))
                        tree)
    tree))

(defn index [{tx :tx :as request}]
  {:body {:name "categories"
          :children (-> (tree tx {:with-metadata true})
                        (term-filter request))}})

(comment
  (require '[clojure.inspector :as inspector])
  (require '[leihs.core.db :as db])
  (let [tx (db/get-ds)]
    (tree tx)
    #_(deep-filter #(re-matches #"(?i).*audio.*" (:name %))
                   (tree tx))))

;;; create category ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [{tx :tx data :body :as request}]
  (if-let [category (jdbc-insert! tx
                                  :model_groups
                                  (-> data
                                      (select-keys [:name])
                                      (assoc :type "Category"
                                             :outlook_calendar (category/outlook-calendar data))))]
    (let [id (:id category)]
      (when-let [image (not-empty (:image data))]
        (let [target-type "ModelGroup"
              image* (category/normalize-image-filetype image)
              thumb* (category/normalize-image-filetype (:thumbnail data))
              parent-filename (:filename image*)
              thumb-filename (some-> (:filename thumb*) category/add-thumb-postfix)
              image-row (jdbc-insert! tx :images
                                      {:target_id id
                                       :target_type target-type
                                       :content (:data image*)
                                       :content_type (:content_type image*)
                                       :filename parent-filename
                                       :width (:width image*)
                                       :height (:height image*)
                                       :thumbnail false})
              thumbnail (:thumbnail data)]
          (jdbc-insert! tx :images
                        {:target_id id
                         :target_type target-type
                         :content (:data thumb*)
                         :content_type (:content_type thumb*)
                         :filename thumb-filename
                         :width (:width thumb*)
                         :height (:height thumb*)
                         :parent_id (:id image-row)
                         :thumbnail true})))

      (doseq [parent (:parents data)]
        (jdbc-insert! tx :model_group_links
                      {:child_id id, :parent_id (:id parent),
                       :label (presence (:label parent))}))

      {:status 201, :body category})
    {:status 422
     :body "No category has been created."}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (index request)
    :post (create request)))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'groups-formated-query)
