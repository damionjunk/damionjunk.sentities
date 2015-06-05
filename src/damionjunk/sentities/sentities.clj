(ns damionjunk.sentities.core
  (:require [damionjunk.nlp.stanford :as nlp]
            [damionjunk.sentities.work :as swork]
            [damionjunk.common :as dj]
            [clojure.tools.logging :as log]
            [clj-http.client :as http]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [oauth.client :as oauth]
            [oauth.signature :as oas]
            [clojure.string :as s]))

;; We don't really need to store the config in an atom, but it's just a
;; habit from larger programs where config is used in multiple places.
(defonce config (atom {}))

;;
;; Some OAuth Header Stuff
;;

(defn sign-query
  [app-config verb uri & {:keys [query]}]
  (merge {:realm "Twitter API"}
         (oauth/credentials (:consumer app-config)
                            (:user-access-token (:oauth app-config))
                            (:user-access-token-secret (:oauth app-config))
                            verb
                            uri
                            query)))


(defn oauth-header-string
  ""
  [s-map]
  (let [sm (map (fn [[k v]] (format "%s=\"%s\"" (name k) (oas/url-encode (str v)))) s-map)]
    (str "OAuth " (s/join "," sm))))


;;
;; Pull configuration from the file system.
;; dj/get-app-config is just reading an EDN file with the private OAuth
;;                   credentials.
;;

(defn configure
  [basepath pubfile]
  (let [cfg      (dj/get-app-config :sentities :basepath basepath :pub pubfile :priv false)
        oa       (:oauth cfg)
        consumer (oauth/make-consumer (:app-consumer-key oa)
                                      (:app-consumer-secret oa)
                                      "https://twitter.com/oauth/request_token"
                                      "https://twitter.com/oauth/access_token"
                                      "https://twitter.com/oauth/authorize"
                                      :hmac-sha1)]
    (swap! config assoc :consumer consumer :oauth oa)))

;;
;;
;;

(defn get-the-tweets
  []
  (let [url  "https://stream.twitter.com/1.1/statuses/sample.json"
        oas  (sign-query @config "GET" url)
        oah  (oauth-header-string (into {} (map (fn [[k v]] [(name k) (str v)]) oas)))
        resp (http/get url {:headers {"Authorization" oah}
                            :as      :stream})
        ]
    (try
      (with-open [rdr (io/reader (:body resp))]
        (doseq [line (line-seq rdr)]
          (if @swork/running?
            (async/>!! swork/t-text-in-chan line)           ;; Toss the tweet to our raw text pipeline for processing
            (throw (Exception. "Running over.")))))
      (catch Exception e (log/warn e "Rolling out of IO Reader")))))


(defn initialize
  "Starts the CoreNLP and our Aysnc handlers."
  []
  (nlp/sentiment-ner-maps "Call before sending tweets to initialize CoreNLP and load the models.")
  (reset! swork/running? true)
  ;; create 2 text consumer Aysnc handlers
  (swork/start-text-consumers 2)

  ;; create 4 json/nlp consumers of the JSON tweets
  (swork/start-json-consumers 4)

  ;; create 1 data aggregator
  (swork/start-annotated-consumers 1))


;;
;; REPL - This is where the MAGIC is happening! Unicorns, Narwhals, and whatnot.
;;

(comment

  ;; The EDN configuration file should contain:
  {:sentities {:oauth {:user-access-token        ""
                       :user-access-token-secret ""
                       :app-consumer-key         ""
                       :app-consumer-secret      ""
                       }}}

  ;; With the values filled out of course!

  (do
    ;; Load the OAuth creds
    (configure "/Users/djunk/.config/damionjunk/" "damionjunk.pub.edn")

    ;; Load CoreNLP and start our Async threads
    (initialize)

    ;; Create a thread for the Web request so that we can cancel it when we
    ;; want, it's a 'run forever' type.
    ;;
    ;; get-the-tweets can't stop, won't stop.
    (future (get-the-tweets)))


  ;; We can kill std-err if it's too annoying ...
  (log/log-capture! "")

  ;;
  ;; After it's been running for a while, lets look at some results:
  (let [results @swork/sentities
        sresults (into (sorted-map-by
                         (fn [k1 k2]
                           (compare [(:count (get results k1)) k1]
                                    [(:count (get results k2)) k2])))
                       results)]
    ;; We want the top 30, sorted by :count
    (clojure.pprint/pprint (map (fn [[k {numer :sentiment denom :count}]]
                                  {:entity k
                                   :sentiment (* 1.0 (/ numer denom))
                                   :count denom})
                                (take-last 30 sresults))))

  ;; This time, lets look at the top nouns
  (let [results @swork/arknouns
        sresults (into (sorted-map-by
                         (fn [k1 k2]
                           (compare [(get results k1) k1]
                                    [(get results k2) k2])))
                       results)]
    (clojure.pprint/pprint (take-last 30 sresults)))


  ;; Check what we've collected so far
  (clojure.pprint/pprint @swork/sentities)

  (clojure.pprint/pprint @swork/arknouns)


  ;; Swap the running? atom, this will kill the web-request and stop the
  ;; async threads.
  (swap! swork/running? not)

  )