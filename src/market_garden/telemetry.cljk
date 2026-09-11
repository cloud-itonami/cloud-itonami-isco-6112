(ns market-garden.telemetry
  "ITONAMI play-data -> operating-ledger bridge (ADR-2607031000 addendum,
  2026-07-04): ingest a network-isekai `Market Garden` playthrough record as a
  REAL operating record, through the SAME governor/store path a live
  proposal would use — never a parallel bypass. network-isekai has no
  server (static, CodePen-style host); the bridge is a JSON record the game
  host persists to localStorage (`isekai.itonami.log.<blueprint-id>`,
  `src/isekai/game.cljc` in network-isekai) that an operator exports and
  feeds to `ingest-playthrough!` here.

  A playthrough with governor violations (an unequipped tend logged by the
  game) is submitted at :high safety-class / low confidence — the SAME hard
  invariant that forces human sign-off on a risky field action forces
  human sign-off on ingesting a risky playthrough, too."
  (:require [market-garden.store :as store]
            [market-garden.governor :as governor]))

(def session-plot-id "network-isekai-session")
(def session-planting-id "network-isekai-playthrough")

(defn ensure-session-planting!
  "Idempotently register the synthetic plot/planting a playthrough files its
  harvest against — distinct from any real plot/planting the operator
  tracks, so playthrough ingestion can never be mistaken for real field
  provenance."
  [st]
  (when-not (store/plot st session-plot-id)
    (store/register-plot! st {:plot-id session-plot-id
                               :name "network-isekai playthrough sessions"
                               :near-water? false}))
  (when-not (store/planting st session-planting-id)
    (store/register-planting! st {:planting-id session-planting-id
                                   :plot-id session-plot-id
                                   :crop "network-isekai-session"
                                   :sown-at "2026-07-04"})))

(defn ingest-playthrough!
  "Run a network-isekai playthrough record
  (`{:score :picked :lives-remaining :violations :outcome}`, matching the
  JSON shape `src/isekai/game.cljc` persists) through the real
  MarketGardenGovernor and, only on :proceed, append it to the ledger via
  `record-harvest!`. Returns the governor's decision map plus `:ingested?`
  and the original `:record`."
  [st {:keys [score picked violations] :as record}]
  (ensure-session-planting! st)
  (let [env (governor/env-for-store st)
        proposal {:action :treat
                  :planting-id session-planting-id
                  :effect :propose
                  :safety-class (if (pos? (or violations 0)) :high :low)
                  :confidence (if (pos? (or violations 0)) 0.4 0.9)}
        decision (governor/assess env proposal)
        proceed? (= :proceed (:decision decision))]
    (when proceed?
      (let [seq-n (count (store/harvests-of st session-planting-id))]
        (store/record-harvest! st {:harvest-id (str "playthrough-" (or (:ts record) seq-n))
                                    :planting-id session-planting-id
                                    :qty-kg (or picked score 0)
                                    :source :network-isekai
                                    :raw record})))
    (assoc decision :ingested? proceed? :record record)))
