(ns example
 (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]

   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]
   [com.wsscode.pathom.profile :as pp]

   [pathom.pedestal :refer [pathom-routes]]))

(def tv-shows
  {:rm  #:tv-show{:id :rm
                  :title         "Rick and Morty"
                  :character-ids [:rick :summer :morty]}
   :bcs #:tv-show{:id :bcs
                  :title         "Better Call Saul"
                  :character-ids [:bcs]}
   :got #:tv-show{:id :got
                  :title         "Game of Thrones"
                  :character-ids [:arya :ygritte]}})

(def characters
  {:rick    #:character{:name "Rick Sanshes" :tv-show-id :rm}
   :summer  #:character{:name "Summer Smith" :tv-show-id :rm}
   :saul    #:character{:name "Saul Goodman" :tv-show-id :bcs}
   :arya    #:character{:name "Arya Stark" :tv-show-id :got}
   :morty   #:character{:name "Morty Smith" :tv-show-id :rm}
   :ygritte #:character{:name "Ygritte" :tv-show-id :got}})


; initialize a dispatch function
(defmulti resolver-fn pc/resolver-dispatch)

; initialize indexes
(def indexes (atom {}))

; this creates a factory that will add a new method on the resolver-fn and add it to the indexes
(def defresolver (pc/resolver-factory resolver-fn indexes))

(defresolver `show-by-id
  {::pc/input  #{:tv-show/id}
   ::pc/output [:tv-show/id :tv-show/title :tv-show/character-ids]}
  (fn [env {:keys [tv-show/id]}]
    (get tv-shows id)))

(defresolver `show-characters
  {::pc/input #{:tv-show/character-ids}
   ::pc/output [{:tv-show/characters [:character/id :character/name :character/tv-show-id]}]}
  (fn [env {:keys [tv-show/character-ids]}]
    {:tv-show/characters (vals (select-keys characters character-ids))}))

(def parser
  (p/parser {::p/plugins
             [(p/env-plugin
                {::p/reader             [p/map-reader
                                         pc/all-readers
                                         p/env-placeholder-reader]
                 ::pc/resolver-dispatch resolver-fn
                 ::p/placeholder-prefixes #{">"}
                 ::pc/indexes           @indexes})
              pp/profile-plugin]}))


;; Pedestal

(def routes
  (route/expand-routes (pathom-routes {:oge? true :parser parser})))

(def port 8890)

(defn create-server []
  (http/create-server
   {::http/routes routes
    ::http/type   :jetty
    ::http/port   port}))

(defn start []
  (http/start (create-server)))

(defn -main [& args]
  (prn (str "Starting server on http://localhost:" port))
  (start))
