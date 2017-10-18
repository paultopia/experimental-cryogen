{:title "For Clojure Webscraping, Try Jsoup!"
 :layout :post
 :date "2017-10-17"
 :executable false
 :mathy false
 :math-preprocessed false
 :tags  ["clojure" "java" "webscraping"]}
 
When people want to do webscraping in Clojure, the standard recommendation/tutorial library is [Enlive](https://github.com/cgrand/enlive). ([Example](http://masnun.com/2016/03/20/web-scraping-with-clojure.html), [and another](http://ericsomdahl.github.io/posts/2015-03-07-gevents1.html), and there are at least two scraping libraries built on top of Enlive, [Pegasus](http://blog.shriphani.com/2016/01/25/pegasus-a-modular-durable-web-crawler-for-clojure/) and [Skyscraper](https://github.com/nathell/skyscraper).) 

But Enlive doesn't seem to be really built for scraping. For example, it's very difficult to actually get the text (the rough equivalent of browser api `document.innerText`, minus ajax-loaded context) out of a html document, and when you can get text, it comes out badly formatted---e.g., if you just pull all the text from the body tag, you don't get spaces between things like table rows and columns.  The best I can come up with to get decently formatted text without just walking all the individual dom nodes myself is the following tangled mess (where `html` is the Enlive html namespace and I've brought `replace` and `trim` in from clojure.string):

```clojure
(defn- space-out-punctuation [text] (replace text #"([.?!\",:;“”\)\(\[\]\{\}])" "$1 "))

(defn- space-out-caps-diff [text] (replace text #"([a-z])([A-Z])" "$1 $2"))

(defn- space-out-letter-digit [text] (replace text #"(\d)([a-zA-Z])" "$1 $2"))

(defn- de-whitespace [text]
  (trim (replace text #"\s+" " ")))

(defn extract-text [webpage]
  (-> webpage
      (html/select [:body])
      (html/transform [:img] nil)
      (html/transform [:script] nil)
      (html/transform [:style] nil)
      html/texts
      first
      space-out-punctuation
      space-out-caps-diff
      space-out-letter-digit
      de-whitespace))
```

By contrast, if you go to Javaland and use [Jsoup](https://jsoup.org/), extracting all the text from a parsed document is a simple method call. And the formatting isn't perfect, but it's better than I can do with all that ugly code with Enlive.

Similarly, suppose you want a list of links from your document.  That's pretty easy to do with Enlive... except if you want to resolve relative links to absolute links, e.g., for crawling, well, that requires pulling in a separate library to sort out the URLs and writing a few more functions.  (Fortunately, Chas Emerick has written a [URL library](https://github.com/cemerick/url), and, like all of Chas's libraries, it works beautifully.)

With Jsoup, that's another simple tweak to a single method call.

What this amounts to is that 50 or so lines of code using Enlive turn into 22 lines of code with Jsoup.  Here is all the code you need to fetch a page (admittedly, this needs error handling for failed requests), get out the texts, and get a list of links, resolved to absolute URLs, with titles for every link: 

```clojure
(ns myscraper.core
  (:import [org.jsoup Jsoup]))

(defn get-page-at-address [address]
  (let [conn (Jsoup/connect address)
        soup (.get conn)]
    soup))

(defn extract-link-data [link]
  (let [linktext (.text link)
        address (.attr link "abs:href")]
    {:linktext linktext :address address}))

(defn extract-links [soup]
  (let [links (.select soup "a")]
    (mapv extract-link-data links)))

(defn fetch [address]
  (let [soup (get-page-at-address address)
        links (extract-links soup)
        text (.text soup)]
    {:soup soup :links links :text text}))
```

I'm really happy I finally took the trouble to get a handle on Clojure-Java interop---there are a lot of really nice Java libraries out there!  

(Note to self: maybe try out [Reaver](https://github.com/mischov/reaver), which is a library someone built to leverage Jsoup in Clojure.)
