(ns pathom.pedestal.jsoup
 (:import
   [java.net URI]
   [org.jsoup.nodes Element]
   [org.jsoup.select Elements]))


(defn relative-url? [url]
  (cond
    (clojure.string/starts-with? url "//") false
    (.isAbsolute (URI. url)) false
    :else true))


(defprotocol AttrFn
  (attr-update [this n f]
    "Similar to update. Updates Jsoup attribute with the result of applying f
     to the old attribute value"))

(extend-type Element
  AttrFn
  (attr-update [this n f]
    (let [attr (.attr this n)]
      (.attr this n (f attr)))))

(extend-type Elements
  AttrFn
  (attr-update [this n f]
    (doseq [el this]
      (attr-update el n f))))
