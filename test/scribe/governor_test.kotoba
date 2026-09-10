(ns scribe.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [scribe.store :as store]
            [scribe.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Ana Reyes"})
    (store/register-client! st {:client-id "client-2" :name "Delegated Client"
                                 :delegation-of-authority? true})
    st))

(defn- op [o]
  {:op o :effect :propose :confidence 0.9 :stake :low})

(deftest ok-on-clean-draft
  (let [st (fresh-store)
        v (governor/check {:client-id "client-1"} {} (op :draft-document) st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-clean-transcription
  (let [st (fresh-store)
        v (governor/check {:client-id "client-1"} {} (op :transcribe-dictation) st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "no-such-client"} {} (op :draft-document) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check {:client-id "client-1"} {}
                           (assoc (op :draft-document) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-sign-on-behalf-without-delegation
  (testing "absence of an explicit, currently-valid delegation grant is a hard block, not a low-confidence issue"
    (let [st (fresh-store)
          v (governor/check {:client-id "client-1"} {}
                             (assoc (op :sign-on-behalf-of-client) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :no-delegation (:rule %)) (:violations v))))))

(deftest hard-on-file-on-behalf-without-delegation
  (testing "absence of an explicit, currently-valid delegation grant is a hard block, not a low-confidence issue"
    (let [st (fresh-store)
          v (governor/check {:client-id "client-1"} {}
                             (assoc (op :file-on-behalf-of-client) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :no-delegation (:rule %)) (:violations v))))))

(deftest escalates-sign-on-behalf-even-with-valid-delegation
  (testing "irreversible legal consequence — human sign-off required even when the hard delegation gate passes"
    (let [st (fresh-store)
          v (governor/check {:client-id "client-2"} {}
                             (assoc (op :sign-on-behalf-of-client) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-file-on-behalf-even-with-valid-delegation
  (testing "irreversible legal consequence — human sign-off required even when the hard delegation gate passes"
    (let [st (fresh-store)
          v (governor/check {:client-id "client-2"} {}
                             (assoc (op :file-on-behalf-of-client) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-certify-copy-even-at-high-confidence
  (testing "attesting a copy as true creates attestation liability regardless of confidence"
    (let [st (fresh-store)
          v (governor/check {:client-id "client-1"} {}
                             (assoc (op :certify-copy) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        v (governor/check {:client-id "client-1"} {}
                           (assoc (op :draft-document) :confidence 0.2) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:client-id "client-1" :op :draft-document})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "client-1"))))
    (is (= 1 (count (store/ledger st))))))
