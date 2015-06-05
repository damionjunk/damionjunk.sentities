# damionjunk.sentities

"Sentities" is a combination of sentiment, and entities. This code connects to
the public Twitter API and runs the Stanford [CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml)
NER and sentiment via my [damionjunk.nlp](https://github.com/damionjunk/damionjunk.nlp) library.

Specifically, `damionjunk.sentities` consumes the Twitter [statuses](https://dev.twitter.com/streaming/reference/get/statuses/sample)
API endpoint: `https://stream.twitter.com/1.1/statuses/sample.json`.
According to Twitter, this stream:

> Returns a small random sample of all public statuses. The Tweets returned by the default access level are the same, so if two different clients connect to this endpoint, they will see the same Tweets.

This program then uses CoreNLP to detect entities and compute the sentiment score for the sentence containing the detected entities.
The entities are then placed into a map, along with a running sum of the sentiment and total number of occurrences of this entity.

The recent updates add CMU's ark-tweet-nlp to the mix. Nouns are pulled out and counted. ark-tweet-nlp does a better job with POS tagging the Twitter stream.

## Background

This is a toy, and the scientific methodology here is severely lacking.
This is intentional.
Much more work would be involved in properly measuring the sentiment surrounding entity mentions on Twitter,
but this may be useful for someone starting out, as an idea spring board.
I would be happy to discuss and collaborate, so contact me if this is of interest to you.

## Libraries

The heavy lifting is done with Stanford's CoreNLP via my NLP wrapper.
CMU's ark-tweet-nlp is also running behind the scenes to do Twitter specific POS tagging.
[Core.async](https://github.com/clojure/core.async) is also used to build asynchronous pipelines,
since under some conditions, the incoming data rate may exceed the processing chain (text -> JSON -> CoreNLP(NER/Sentiment/POS) -> Aggregation).
Core.async makes it very easy and fun to split into separate handlers and parallelize the tasks post-HTTP.
[clj-http](https://github.com/dakrone/clj-http) is used for the HTTP, [Cheshire](https://github.com/dakrone/cheshire) for JSON parsing, and [clj-oauth](https://github.com/mattrepl/clj-oauth) for taking care of the OAuth tasks.

## Usage

**You will need OAuth credentials from Twitter for this to work.**

This code was created to demonstrate Twitter API use combined with entity
detection and sentiment. `damionjunk.sentities.sentities` contains a `(comment)`
block for REPL based exploration.

I walk through this code at my [blog](http://damionjunk.com/post/sentities/) as well.

You can take a look at the top nouns by examining the contents of `damionjunk.sentities.work/arknouns`:

```clojure
(let [results @swork/arknouns
        sresults (into (sorted-map-by
                         (fn [k1 k2]
                           (compare [(get results k1) k1]
                                    [(get results k2) k2])))
                       results)]
    (clojure.pprint/pprint (take-last 30 sresults)))

;; Results =>
;;

(["harry" 36]
 ["boys" 37]
 ["followers" 37]
 ["home" 37]
 ["summer" 37]
 ["tonight" 41]
 ["man" 42]
 ["everyone" 45]
 ["game" 45]
 ["guys" 45]
 ["thanks" 46]
 ["shit" 48]
 ["twitter" 49]
 ["way" 50]
 ["beyoncé" 51]
 ["minaj" 52]
 ["world" 53]
 ["ariana" 54]
 ["grande" 54]
 ["nicki" 55]
 ["someone" 55]
 ["girl" 58]
 ["control" 59]
 ["bitches" 65]
 ["video" 65]
 ["life" 66]
 ["time" 98]
 ["day" 102]
 ["today" 123]
 ["people" 130])
```

Similarly, you can dump contents of `damionjunk.sentities.work/sentities`:

```clojure
(let [results @swork/sentities
      sresults (into (sorted-map-by (fn [k1 k2]
                                      (compare [(:count (get results k1)) k1]
                                               [(:count (get results k2)) k2])))
                     results)]
  ;; We want the top 30, sorted by :count
  (clojure.pprint/pprint (map (fn [[k {numer :sentiment denom :count}]]
                                {:entity k
                                 :sentiment (* 1.0 (/ numer denom))
                                 :count denom})
                              (take-last 30 sresults))))

;; Results
;; =>

({:entity "Barcelona", :sentiment 1.5, :count 6}
 {:entity "Bieber", :sentiment 1.166666666666667, :count 6}
 {:entity "Brown", :sentiment 1.5, :count 6}
 {:entity "Caitlyn", :sentiment 1.0, :count 6}
 {:entity "Curry", :sentiment 1.333333333333333, :count 6}
 {:entity "Gaga", :sentiment 2.0, :count 6}
 {:entity "House", :sentiment 1.166666666666667, :count 6}
 {:entity "Lady", :sentiment 2.0, :count 6}
 {:entity "Lauren", :sentiment 1.166666666666667, :count 6}
 {:entity "Taylor", :sentiment 1.166666666666667, :count 6}
 {:entity "Zayn", :sentiment 1.0, :count 6}
 {:entity "LIAM", :sentiment 1.0, :count 7}
 {:entity "Luke", :sentiment 1.0, :count 7}
 {:entity "Netflix", :sentiment 1.428571428571429, :count 7}
 {:entity "Davis", :sentiment 1.25, :count 8}
 {:entity "Liam", :sentiment 1.0, :count 9}
 {:entity "Chris", :sentiment 1.5, :count 10}
 {:entity "Louis", :sentiment 1.5, :count 10}
 {:entity "Niall", :sentiment 1.5, :count 10}
 {:entity "Jeremy", :sentiment 1.0, :count 12}
 {:entity "Justin", :sentiment 1.230769230769231, :count 13}
 {:entity "London", :sentiment 1.0, :count 13}
 {:entity "&", :sentiment 1.285714285714286, :count 14}
 {:entity "Facebook", :sentiment 2.125, :count 16}
 {:entity "#IChooseBEYONCE", :sentiment 1.0, :count 51}
 {:entity "Beyoncé", :sentiment 1.0, :count 51}
 {:entity "Grande", :sentiment 1.0, :count 52}
 {:entity "Minaj", :sentiment 1.0, :count 52}
 {:entity "Ariana", :sentiment 1.055555555555556, :count 54}
 {:entity "Nicki", :sentiment 1.037037037037037, :count 54})
```

Make any sorts of assumptions that you'd like about the results. :)

## License

Copyright © 2015 Damion Junk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
