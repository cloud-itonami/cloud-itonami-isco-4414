(ns scribe.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [scribe.actor :as actor]
            [scribe.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Ana Reyes"})
    (store/register-client! st {:client-id "client-2" :name "Delegated Client"
                                 :delegation-of-authority? true})
    st))

(deftest commits-a-clean-low-risk-draft
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :draft-document :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-on-unregistered-client-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "no-such-client" :op :draft-document :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-client")))
    (is (= :hold (:disposition (:state result))))))

(deftest holds-sign-on-behalf-without-delegation-without-committing
  (testing "the hard delegation gate blocks the run before any human-approval interrupt is even reached"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:client-id "client-1" :op :sign-on-behalf-of-client :stake :high}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "client-1"))))))

(deftest interrupts-then-commits-certify-copy-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; certify-copy always escalates (attestation liability)
        request {:client-id "client-1" :op :certify-copy :stake :high}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "client-1")))))))

(deftest interrupts-then-commits-sign-on-behalf-with-delegation-on-human-approval
  (testing "even with a valid delegation grant, signing on the client's behalf still requires human sign-off"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:client-id "client-2" :op :sign-on-behalf-of-client :stake :high}
          interrupted (actor/run-request! graph request {} "thread-5")]
      (is (= :interrupted (:status interrupted)))
      (is (empty? (store/records-of st "client-2")))
      (let [resumed (actor/approve! graph "thread-5")]
        (is (= :done (:status resumed)))
        (is (some? (get-in resumed [:state :record])))
        (is (= 1 (count (store/records-of st "client-2"))))))))
