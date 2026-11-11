(ns leihs.admin.resources.categories.category.main
  (:refer-clojure :exclude [get])
  (:require
   [better-cond.core :as b]
   [clojure.string :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.core.resources.categories.filter :refer [deep-filter]]
   [leihs.core.resources.categories.shared :refer [base-query sql-add-metadata]]
   [leihs.core.resources.categories.tree :refer [tree convert-tree-path roots]]
   [next.jdbc.sql :as jdbc]
   [taoensso.timbre :refer [debug spy warn]]))

;;; category ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query [id]
  (-> base-query
      (sql/where [:= :model_groups.id id])))

(defn merge-parents [tx category]
  (let [subtree (if (some #{(:category_id category)}
                          (map :category_id (roots tx)))
                  []
                  (deep-filter #(= (:category_id %) (:category_id category))
                               (tree tx
                                     :with-metadata true
                                     :exclude [:image_url :thumbnail_url])))]
    (assoc category :parents (map convert-tree-path subtree))))

(defn get-one [tx id]
  (-> id query
      (sql/select :model_groups.outlook_calendar)
      (sql-add-metadata :label nil)
      sql-format
      (->> (jdbc/query tx))
      first
      (->> (merge-parents tx))))

(comment
  (do (require '[leihs.core.db :as db])
      (let [tx (db/get-ds)
            id #uuid "47c5389d-f98d-5bf1-8c6e-8fec37d907a0"]
        (tree tx
              :with-metadata true
              :exclude [:image_url :thumbnail_url])
        #_(get-one (db/get-ds) id))))

(defn get
  [{tx :tx {id :category-id} :route-params}]
  {:body (get-one tx id)})

;;; delete category ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete
  [{tx :tx {id :category-id} :route-params}]
  (assert id)
  (if (= id (:id (jdbc/delete! tx :model_groups
                               ["type = 'Category' AND id = ?" id]
                               {:return-keys true})))
    {:status 204}
    {:status 404 :body "Deleting category failed without error."}))

;;; outlook calendar ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private email-pattern #"[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}")

(defn outlook-calendar
  "The mailbox whose calendar receives the reservations of the category, as
  stored: trimmed and in lower case, nil when empty. Rejects anything that is
  not an email address."
  [data]
  (when-let [mailbox (some-> (:outlook_calendar data) str str/trim str/lower-case presence)]
    (when-not (re-matches email-pattern mailbox)
      (throw (ex-info (str "The Outlook calendar must be an email address, like so@enti.cat: " mailbox)
                      {:status 422})))
    mailbox))

;;; update category ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- delete-image-with-thumb! [tx id]
  (jdbc/delete! tx :images ["target_id = ?" id]))

(defn add-thumb-postfix [filename]
  (let [[_ name ext] (re-matches #"(.+?)(\.[^.]+)$" filename)]
    (str name "_thumb" ext)))

;; MIME and filename normalization helpers
(def mime-by-ext
  {"jpg" "image/jpeg"
   "jpeg" "image/jpeg"
   "png" "image/png"
   "gif" "image/gif"
   "webp" "image/webp"
   "svg" "image/svg+xml"
   "bmp" "image/bmp"
   "tif" "image/tiff"
   "tiff" "image/tiff"})

(def preferred-ext-by-mime
  {"image/jpeg" "jpg"
   "image/png" "png"
   "image/gif" "gif"
   "image/webp" "webp"
   "image/svg+xml" "svg"
   "image/bmp" "bmp"
   "image/tiff" "tiff"})

(defn filename-parts [filename]
  (re-matches #"(.+?)(\.[^.]+)$" filename))

(defn filename-ext [filename]
  (some-> (filename-parts filename) (nth 2) (subs 1) str/lower-case))

(defn normalize-filename-ext [filename preferred-ext]
  (let [[_ base dot-ext] (filename-parts filename)
        ext (when dot-ext (subs dot-ext 1))]
    (cond
      (and preferred-ext base dot-ext (not= (str/lower-case ext) preferred-ext))
      (str base "." preferred-ext)
      (and preferred-ext base (nil? dot-ext))
      (str filename "." preferred-ext)
      :else filename)))

(defn data-url-mime [url]
  (when (and url (string? url) (str/starts-with? url "data:"))
    (let [meta (subs url 5)
          before-comma (first (str/split meta #"," 2))
          mime (first (str/split before-comma #";" 2))]
      mime)))

(defn normalize-image-filetype [img]
  (let [url (:url img)
        url-mime (data-url-mime url)
        provided-mime (:content_type img)
        filename (:filename img)
        ext (when filename (some-> (filename-ext filename) str/lower-case))
        expected-mime (or url-mime (and ext (mime-by-ext ext)) provided-mime "image/jpeg")
        preferred-ext (and expected-mime (preferred-ext-by-mime expected-mime))
        fixed-mime (or expected-mime provided-mime "image/jpeg")
        fixed-filename (cond
                         (and filename ext preferred-ext)
                         (normalize-filename-ext filename preferred-ext)
                         filename
                         filename
                         :else
                         nil)]
    (when (and url-mime provided-mime (not= url-mime provided-mime))
      (warn "MIME mismatch between image.url and content_type"
            {:url_mime url-mime :content_type provided-mime :filename filename}))
    (when (and preferred-ext ext (not= preferred-ext (str/lower-case ext)))
      (warn "Filename extension differs from MIME"
            {:mime expected-mime :filename filename
             :expected_ext preferred-ext :given_ext (str/lower-case ext)}))
    (-> img
        (assoc :content_type fixed-mime)
        (assoc :filename fixed-filename))))

(defn- insert-image-with-thumb! [tx id image thumbnail]
  (let [target-type "ModelGroup"
        image (normalize-image-filetype image)
        thumbnail (normalize-image-filetype thumbnail)
        image-row (jdbc/insert! tx :images
                                {:target_id id
                                 :target_type target-type
                                 :content (:data image)
                                 :content_type (:content_type image)
                                 :filename (:filename image)
                                 :width (:width image)
                                 :height (:height image)
                                 :thumbnail false})]
    (jdbc/insert! tx :images
                  {:target_id id
                   :target_type target-type
                   :content (:data thumbnail)
                   :content_type (:content_type thumbnail)
                   :filename (add-thumb-postfix (:filename thumbnail))
                   :width (:width thumbnail)
                   :height (:height thumbnail)
                   :parent_id (:id image-row)
                   :thumbnail true})))

(defn patch
  [{{id :category-id} :route-params tx :tx data :body :as request}]
  (if (-> (query id) sql-format (->> (jdbc/query tx)) first)
    (do (jdbc/update! tx :model_groups
                      (cond-> (select-keys data [:name])
                        (contains? data :outlook_calendar)
                        (assoc :outlook_calendar (outlook-calendar data)))
                      ["type = 'Category' AND id = ?" id])

        (let [target-type "ModelGroup"
              image-content (:image data)
              thumb-content (:thumbnail data)]
          (cond
            ; image removed in FE
            (and (not (:thumbnail data))
                 (not (-> data :image :url)))
            (delete-image-with-thumb! tx id)
            ; image changed in FE
            (and (-> data :image :data) (-> data :thumbnail :data))
            (do (delete-image-with-thumb! tx id)
                (insert-image-with-thumb! tx id (:image data) (:thumbnail data)))
            ; image unchanged in FE
            :else (debug "image unchanged")))

        (jdbc/delete! tx :model_group_links ["child_id = ?" id])
        (doseq [parent (:parents data)]
          (jdbc/insert! tx :model_group_links {:child_id id,
                                               :parent_id (:id parent),
                                               :label (presence (:label parent))}))
        {:status 200, :body {}})
    {:status 404}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get request)
    :patch (patch request)
    :delete (delete request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
