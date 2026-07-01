(ns market-garden.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [market-garden.store :as store]
            [market-garden.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-plot! st {:plot-id "plot-dry" :name "North Bed" :near-water? false})
    (store/register-plot! st {:plot-id "plot-wet" :name "Creekside Bed" :near-water? true})
    (store/register-planting! st {:planting-id "p1" :plot-id "plot-dry" :crop "tomato" :sown-at "2026-03-01"})
    (store/register-planting! st {:planting-id "p2" :plot-id "plot-wet" :crop "lettuce" :sown-at "2026-03-01"})
    st))

(deftest proceeds-on-clean-dry-plot-treatment
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :treat :planting-id "p1" :safety-class :low
                   :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-planting
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :treat :planting-id "no-such-planting" :safety-class :low
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-planting (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :treat :planting-id "p1" :safety-class :low
                   :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest holds-on-water-source-treatment-without-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :treat :planting-id "p2" :safety-class :medium
                   :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :water-source-safety (:rule %)) (:violations result)))))

(deftest human-approval-on-high-safety-class-even-when-clean
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :treat :planting-id "p2" :safety-class :high
                   :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:action :treat :planting-id "p1" :safety-class :none
                   :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-treatment! st {:treatment-id "t1" :planting-id "p1" :kind :water})
    (store/record-harvest! st {:harvest-id "h1" :planting-id "p1" :qty-kg 12})
    (is (= 1 (count (store/treatments-of st "p1"))))
    (is (= 1 (count (store/harvests-of st "p1"))))
    (is (= 1 (count (store/plantings-of st "plot-dry"))))
    (is (empty? (store/treatments-of st "p2")))))
