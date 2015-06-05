(defproject damionjunk/sentities "0.1.0-SNAPSHOT"
  :description "A toy program to demonstrate twitter api consumption and CoreNLP processing."
  :url "https://github.com/damionjunk/damionjunk.sentities"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [cheshire "5.4.0"]
                 [clj-oauth "1.5.2"]
                 [damionjunk/nlp "0.3.0"]
                 [damionjunk/common "0.1.0"]
                 [clj-http "1.1.2"]])
