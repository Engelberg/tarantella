(defproject tarantella "1.1.1"
  :description "A Clojure implementation of Knuth's Dancing Links algorithm"
  :url "http://github.com/Engelberg/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [better-cond "2.1.0"]] 
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"])
