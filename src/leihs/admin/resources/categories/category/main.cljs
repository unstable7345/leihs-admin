(ns leihs.admin.resources.categories.category.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]

   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.categories.category.core :as core]
   [leihs.admin.resources.categories.category.delete :as delete]
   [leihs.admin.resources.categories.category.edit :as edit]
   [leihs.admin.resources.categories.category.models.main :as models-main]
   [leihs.admin.resources.categories.main :as categories-main]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn fetch-categories []
  (http-client/route-cached-fetch
   core/categories-cache* {:route (path :categories)
                           :reload true}))

(defn info-table []
  [:<>
   [table/container
    {:borders false
     :header [:tr [:th "Property"] [:th.w-75 "Value"]]
     :body
     [:<>
      [:tr.name
       [:td [:strong "Name"] [:small " (name)"]]
       [:td (str (:name @core/data*))]]
      [:tr.outlook-calendar
       [:td [:strong "Outlook calendar"] [:small " (outlook_calendar)"]]
       [:td (or (:outlook_calendar @core/data*) "—")]]
      [:tr.parents
       [:td [:strong "Parents"] [:small " (metadata:parents)"]]
       [:td [core/parent-paths-list (-> @core/data* :parents)]]]

      [:tr.image
       [:td [:strong "Image"] [:small " (metadata:image_url)"]]
       [:td [:img {:src (-> @core/data* :metadata :image_url)}]]]]}]])

(defn header []
  [:header.my-5
   [breadcrumbs/main  {:to (path :categories)}]
   [:h1.mt-3 (-> @core/data* :name)]
   (let [label (-> @core/data* :metadata :label)]
     (when label [:p "( " label " )"]))])

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount (fn []
                  (when (empty? @core/categories-data*) fetch-categories)
                  (core/fetch-models)
                  (core/fetch))}]

   (if-not @core/data*
     [:div.my-5
      [wait-component]]

     [:article.category
      [header]
      [:section
       [info-table]
       [edit/button]
       [edit/dialog]
       (when (-> @core/data* :metadata :is_deletable)
         [delete/button])
       [delete/dialog]
       [:div.mt-5.mb-5
        [models-main/models-in-category-table]]
       [core/debug-component]]])])

