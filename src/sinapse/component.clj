(ns sinapse.component
  (:require [integrant.core :as ig]
            [clojure.tools.logging :as log]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [common-clj.traceability.core :as traceability]
            [schema.core :as s])
  (:import (clojure.lang IFn PersistentQueue)))

(s/defschema Consumers
  {s/Str {:schema       s/Any
          :interceptors [s/Any]
          :handler-fn   IFn}})

(defn handler-fn->interceptor
  [handler-fn]
  (interceptor/interceptor
    {:name  ::consumer-handler-fn-interceptor
     :enter (fn [context]
              (handler-fn context)
              context)}))

(defn enqueue!
  [{:keys [topic] :as message}
   sinapse]
  (let [message' (assoc message :meta {:correlation-id (traceability/current-correlation-id!)})]
    (swap! (get-in sinapse [:messages topic]) conj message')))

(defmethod ig/init-key ::sinapse
  [_ {:keys [components consumers]}]
  (log/info :starting ::sinapse)

  (s/validate Consumers consumers)

  (let [bag-of-messages (reduce (fn [acc current]
                                  (assoc acc (:title current) (atom PersistentQueue/EMPTY)))
                                {} (-> components :config :topics))
        sinapse {:messages bag-of-messages}]

    (doseq [{:keys [title parallel-consumers]} (-> components :config :topics)
            :let [schema (get-in consumers [title :schema])
                  interceptors (get-in consumers [title :interceptors])
                  handler-fn (get-in consumers [title :handler-fn])]]

      (dotimes [_n (or parallel-consumers 4)]
        (future (while true
                  (let [[old _new] (-> (get bag-of-messages title)
                                       (swap-vals! pop))
                        {:keys [payload] :as message} (peek old)]
                    (try
                      (s/validate schema payload)
                      (chain/execute {:payload    payload
                                      :components (assoc components :sinapse sinapse)}
                                     (conj (or interceptors []) (handler-fn->interceptor handler-fn)))
                      (catch Exception ex
                        (log/error ::error-while-consuming-message :exception ex)
                        (enqueue! message sinapse))))))))
    sinapse))

(defmethod ig/halt-key! ::sinapse
  [_ _]
  (log/info :stopping ::sinapse))
