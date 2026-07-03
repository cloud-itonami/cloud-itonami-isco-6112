# Business Model: Independent Market Gardening Operations

## Classification

- Repository: `cloud-itonami-isco-6112`
- ISCO-08: `6112`
- Occupation: Market Gardeners and Crop Growers
- Domain: `:agriculture/market-gardening`
- Social impact: food-security, rural-livelihoods, environment
- Robotics-premise: `true` — this blueprint assumes field robots do physical plot work
  (planting, treatment, tending, harvest-assist) under human/co-op direction, not that a human
  does 100% of the fieldwork by hand.

## Customer

- smallholder growers
- market-garden co-ops
- farmers markets
- CSAs

## Offer

- planting and crop planning
- field-robot task assignment
- harvest scheduling
- direct-sale order management
- yield and cost reporting

## Revenue

- setup fee
- monthly garden-operations platform fee
- per-harvest fee
- reporting package

## Governor: `market-garden-governor`

Every field-robot action against a plot — treatment application, tending, harvest — is proposed
by the operations layer but only *executed* if `market-garden-governor` signs off first. The
governor is not a one-time onboarding check; it is a per-action gate, re-earned for each
individual plot action, not a standing permit. Concretely, the governor's decision rule:

- **Approves** a plot action only when a fresh sign-off exists for that specific action and that
  specific plot/window. A sign-off obtained for one plot does not carry over to the next — each
  tending, treatment, or harvest call re-triggers the gate. This mirrors the blueprint's own
  Trust Controls (below): the governor is consumed, not accumulated.
- **Rejects** any treatment application (pesticide/fertilizer/organic-input dispatch to a field
  robot) that lacks sign-off, protecting the `food-security` social-impact commitment — an
  ungated treatment could compromise crop safety or contaminate yield data that co-op members and
  CSA subscribers rely on.
- **Rejects** any water-source-adjacent robot action without human sign-off, protecting the
  `environment` social-impact commitment — irrigation/runoff-adjacent actions are the highest
  ecological-risk category in market gardening and are escalated rather than auto-approved.
  water-source-adjacent actions require human sign-off. no treatment application without
  governor gate.
- **Ties every approval and rejection to the audit ledger** (see Required Technologies below),
  so a smallholder co-op member can trace exactly which robot action was authorized, by what
  rule, for which plot — protecting `rural-livelihoods`: co-op members are billed and credited
  only for governor-approved robot work, so no member absorbs the cost of an unauthorized or
  mistaken robot action.

An action attempted without a live sign-off is a **governor violation**: the robot action does
not execute, the plot is left untended, and the incident is escalated/logged rather than silently
retried. This "propose → gate → execute-or-block" shape is exactly what the companion playable
prototype makes tangible — see `docs/operator-guide.md`.

## Required Technologies

`blueprint.edn` names `[:robotics :telemetry :optimization :dmn :bpmn :audit-ledger :forms]`.
Each exists for a specific job in this business, not as a generic stack:

- **`:robotics`** — the field robots that physically do planting, weeding, treatment
  application, and harvest-assist work on smallholder/co-op plots. This is the thing the
  governor gates: robotics is the actuator, the governor is the safety valve in front of it.
- **`:telemetry`** — soil, moisture, and crop-condition signal coming off the plots and robots
  (e.g. an aphid-pressure or moisture threshold breach on a specific bed) that triggers an intake
  event in the first place. Without telemetry the operator is gardening blind between visits.
- **`:optimization`** — plot-visit routing and planting/harvest scheduling across a grower's
  full set of plots, including prioritizing a time-boxed, higher-value harvest window (a
  ripe-produce window that must be picked before it turns) ahead of routine tending.
- **`:dmn`** — the decision-table engine `market-garden-governor` runs on: encodes which
  treatment types are on the approved/organic list, which actions are water-source-adjacent, and
  which combinations require escalation to a human vs. auto-approve.
- **`:bpmn`** — the process definition for the intake → propose → approve → execute → audit loop
  itself (see `docs/operator-guide.md`), so the same workflow runs identically whether the
  triggering event is a telemetry alert, a co-op member's request, or a scheduled harvest.
- **`:audit-ledger`** — the append-only record of every governor decision and every robot action
  taken against every plot. This is what makes "yield and cost data is auditable, not editable"
  (see Trust Controls) true in practice, and what a co-op member or certifier can pull to verify
  a specific harvest was governor-approved end to end.
- **`:forms`** — the intake surface: consent/purpose capture, plot registration, and treatment
  or harvest requests from smallholder growers and co-op members who are not themselves operating
  the robots.

## Trust Controls

- no treatment application without governor gate
- water-source-adjacent actions require human sign-off
- yield and cost data is auditable, not editable
- a governor sign-off is single-use per plot action (see Governor section above) — it is not a
  standing authorization, so a robot cannot "coast" on an earlier approval to touch a different
  plot or repeat an action
