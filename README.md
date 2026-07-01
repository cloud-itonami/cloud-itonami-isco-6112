# cloud-itonami-isco-6112

Open Occupation Blueprint for **ISCO-08 6112**: Market Gardeners and Crop Growers.

This repository designs a forkable OSS business for an independent smallholder market gardener: a field robot performs sowing, weeding and harvest-support work in the field under a governor-gated actor, so growers keep their own operating records instead of renting a closed farm-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a field robot performs sowing, weeding, irrigation-check and targeted harvest support under an actor that proposes
actions and an independent **Market Garden Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near buyers/visitors on-site, or applying treatments near water sources) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
planting plan + crop plan + market order
        |
        v
Garden Advisor -> Market Garden Governor -> tend/harvest, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `6112`). Required capabilities:

- :robotics
- :telemetry
- :optimization
- :dmn
- :bpmn
- :audit-ledger
- :forms

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/market_garden/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `market-garden.store` — `Store` protocol + `MemStore`: plots, plantings,
  treatments, harvests. A treatment/harvest can only be recorded against a
  registered planting on a registered plot (planting provenance).
- `market-garden.governor` — `MarketGardenGovernor`: `assess` gates a
  proposal against the plot/planting env. Hard invariants force `:hold`
  (unregistered planting, direct-write instead of `:propose`, or a
  water-source-adjacent treatment below `:high` safety-class); `:high`/
  `:safety-critical` proposals always return `:human-approval` even when
  every hard invariant passes; low-confidence proposals also escalate.

```bash
clojure -M:test   # 7 tests, 14 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) — the
first `cloud-itonami-isco-*` occupation to reach that tier (ADR-2607012000).

## License

AGPL-3.0-or-later.
