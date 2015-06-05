(ns damionjunk.sentities.work
  (:require [damionjunk.nlp.stanford :as nlp]
            [damionjunk.nlp.cmu-ark :as ark]
            [clojure.string :as s]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async]))


(defonce running? (atom true))

;; Data collection atoms:
(defonce sentities (atom {}))
(defonce arknouns (atom {}))


;;
;; Async Processing chain:
;;
;; text -> JSON -> CoreNLP -> ???
(defonce t-text-in-chan (async/chan))
(defonce t-json-out-chan (async/chan))
(defonce t-annotated-out-chan (async/chan))


;; Don't bother using CoreNLP to process known non-english tweets.
(defn english? [{lang :lang}] (or (nil? lang) (= lang "en") (= lang "und")))

(defn annotate-tweet
  ""
  [tweet]
  (let [tid (:id tweet)
        uid (:id (:user tweet))
        uname (:name (:user tweet))
        text (:text tweet)]
    (when (and (not (nil? text)) (english? tweet))
      (let [smaps (nlp/sentiment-ner-maps text)]
        {:sentiment-ner smaps
         :ark (ark/tag text)
         :uid uid
         :tid tid
         :uname uname
         :tweet text}))))

(defn process-to-json [line] (json/parse-string line true))

;;
;; Async Workers and Worker Utils
;;

(defn start-text-consumers
  ""
  [n]
  (dotimes [_ n]
    (async/thread
      (while @running?
        (let [line (async/<!! t-text-in-chan)
              jdata (process-to-json line)]
          (async/>!! t-json-out-chan jdata))))))

(defn start-json-consumers
  "Starts N consumers who will eat JSON and annotate with the Stanford NLP."
  [n]
  (dotimes [_ n]
    (async/thread
      (while @running?
        (let [jdata (async/<!! t-json-out-chan)
              adata (annotate-tweet jdata)]
          ;; Annotate, and write to the annotated channel.
          (when adata (async/>!! t-annotated-out-chan adata)))))))



(defn entities-mfn
  "A mapping function that maps to sequences like:

   ({:sentiment 2,
     :text \"Google it.\",
     :tokens ({:pos \"NNP\", :ner \"ORGANIZATION\", :token \"Google\"}
              {:pos \"PRP\", :ner \"O\", :token \"it\"}
              {:pos \".\", :ner \"O\", :token \".\"})})

   Returns a sequence of maps like:

   ({:sentiment 2 :entity \"Google\"} ... )
   "
  [smap]
  (let [toks (:tokens smap)]
    (keep identity (map (fn [{ner :ner t :token}]
                          (when (or (= "ORGANIZATION" ner) (= "PERSON" ner))
                            {:sentiment (:sentiment smap) :entity t}))
                        toks))))

(defn nouns-filter-pred
  "A nouns predicate that looks for CMU's POS tags that
   indicate whether the token is a noun or not."
  [annotation]
  (case (:pos annotation)
    "N" true
    "^" true
    "S" true
    "Z" true
    false))

(defn start-annotated-consumers
  "Does something with the 'Annotated' tweets we've generated."
  [n]
  (dotimes [_ n]
    (async/thread
      (while @running?
        (let [adata     (async/<!! t-annotated-out-chan)
              smaps     (:sentiment-ner adata)
              entities  (mapcat entities-mfn smaps)
              anouns    (filter nouns-filter-pred (:ark adata))]
          (doseq [an anouns]
            ;; update the `anouns` atom/map
            (let [noun (s/lower-case (:token an))]
              (if (nil? (get @arknouns noun)) (swap! arknouns assoc noun 1)
                                              (swap! arknouns update-in [noun] inc))))
          (doseq [se entities]
            ;; update the `sentities` atom/map
            (let [ek (:entity se)
                  se (dissoc se :entity)
                  se (assoc se :count 1)]
              ;; Update the `sentities` atom map by + merging the modified `se`
              ;; map which contains the sentiment and a count so we can track
              ;; entity frequency, and average sentiment surrounding the
              ;; entity.
              (swap! sentities update-in [ek] (partial merge-with +) se))))))))