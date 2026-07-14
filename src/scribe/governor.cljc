(ns scribe.governor
  "ScribingGovernor — the independent safety/traceability layer named
  in this repository's README/business-model.md ('Scribing Advisor ->
  Scribing Governor'), gating every document-preparation operation an
  advisor may propose for a client. The Advisor has no notion of
  client provenance, delegated-authority scope or attestation/legal-
  filing risk, so this MUST be a separate system able to reject a
  proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section). Modeled on cloud-itonami-isco-4214's
  debtcollection.governor.

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store and never itself files, signs or
  certifies anything. The StateGraph's `:decide` node routes on the
  verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance    — the request's client must be a
                              registered, verified record in the store
                              before ANY drafting/transcription/
                              filing/signing action is committed.
    2. no-actuation          — proposal :effect must be :propose only
                              (the actor never directly files or signs
                              anything itself; it only proposes to).
    3. delegation required   — :file-on-behalf-of-client and
                              :sign-on-behalf-of-client must always
                              hold UNLESS the store's client record
                              shows an explicit, currently-valid
                              delegation-of-authority grant
                              (`:delegation-of-authority?` true).
                              Absence of that grant is a hard block,
                              not merely a low-confidence issue.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per
  the README robotics-premise and business-model.md's Trust Controls —
  'no legal or official document certification without governor
  gate'):
    4. :op :certify-copy — attesting a copy as a true copy of an
                           original creates attestation liability.
    5. :op :file-on-behalf-of-client / :sign-on-behalf-of-client —
                           EVEN when the hard delegation check (3)
                           passes, filing with a third party or
                           signing on the client's behalf carries
                           irreversible legal consequence and always
                           requires human sign-off in addition to the
                           hard gate.
    6. low confidence (< `confidence-floor`)."
  (:require [scribe.store :as store]))

(def confidence-floor 0.6)

(def ^:private delegation-required-ops
  #{:file-on-behalf-of-client :sign-on-behalf-of-client})

(def ^:private always-escalate-ops
  #{:certify-copy :file-on-behalf-of-client :sign-on-behalf-of-client})

(defn- hard-violations [{:keys [proposal]} client-record]
  (cond-> []
    (nil? client-record)
    (conj {:rule :no-client :detail "未登録/未検証 client — 起草/代筆/提出/署名の前に登録済み client レコードが必須"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（actor は自ら提出/署名/認証しない）"})

    (and client-record
         (contains? delegation-required-ops (:op proposal))
         (not (:delegation-of-authority? client-record)))
    (conj {:rule :no-delegation
           :detail "委任状 (delegation-of-authority) が未登録/無効 — client の代理提出/代理署名には明示的かつ現に有効な委任の grant が必須。欠如は単なる低確信ではなくハードブロック"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `scribe.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        hard (hard-violations {:proposal proposal} client-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
