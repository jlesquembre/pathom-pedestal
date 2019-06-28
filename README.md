# pathom-pedestal

[![Clojars Project](https://img.shields.io/clojars/v/lafuente/pathom-pedestal.svg)](https://clojars.org/lafuente/pathom-pedestal)
[![cljdoc badge](https://cljdoc.org/badge/lafuente/pathom-pedestal)](https://cljdoc.org/d/lafuente/pathom-pedestal/CURRENT)

A library to integrate [pathom](https://github.com/wilkerlucio/pathom) and
[pedestal](http://pedestal.io/)

## Usage

This library provide one main function, `pathom-routes`, which return a set of
[Pedestal routes](http://pedestal.io/reference/routing-quick-reference), using
the
[table syntax](http://pedestal.io/reference/routing-quick-reference#_table_syntax)

```clojure
(ns example.core
 (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [pathom.pedestal :refer [pathom-routes]]))


(def routes
  (route/expand-routes (pathom-routes {:oge? true :parser parser})))

(defn create-server []
  (http/create-server
   {::http/routes routes
    ::http/type   :jetty
    ::http/port   8890}))

(http/start (create-server)))
```

See [API docs](https://cljdoc.org/d/lafuente/pathom-pedestal/CURRENT) for a list
of valid options

For convenience, a helper function, `make-parser`, is also provided.

## Example project

See the example directory. To run the example:

```
clj -A:dev
```

Go to http://localhost:8890/oge and try some query. E.g.:
`[{[:tv-show/id :bcs] [:tv-show/title]}]`
