(ns scribe.store
  "SSoT for the ISCO-08 4414 independent scribing & document-preparation
  practice actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a printing, binding
  and certified-copy robot performs the physical document work under
  this advisor/governor pair, which never dispatches hardware itself
  and never certifies, files or signs a document outside the governor
  gate). Modeled on cloud-itonami-isco-4214's debtcollection.store.

  Domain:

    client — a registered scribing client (:client-id, :name and an
             optional :delegation-of-authority? flag). The flag
             records whether the client has an explicit, currently-
             valid grant of legal delegated authority on file —
             absence of the flag (nil/false) is the default and is
             the ONLY state under which :file-on-behalf-of-client /
             :sign-on-behalf-of-client are structurally blocked by
             `scribe.governor` (a hard, non-overridable gate; see
             that ns's docstring).
    record — a committed operating record under a client (a drafted
             document, a transcription, a certified copy, a filing or
             a signature performed on the client's behalf) — written
             ONLY via commit-record!, never mutated in place.
    ledger — an append-only audit trail of every proposal/verdict/
             disposition, regardless of outcome (commit or hold).")

(defprotocol Store
  (client [s client-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :records [] :ledger []} seed)))))
