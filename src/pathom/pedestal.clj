(ns pathom.pedestal
 (:require
   [clojure.zip :as zip]
   [clojure.java.io :as io]

   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.http.secure-headers :as secure-headers]

   [ring.util.response :as ring-resp]

   [hickory.core :as h]
   [hickory.select :as s]
   [hickory.zip :refer [hickory-zip]]
   [hickory.render :refer [hickory-to-html]])

 (:gen-class))


(def ^:private default-path "/oge")

(def ^:private default-asset-path "/assets/oge")


(defn or-selector
  "Takes any number of selectors and returns a selector that returns the first
   selector that is satisfied by the zip-loc given"
  [selectors]
  (fn [zip-loc]
    (loop [xs (seq selectors)]
      (if-let [sel (first xs)]
        (if (sel zip-loc)
          sel
          (recur (next xs)))))))


(defn render-template
  "Takes a zip location (usually the root) and a map of selectors to functions.
   For every matching selector, applies the function.
   Returns the zip root, once reaches the end"
  [hzip-loc forms]
  (let [selectors (keys forms)
        selector-fn (or-selector selectors)]
    (loop [loc hzip-loc]
      (if (zip/end? loc)
        (zip/root loc)
        (if-let [sel (selector-fn loc)]
          (let [f (get forms sel)]
            (recur (zip/next (f loc))))
          (recur (zip/next loc)))))))


(defn- prepend-attr
  [m k x]
  (update-in m [:attrs k] #(str x "/" %)))

(defn oge-ide-response
  [options]
  (let [stylesheet-selector(s/and (s/tag :link) (s/attr :rel #(= % "stylesheet")))

        forms {stylesheet-selector #(zip/edit % prepend-attr :href default-asset-path)
               (s/tag :script) #(zip/edit % prepend-attr :src default-asset-path)}

        index (-> "oge/index.html"
                  io/resource
                  slurp
                  h/parse
                  h/as-hickory
                  hickory-zip
                  (render-template forms)
                  hickory-to-html)]
    {:status 200
     :body index
     :headers {"Content-Type" "text/html"}}))


(defn- pathom-response [parser]
  (fn
    [{:keys [transit-params]}]
    {:status 200
     :body (parser {} transit-params)}))

(defn- custom-secure-headers []
  (secure-headers/secure-headers
    {:content-security-policy-settings "default-src * 'unsafe-inline'"}))

(defn default-routes [options]
  (let [{:keys [path parser]
         :or {path default-path}}
        options]
    #{[path :post [(body-params/body-params) http/transit-body (pathom-response parser)] :route-name ::graph-api]}))


(defn pathom-routes [options]
  (let [{:keys [path asset-path oge?]
         :or {path default-path
              asset-path default-asset-path}}
        options
        base-routes (default-routes options)]
    (if-not oge?
      base-routes
      (let [asset-path' (str asset-path "/*path")

            index-handler (let [index-response (oge-ide-response options)]
                            (fn [request]
                              index-response))

            asset-get-handler (fn [request]
                                (ring-resp/resource-response (-> request :path-params :path)
                                                            {:root "oge"}))
            asset-head-handler #(-> %
                                    asset-get-handler
                                    (assoc :body nil))]
        (conj base-routes
          [path :get [(custom-secure-headers) index-handler] :route-name ::graphiql-ide-index]
          [asset-path' :get asset-get-handler :route-name ::pathom-get-assets]
          [asset-path' :head asset-head-handler :route-name ::pathom-head-assets])))))


(comment

  ; TODO maybe use a vector of vectors, with [MATCHER FUNCTION NEXT-FN] where NEXT-FN is optional, (zip/next by default)

  (let [data (-> "oge/index.html"
                 io/resource
                 slurp
                 h/parse
                 h/as-hickory
                 hickory-zip)

        forms {(s/and (s/tag :link) (s/attr :rel #(= % "stylesheet"))) #(zip/edit % prepend-attr :href "PREPEND")
               (s/tag :div) identity}]
    (render-template data forms)))
