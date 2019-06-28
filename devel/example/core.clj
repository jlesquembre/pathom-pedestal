(ns example.core
 (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]

   [com.wsscode.pathom.connect :as pc]

   [pathom.pedestal :refer [pathom-routes make-parser]]))

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


(pc/defresolver show-by-id [env {:keys [tv-show/id]}]
  {::pc/input  #{:tv-show/id}
   ::pc/output [:tv-show/id :tv-show/title :tv-show/character-ids]}
  (get tv-shows id))

(pc/defresolver show-characters [env {:keys [tv-show/character-ids]}]
  {::pc/input #{:tv-show/character-ids}
   ::pc/output [{:tv-show/characters [:character/id :character/name :character/tv-show-id]}]}
  {:tv-show/characters (vals (select-keys characters character-ids))})


(def parser (make-parser [show-by-id show-characters]))

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
  (prn (str "Open http://localhost:" port "/oge"))
  (start))
