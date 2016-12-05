{:title "Using macros to get data into Clojurescript front-end pages"
 :layout :post
 :date "2016-12-04"
 :executable false
 :tags  ["clojure" "clojurescript"]}
 
Suppose you want to construct pure front-end static pages that will be as fast as possible.  Optimum speed at least arguably involves minimizing server round trips.  Ideally, everything should be one round trip, which should fetch all code and data for the entire site (so long as it isn't huge). This minimizes network latency, permits aggressive caching strtegies, the use of services like cloudflare to optimize the stink out of everything, etc. 

But that comes at a cost. If everything is bundled up together, that means instead of, say, having your SPA application go fetch some markdown from the server with an AJAX request and render it to the user (network round trip! oh noes!), you get to mix the code and the data. But that's just ugly, and hard to maintain. For example, this is what some of the code from [gowder.io](http://gowder.io), which is just a personal static site built with Reagent, looks like: 

```
(defn research-page []
  [shell "Researcher"
   [:div
    [:p "I'm currently an academic researcher, and in that capacity, since 2012, I've published one book, "
     [:a {:href "http://rulelaw.net"} "The Rule of Law in the Real World"]
     ", and over a dozen articles in constitutional law, political and legal philosophy, critical race theory, classical Athenian law and political theory, and distributive justice. I've also given about 40 scholarly presentations in the last 4 years. For details, see "
     [:a {:href "http://paul-gowder.com"} "my academic website"] ", where you can also download copies of many articles. Other than the book, I'm perhaps proudest of "
     [:a {:href "https://zenodo.org/record/57249"} "an article"]
     " on urban racial segregation, cognitive bias, and ascriptive injuries."]
    [:p "Currently, I'm working on more quantitative and computational projects, including some experiments with machine-learning predictions of judicial outcomes and with human-computer facilitated empirical data collection."]
    [:p "I have a Ph.D. in political science from "
     [:a {:href "https://politicalscience.stanford.edu"} "Stanford"]
     " and a law degree from "
     [:a {:href "http://hls.harvard.edu"} "Harvard."]
```

What a bloody mess.  Any time I have to change the content, I have to be careful to make sure I don't accidentally unmatch some braces or mis-nest some tags or something equally stupid. In addition to being likely to introduce errors into the code, this is also likely to introduce errors into the content, since it isn't readily readable, and it gets formatted with indentation and such for code rather than for text. 

## When do Code and Content Come Together?

This demonstrates two strategies for bringing code and content together, which are imperfect because of their timing. Summarizing the above: 

1.  *Code and content come together at runtime*: this means that the code has to go fetch the content from somewhere. And that means network latency and stuff.

2.  *Code and content come together at write time*: this means that the code and content have to be written together, like in the same file. And that's difficult to write and maintain. 

But there is a alternative. 

Clojurescript is a lisp!  It has macros!  This means that we can take content, write it in a separate place from the code, and then bring the two together *at compile time*. 

Here's a trivial demo, which is available in full runnable form [on Github](https://github.com/paultopia/jsonmacrodemo). We take [Reagent](https://reagent-project.github.io/), [Cheshire](https://github.com/dakrone/cheshire), [Hickory](https://github.com/davidsantiago/hickory) and [Selmer](https://github.com/yogthos/Selmer), and run them together as follows: 

First, in a macro running on the JVM, we take a JSON of content, parse it with Cheshire, and then pass it to Selmer to turn it into HTML. It should be pretty easy to do something similar with Markdown content, if you're into that. Then we parse that with Hickory into Hiccup data structures.

Then we call that macro from CLJS, and at compile-time, it kindly fetches the data and inserts it into our code, ready to be used by Reagent. 

So we have a minimal template.html:

```
<p>This text comes from a Selmer template! Hello {{name}}!</p>
```

And we have a minimal name.json: 

```
{"name": "World!"}
```

And then we have a macro.clj:

```
(ns jsonmacrodemo.macro
  (:require [cheshire.core :refer [parse-string]]
            [selmer.parser :refer [render-file render]]
            [hickory.core :refer [parse-fragment as-hiccup]]))

(defn get-data [jsonfile]
  (parse-string (slurp jsonfile) true))

(defmacro from-template [template jsonfile]
  (first (map as-hiccup (parse-fragment (render (slurp template) (get-data jsonfile))))))
```

And finally, we have some the Clojurescript that consumes it, core.cljs: 

```
(ns jsonmacrodemo.core
  (:require [reagent.core :as reagent])
  (:require-macros [jsonmacrodemo.macro :as m]))

(defn home-page []
  [:div
   (m/from-template "template.html" "name.json")])

(reagent/render [home-page] (.getElementById js/document "app"))

```

Code and content are completely separated at write time, but get compiled together before they ever see a web browser. Best of both worlds.
