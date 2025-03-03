(ns sinapse.component
  (:require [iapetos.core :as prometheus]
            [integrant.core :as ig]
            [clojure.tools.logging :as log]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [common-clj.traceability.core :as traceability]
            [medley.core :as medley]
            [schema.core :as s])
  (:import (clojure.lang IFn PersistentQueue)))

(def metrics
  [(prometheus/counter :produced-messages {:labels [:service :topic :host]})
   (prometheus/counter :consumed-messages {:labels [:service :topic :host]})
   (prometheus/counter :replayed-messages {:labels [:service :topic :host]})])

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
   {:keys [registry service-name] :as sinapse}]
  (let [message' (assoc message :meta {:correlation-id (traceability/current-correlation-id!)})]
    (swap! (get-in sinapse [:messages topic]) conj message')
    (when registry
      (prometheus/inc registry :produced-messages {:service service-name
                                                   :topic   topic}))))

(defmethod ig/init-key ::sinapse
  [_ {:keys [components consumers]}]
  (log/info :starting ::sinapse)

  (s/validate Consumers consumers)

  (let [prometheus (:prometheus components)
        bag-of-messages (reduce (fn [acc current]
                                  (assoc acc (:title current) (atom PersistentQueue/EMPTY)))
                                {} (-> components :config :topics))
        sinapse (medley/assoc-some {:messages     bag-of-messages
                                    :service-name (-> components :config :service-name)}
                                   :registry (:registry prometheus))]

    (doseq [{:keys [title parallel-consumers]} (-> components :config :topics)
            :let [schema (get-in consumers [title :schema])
                  interceptors (get-in consumers [title :interceptors])
                  handler-fn (get-in consumers [title :handler-fn])]]

      (dotimes [_n (or parallel-consumers 4)]
        (future (while true
                  (let [[old _new] (-> (get bag-of-messages title)
                                       (swap-vals! pop))
                        {:keys [payload] :as message} (peek old)]
                    (when message
                      (try
                        (s/validate schema payload)
                        (chain/execute {:payload    payload
                                        :components (assoc components :sinapse sinapse)}
                                       (conj (or interceptors []) (handler-fn->interceptor handler-fn)))
                        (when prometheus
                          (prometheus/inc (:registry prometheus) :consumed-messages {:service (-> components :config :service-name)
                                                                                     :topic   title}))
                        (catch Exception ex
                          (log/error ::error-while-consuming-message :exception ex)
                          (enqueue! message sinapse)
                          (when prometheus
                            (prometheus/inc (:registry prometheus) :replayed-messages {:service (-> components :config :service-name)
                                                                                       :topic   title}))))))))))
    sinapse))

(defmethod ig/halt-key! ::sinapse
  [_ _]
  (log/info :stopping ::sinapse))
