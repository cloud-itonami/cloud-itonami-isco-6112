(ns market-garden.governor
  "MarketGardenGovernor — the independent safety/traceability layer for the
  ISCO-08 6112 market-gardening actor. The Garden Advisor proposes actions
  (treat a planting, record a harvest); it has no notion of water-source
  safety or planting provenance, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD — the itonami-actor pattern
  (independent Governor gates a proposing actor) applied to this occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. `:high`/`:safety-critical` actions
  ALWAYS require human sign-off, even when every hard invariant passes.

  HARD invariants for :garden/propose:
    1. Planting provenance — a treatment or harvest must reference a
       registered planting on a registered plot.
    2. Water-source safety  — any treatment on a near-water? plot has
       safety-class at least :high, which forces human sign-off; it is never
       auto-approved.
    3. No-actuation         — the proposal must not directly mutate a harvest
       or treatment record outside the record-treatment!/record-harvest!
       path (effect must be :propose, never a raw store write).
  SOFT:
    4. Confidence floor → escalate."
  (:require [market-garden.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- hard-violations [{:keys [planting-fn plot-fn]} proposal]
  (let [{:keys [action planting-id safety-class effect]} proposal
        planting (planting-fn planting-id)
        plot     (when planting (plot-fn (:plot-id planting)))]
    (cond-> []
      (nil? planting)
      (conj {:rule :no-planting :detail (str "未登録 planting " planting-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and (= action :treat) plot (:near-water? plot)
           (< (safety-rank (or safety-class :none)) (safety-rank :high)))
      (conj {:rule :water-source-safety
             :detail "near-water? plot への treatment は :high 以上の safety-class が必須"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:planting-fn`/`:plot-fn`
  lookups, decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 1.0)]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `market-garden.store/Store` implementation."
  [store]
  {:plot-fn     #(store/plot store %)
   :planting-fn #(store/planting store %)})
