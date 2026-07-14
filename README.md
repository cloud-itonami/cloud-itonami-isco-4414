# cloud-itonami-isco-4414

Open Occupation Blueprint for **ISCO-08 4414**: Scribes and Related Workers.

This repository designs a forkable OSS business for an independent scribing and document-preparation practice: a printing, binding and certified-copy robot handles physical document production under a governor-gated actor, so the practice keeps its own preparation records instead of renting a closed document SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a printing, binding and certified-copy robot performs document printing, binding and certified-copy production under an actor that proposes
actions and an independent **Scribing Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
legal or official document certification) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client request + document template + accuracy confirmation
        |
        v
Scribing Advisor -> Scribing Governor -> draft/finalize, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `4414`). Required capabilities:

- :robotics
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
