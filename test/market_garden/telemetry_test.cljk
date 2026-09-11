(ns market-garden.telemetry-test
  (:require [clojure.test :refer [deftest is testing]]
            [market-garden.store :as store]
            [market-garden.telemetry :as telemetry]))

(deftest clean-playthrough-proceeds-and-appends-harvest
  (let [st (store/mem-store)
        record {:score 8 :picked 8 :lives-remaining 3 :violations 0 :outcome "victory" :ts 111}
        result (telemetry/ingest-playthrough! st record)]
    (is (= :proceed (:decision result)))
    (is (true? (:ingested? result)))
    (is (= 1 (count (store/harvests-of st telemetry/session-planting-id))))
    (is (= 8 (:qty-kg (first (store/harvests-of st telemetry/session-planting-id)))))))

(deftest violated-playthrough-is-held-and-not-silently-appended
  (let [st (store/mem-store)
        record {:score 5 :picked 5 :lives-remaining 1 :violations 2 :outcome "gameover" :ts 222}
        result (telemetry/ingest-playthrough! st record)]
    (is (= :human-approval (:decision result)))
    (is (false? (:ingested? result)))
    (is (empty? (store/harvests-of st telemetry/session-planting-id)))))

(deftest session-planting-is-registered-idempotently
  (let [st (store/mem-store)]
    (telemetry/ingest-playthrough! st {:score 1 :picked 1 :violations 0 :ts 1})
    (telemetry/ingest-playthrough! st {:score 2 :picked 2 :violations 0 :ts 2})
    (is (some? (store/plot st telemetry/session-plot-id)))
    (is (some? (store/planting st telemetry/session-planting-id)))
    (is (= 2 (count (store/harvests-of st telemetry/session-planting-id))))))
