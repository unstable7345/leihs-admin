(ns leihs.admin.resources.categories.category.core
  (:require
   ["/admin-ui" :as UI]
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :as str]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.categories.category.image :as image]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.resources.categories.filter :as tree-filter]
   [leihs.core.resources.categories.tree :as tree-path]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form InputGroup Row Col Button Container Card]]
   [reagent.core :as reagent :refer [reaction]]))

;;; atoms ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce id*
  (reaction (or (-> @routing/state* :route-params :category-id presence)
                ":category-id")))

(defonce cache* (reagent/atom nil))

(defonce path*
  (reaction (path :category {:category-id @id*})))

(defonce data*
  (reaction (get @cache* @path*)))

(defonce data-models* (reagent/atom nil))

(defonce categories-cache* (reagent/atom nil))

(defonce categories-data*
  (reaction (get @categories-cache* "/admin/categories/")))

;;; fetch data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch []
  (http-client/route-cached-fetch cache* {:route @path*}))

(defn fetch-models []
  (go (reset! data-models*
              (some-> {:chan (async/chan)
                       :url (path :category-models {:category-id @id*})}
                      http-client/request
                      :chan <!
                      http-client/filter-success!
                      :body :models))))

;;; helper ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn resolve-parent-paths [category_id]
  (let [tree @categories-data*
        parent-paths (tree-filter/deep-filter #(= (:category_id %)
                                                  category_id)
                                              (:children tree))]
    (mapv tree-path/convert-tree-path parent-paths)))

(defn parents-labels [parents]
  (for [parent parents]
    (map-indexed (fn [idx el]
                   {:index idx
                    :name (:name el)
                    :label (-> el :metadata :label)})
                 parent)))

(defn parent-paths-list [parents & {:keys [unstyled]
                                    :or {unstyled false}}]
  [:ul {:class-name (when unstyled "list-unstyled")}
   (for [path (parents-labels parents)]
     ^{:key (str path)}
     [:li
      (for [parent path]
        ^{:key (:index parent)}
        [:span
         (when (> (:index parent) 0) " <- ")

         (if (:label parent)

           [:span (:name parent) " "
            [:span {:class-name "badge badge-info"} (:label parent)]]
           (:name parent))])])])

;;; form ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parent-category [form-data* parent index]

  [:> Card {:class-name "mb-3"}
   [:> Container {:class-name "p-2"}
    [:button {:type "button"
              :class-name "close"
              :style {:z-index "9999"
                      :position "absolute"
                      :right "0.5rem"}
              :on-click #(swap! form-data*
                                update :parents
                                (fn [parents]
                                  (remove (fn [p] (= p (nth parents index))) parents)))}
     [icons/delete]]

    [:> Row
     [:> Col (if (-> parent :thumbnail_url)
               [:img  {:style {:object-fit "contain"
                               :aspect-ratio "1/1"
                               :display "flex"}
                       :src (-> parent :thumbnail_url)}]

               [:div {:style {:font-size "0.5rem"
                              :aspect-ratio "1/1"
                              :display "flex"
                              :align-items "center"
                              :justify-content "center"}
                      :class-name "border rounded"} [:span "no picture"]])]
     [:> Col {:sm 10}
      [parent-paths-list (resolve-parent-paths (:id parent)) {:unstyled true}]
      [:> Form.Group

       [:input.form-control
        {:id "label"
         :type "text"
         :placeholder "Enter Label"
         :value (or (-> parent :label) "")
         :onChange #(swap! form-data*
                           assoc-in [:parents index :label]
                           (.. % -target -value))}]]]]]])

(defonce show-category-tree* (reagent/atom false))

(defn form [action form-data*]
  [:> Form {:id "category-form"
            :on-submit (fn [e] (.preventDefault e) (action))}
   [:> Row
    [:> Col {:sm 4}
     [:> Form.Group {:control-id "name"}

      [:> Form.Label {:class-name "h5"} "Name"]
      [:input.form-control
       {:id "name"
        :type "text"
        :required true
        :placeholder "Enter Name"
        :value (or (:name @form-data*) "")
        :onChange (fn [e] (swap! form-data* assoc :name (-> e .-target .-value)))}]]

     [:> Form.Group {:control-id "outlook_calendar"}
      [:> Form.Label {:class-name "h5"} "Outlook calendar"]
      [:input.form-control
       {:id "outlook_calendar"
        :type "email"
        :placeholder "so@enti.cat"
        :value (or (:outlook_calendar @form-data*) "")
        :onChange (fn [e] (swap! form-data* assoc :outlook_calendar (-> e .-target .-value)))}]
      [:> Form.Text {:className "text-muted"}
       "Approved reservations of the models in this category and its subcategories "
       "are copied as events to the calendar of this mailbox. Leave it empty to not copy them."]]

     [:> Form.Group {:control-id "description"}
      [:> Form.Label {:class-name "h5"} "Image"]
      [image/component form-data*]]]

    [:> Col
     (when (seq (:parents @form-data*))
       [:div {:class-name "mb-3"}
        [:> Form.Label {:class-name "h5 mb-0"} "Parent assignments"]
        [:> Form.Text  "Selected Parent Categories"]])

     (for [[index parent] (map-indexed vector (:parents @form-data*))]
       ^{:key index}
       [parent-category form-data* parent index])

     (if (not @show-category-tree*)
       [:> Button {:on-click #(reset! show-category-tree* true)} "Add parent category"]
       [:> UI/Components.TreeView
        {:data (clj->js @categories-data*)
         :onSelected #(do (reset! show-category-tree* false)
                          (swap! form-data*
                                 update :parents
                                 (fn [parents]
                                   (vec (conj parents
                                              {:id  (.. % -metadata -id)
                                               :name  (.. % -metadata -name)
                                               :label ""
                                               :thumbnail_url  (.. ^js % -metadata -thumbnail_url)})))))}])]]])

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.container.text-break
     [:div.category-debug
      [:hr]
      [:div.category-data
       [:h2 "@data*"]
       [:pre (with-out-str (pprint @data*))]]]
     [:div.category-debug
      [:hr]
      [:div.category-data
       [:h2 "@category-models-data*"]
       [:pre (with-out-str (pprint @data-models*))]]]]))
