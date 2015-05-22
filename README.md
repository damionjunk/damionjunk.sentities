# damionjunk.sentities

"Sentities" is a combination of sentiment, and entities. This code connects to
the public Twitter API and runs the Stanford [CoreNLP](http://nlp.stanford.edu/software/corenlp.shtml)
NER and sentiment via my [damionjunk.nlp](https://github.com/damionjunk/damionjunk.nlp) library.

## Background

This is a toy, and the scientific methodology here is severely lacking.
This is intentional.
Much more work would be involved in properly measuring the sentiment surrounding entity mentions on Twitter,
but this may be useful for someone starting out, as an idea spring board.
I would be happy to discuss and collaborate, so contact me if this is of interest to you.

## Usage

**You will need OAuth credentials from Twitter for this to work.**

This code was created to demonstrate Twitter API use combined with entity
detection and sentiment. `damionjunk.sentities.sentities` contains a `(comment)`
block for REPL based exploration.

I walk through this code at my [blog](http://damionjunk.com/post/) as well.
The first post in the series can be found [here](http://damionjunk.com/post/).

After it's been running for a while, you can dump contents of `damionjunk.sentities.work/sentities`:

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

({:entity "of", :sentiment 1.5, :count 16}
 {:entity "Calum", :sentiment 1.411764705882353, :count 17}
 {:entity "Duggar", :sentiment 1.117647058823529, :count 17}
 {:entity "ISIS", :sentiment 1.352941176470588, :count 17}
 {:entity "Jin", :sentiment 1.588235294117647, :count 17}
 {:entity "News", :sentiment 1.352941176470588, :count 17}
 {:entity "Alistair", :sentiment 1.0, :count 18}
 {:entity "Bill", :sentiment 1.277777777777778, :count 18}
 {:entity "Josh", :sentiment 1.222222222222222, :count 18}
 {:entity "Luke", :sentiment 1.333333333333333, :count 18}
 {:entity "Bank", :sentiment 1.157894736842105, :count 19}
 {:entity "Black", :sentiment 1.157894736842105, :count 19}
 {:entity "Curry", :sentiment 1.263157894736842, :count 19}
 {:entity "Eric", :sentiment 1.947368421052632, :count 19}
 {:entity "Niall", :sentiment 1.210526315789474, :count 19}
 {:entity "San", :sentiment 1.857142857142857, :count 21}
 {:entity "Chris", :sentiment 1.590909090909091, :count 22}
 {:entity "New", :sentiment 1.590909090909091, :count 22}
 {:entity "Sam", :sentiment 1.739130434782609, :count 23}
 {:entity "John", :sentiment 1.64, :count 25}
 {:entity "Justin", :sentiment 1.259259259259259, :count 27}
 {:entity "Obama", :sentiment 1.444444444444444, :count 27}
 {:entity "Google", :sentiment 1.25, :count 28}
 {:entity "-", :sentiment 1.4, :count 30}
 {:entity "Carmichael", :sentiment 0.96875, :count 32}
 {:entity "James", :sentiment 1.424242424242424, :count 33}
 {:entity "David", :sentiment 1.277777777777778, :count 36}
 {:entity "Michael", :sentiment 1.5625, :count 48}
 {:entity "&", :sentiment 1.225806451612903, :count 62}
 {:entity "Facebook", :sentiment 2.375, :count 72})
```

Make any sorts of assumptions that you'd like about the results. :)

## License

Copyright © 2015 Damion Junk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
