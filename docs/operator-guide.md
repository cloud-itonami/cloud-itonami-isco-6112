# Operator Guide

## First Deployment

1. Define the operator's service area and intake process.
2. Define consent and purpose categories.
3. Run synthetic operating cases.
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path
- provenance for all operating records
- human review for high-risk cases
- audit export for all gated actions

## Day in the Life: intake → propose → approve → execute → audit

This is what running `cloud-itonami-isco-6112` (Independent Market Gardening Operations) looks
like for one concrete plot event, not an abstract "task." Say a smallholder grower in the co-op
has a lettuce bed showing aphid pressure.

1. **Intake.** The plot's telemetry (`:telemetry`, see `docs/business-model.md`) reports a leaf
   or moisture sensor threshold breach on Plot 4, or the grower files an intake form (`:forms`)
   requesting a look at the bed. Either way, an intake event is opened against that specific
   plot — not a generic backlog item.
2. **Propose.** The operations layer proposes an action: dispatch the field robot (`:robotics`)
   to Plot 4 to apply an organic aphid treatment, sequenced by the day's plot-visit optimization
   (`:optimization`) alongside the grower's other beds and any time-boxed ripe-harvest windows
   that need picking before they turn.
3. **Approve.** Before the robot moves, the proposal hits `market-garden-governor`. Its DMN
   rule-table (`:dmn`) checks: is this treatment on the approved/organic list? Is Plot 4
   water-source-adjacent (if so, it escalates to a human instead of auto-approving, per the
   blueprint's Trust Controls)? Is there already a live sign-off for *this* plot action, or does
   one need to be issued now? Only a fresh, plot-specific sign-off lets execution proceed — an
   old sign-off from yesterday's harvest does not carry over.
4. **Execute.** With sign-off in hand, the BPMN-defined workflow (`:bpmn`) dispatches the field
   robot to Plot 4, it applies the approved treatment, and the plot's status updates from
   "needs-attention" to "tended." If the robot (or a human override) tries to touch Plot 4
   *without* a live sign-off, that's a governor violation: the action does not execute, the plot
   stays untended, and the attempt is escalated rather than silently retried or silently allowed.
5. **Audit.** The governor's decision (approve/reject/escalate), the robot's action, and the
   resulting plot state all land in the append-only audit ledger (`:audit-ledger`). The co-op
   member who owns Plot 4 — and, at reporting time, the whole co-op's yield/cost report — can
   trace exactly which treatment was applied, under what sign-off, and when. Nothing in that
   trail is editable after the fact.

Multiply this by every plot a grower or co-op runs, plus scheduled harvest calls, and that intake
→ propose → approve → execute → audit loop is the entire operating model of this blueprint.

### Feel the approval-gate loop hands-on

network-isekai ships a playable prototype of exactly this loop, generated from this blueprint:
**`/itonami/market-garden`** (network-isekai `public/games/itonami/market-garden`). In the game,
you move a grower around a shed (the depot) and eight plots. Touching the shed first is the
`market-garden-governor` plot sign-off (the game's `equipped` flag); tending a plot while
equipped clears it, exactly like Step 4 above. Tending a plot *without* first visiting the shed
reproduces a governor violation — the plot is left untended and you lose a life, the same
"blocked, not silently allowed" behavior described in Step 3-4. A rarer "ripe-harvest" plot is
worth 3x score under the identical sign-off rule, mirroring the time-boxed harvest-window
prioritization described in the Propose step above. It's a fast, hands-on way for an operator or
certifier to internalize why the sign-off is single-use per action rather than a standing permit,
before touching the real robot-dispatch workflow.

## Certification

Certified operators must prove that the governor gates every safety-critical
robot action, and that safety-critical risks escalate to humans.
