(ns puppetlabs.puppetdb.metrics.server
  (:require [puppetlabs.puppetdb.metrics.core :as metrics]
            [net.cgrand.moustache :refer [app]]
            [puppetlabs.puppetdb.middleware :refer
             [wrap-with-debug-logging wrap-with-authorization
              wrap-with-certificate-cn wrap-with-default-body]]
            [ring.middleware.params :refer [wrap-params]]))

(def v1-app
  (app
    []
    {:get metrics/list-mbeans}

    [& names]
    {:get (app (metrics/mbean names))}))

(defn routes
  []
  (app
   ["v1" "mbeans" &]
   {:any v1-app}))

(defn build-app
  "Generate a Ring application that handles metrics requests

  `options` is a list of keys and values where keys can be the following:

  * `authorizer` - a function that takes a request and returns a
    :authorized if the request is authorized, or a user-visible reason if not.
    If not supplied, we default to authorizing all requests."
  [& options]
  (let [opts (apply hash-map options)]
    (-> (routes)
        (wrap-params)
        (wrap-with-authorization (opts :authorizer (constantly :authorized)))
        (wrap-with-certificate-cn)
        (wrap-with-default-body)
        (wrap-with-debug-logging))))
