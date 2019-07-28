(ns pathom.pedestal
 (:require
   [clojure.java.io :as io]

   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.http.secure-headers :as secure-headers]

   [ring.util.response :as ring-resp]

   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]

   [pathom.pedestal.jsoup :refer [attr-update relative-url?]])

 (:import
   [org.jsoup Jsoup])
 (:gen-class))


(def ^:private default-pathom-url "/pathom")

(def ^:private default-asset-path "/assets/pathom-viz")


(defn index-html
  "Returns index.html, updating urls to the linked resources"
  []
  (let [doc (-> "pathom-viz/index.html" io/resource slurp Jsoup/parse)
        prepend #(str default-asset-path "/" %)
        prepend-relative #(cond-> % (relative-url? %) prepend)]

    (-> doc
      (.select "link[rel=\"stylesheet\"]")
      (attr-update "href" prepend-relative))
    (-> doc
      (.select "script")
      (attr-update "src" prepend-relative))

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
  (let [{:keys [pathom-url parser interceptors]
         :or {pathom-url default-pathom-url}}
        options
        default-interceptors [(body-params/body-params) http/transit-body]
        handler (pathom-response parser)
        inter (-> default-interceptors
                  (concat interceptors)
                  (as-> <> (into [] <>))
                  (conj handler))]
    #{[pathom-url :post inter :route-name ::graph-api]}))


(defn pathom-routes
  "Valid options are:

   :parser        pathom parser. Mandatory.

   :pathom-url    Path for pathom viz UI (GET requests) and to listen for queries
                  (POST requests). \"/pathom\" by default.

   :pathom-viz?   Enable pathom viz UI. False by default.

   :interceptors  Extra interceptors to append to the endpoint responding to
                  EQL queries
   "
  [options]
  (let [{:keys [pathom-url pathom-viz?]
         :or {pathom-url default-pathom-url}}
        options
        base-routes (default-routes options)]
    (if-not pathom-viz?
      base-routes
      (let [asset-path' (str default-asset-path "/*path")

            index-handler (let [index-response (index-html)]
                            (fn [request]
                              index-response))

            asset-get-handler (fn [request]
                                (ring-resp/resource-response (-> request :path-params :path)
                                                             {:root "pathom-viz"}))
            asset-head-handler #(-> %
                                    asset-get-handler
                                    (assoc :body nil))]
        (conj base-routes
          [pathom-url :get [(custom-secure-headers) index-handler] :route-name ::graphiql-ide-index]
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
                          ;; TODO only for dev mode
                          p/trace-plugin]}))
