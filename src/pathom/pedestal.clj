(ns pathom.pedestal
 (:require
   [clojure.java.io :as io]

   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.http.secure-headers :as secure-headers]

   [ring.util.response :as ring-resp]

   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]
   [com.wsscode.pathom.profile :as pp]

   [pathom.pedestal.jsoup :refer [attr-update]])

 (:import
   [org.jsoup Jsoup])
 (:gen-class))


(def ^:private default-path "/oge")

(def ^:private default-asset-path "/assets/oge")


(defn index-html
  "Returns index.html, updating urls to the linked resources"
  []
  (let [doc (-> "oge/index.html" io/resource slurp Jsoup/parse)
        prepend #(str default-asset-path "/" %)]
    (-> doc
      (.select "link[type=\"text/css\"]")
      (attr-update "href" prepend))
    (-> doc
      (.select "script")
      (attr-update "src" prepend))

    {:status 200
     :body (.html doc)
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
  (let [{:keys [path parser extra-interceptors]
         :or {path default-path}}
        options
        default-interceptors [(body-params/body-params) http/transit-body]
        handler (pathom-response parser)
        interceptors (-> default-interceptors
                         (concat extra-interceptors)
                         (as-> <> (into [] <>))
                         (conj handler))]
    #{[path :post interceptors :route-name ::graph-api]}))


(defn pathom-routes [options]
  (let [{:keys [path asset-path oge?]
         :or {path default-path
              asset-path default-asset-path}}
        options
        base-routes (default-routes options)]
    (if-not oge?
      base-routes
      (let [asset-path' (str asset-path "/*path")

            index-handler (let [index-response (index-html)]
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


(defn make-parser
  "Helper to create a pathom parser.
   extra-env can be used to inject dependencies required by the queries (like
   a database connection)"
  [app-registry & {:keys [extra-env]}]
  (p/parser {::p/env     (merge
                           extra-env
                           {::p/reader               [p/map-reader
                                                      pc/reader2
                                                      pc/ident-reader
                                                      p/env-placeholder-reader]
                            ::pc/resolver-dispatch   pc/resolver-dispatch-embedded
                            ::pc/mutate-dispatch     pc/mutation-dispatch-embedded
                            ::p/placeholder-prefixes #{">"}
                            ::db                     (atom {})})
             ::p/mutate  pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register app-registry})
                          p/error-handler-plugin
                          p/request-cache-plugin
                          pp/profile-plugin]}))
