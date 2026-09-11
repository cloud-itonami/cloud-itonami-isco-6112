(ns market-garden.store
  "SSoT for the ISCO-08 6112 market-gardening sole-proprietor actor, behind a
  `Store` protocol so the backend is a swap (MemStore default ‖ a real
  Datomic/kotoba-server backend, per the itonami actor pattern).

  Domain = independent market-garden operations:

    plot        — a growing area (plotId, name, near-water? boolean)
    planting    — a crop sown on a plot (plantingId, plotId, crop, sownAt)
    treatment   — an input applied to a planting (treatmentId, plantingId,
                  kind #{:water :fertilizer :pesticide}, safetyClass)
    harvest     — yield recorded against a planting (harvestId, plantingId,
                  qtyKg)

  The append-only records are the operating ledger: a planting must exist
  before a treatment or harvest can reference it, and treatments/harvests are
  never mutated in place, only appended.")

(defprotocol Store
  (plot [st plot-id])
  (planting [st planting-id])
  (plantings-of [st plot-id])
  (treatments-of [st planting-id])
  (harvests-of [st planting-id])
  (register-plot! [st plot])
  (register-planting! [st planting])
  (record-treatment! [st treatment])
  (record-harvest! [st harvest]))

(defrecord MemStore [state]
  Store
  (plot [_ plot-id]
    (get-in @state [:plots plot-id]))
  (planting [_ planting-id]
    (get-in @state [:plantings planting-id]))
  (plantings-of [_ plot-id]
    (filter #(= plot-id (:plot-id %)) (vals (:plantings @state))))
  (treatments-of [_ planting-id]
    (filter #(= planting-id (:planting-id %)) (:treatments @state)))
  (harvests-of [_ planting-id]
    (filter #(= planting-id (:planting-id %)) (:harvests @state)))
  (register-plot! [_ plot]
    (swap! state assoc-in [:plots (:plot-id plot)] plot))
  (register-planting! [_ planting]
    (swap! state assoc-in [:plantings (:planting-id planting)] planting))
  (record-treatment! [_ treatment]
    (swap! state update :treatments (fnil conj []) treatment))
  (record-harvest! [_ harvest]
    (swap! state update :harvests (fnil conj []) harvest)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:plots {} :plantings {} :treatments [] :harvests []} seed)))))
