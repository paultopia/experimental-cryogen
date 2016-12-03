{:title "Devcards for Testing Clojurescript Projects: A Beginner Introduction"
 :layout :post
 :date "2016-12-03"
 :executable false
 :tags  ["devcards" "testing" "clojure" "clojurescript"]}
 
Devcards is an amazing library that basically lets you create a separate test build of a Clojurescript project.  [The documentation](http://rigsomelight.com/devcards/#!/devdemos.testing) is pretty clear, but if you're a relative Clojurescript beginner like me, you'll need a little bit more hand-holding. So here's my mini-walkthrough for getting Devcards integrated into a CLJS project, targeted at people who know the basics. (Recommended background knowledge: can set up a small Clojurescript project, perhaps with help of a lein template, and get it to compile. It will also help to [know how testing works](http://blog.jayfields.com/2010/08/clojuretest-introduction.html) on JVM Clojure-side with clojure.test.). 
 
## Why?
 
Clojurescript testing is hard. Or, at least, it seems hard. Honestly, I haven't gotten to the point of figuring out how to make it work yet. Unlike JVM Clojure where you can just use clojure.test and set up some deftests and then just run them with lein test, Clojurescript testing requires setting up some browser environment (or, I guess, Node or something?) to run it in.  Most non-Devcards techniques [seem to require you](https://github.com/bensu/doo#setting-up-environments) to do extreme yak-shavey things like set up Phantom.js.
 
By contrast, Devcards just lets you set up "cards," that only show up in special builds, and that demonstrate their functionality on a convenient little webpage. So then when you want to do unit testing on your Clojurescript app, you just compile the Devcards build, open up the special Devcards webpage, and then look to make sure everything is working as expected. Your test run is opening a webpage. (And if you have Figwheel going, then you get live test reloading on that webpage.) 

You can have an ordinary test, which displays on the web much like it displays when running lein test (i.e., you can see what passed and what failed), and you can also use a "card," which is just a snippet of code that gets executed and displayed, and is useful for "testing" stuff that requires direct visual examination. (For example, I'm working on a browser extension that uses [Chartist.js](https://gionkunz.github.io/chartist-js/) to display data vis, and I'm just [defining cards in a test namespace](https://github.com/paultopia/browser-stats/commit/b6936f8466aba7b0c1695139bbd5c4fe6773ee2f) to allow me to eyeball test charts and make sure they look like I expect.)

## Setup

Of course you're going to want to require Devcards in your Leiningen dependencies (or do whatever it is that the Boot folks do). More importantly, you're also going to want to set up a separate build in your project.clj.  Here's what the builds section of my project.clj looks like (this is based off the awesome [reagent frontend template](https://github.com/reagent-project/reagent-frontend-template), which I basically use for everything, and then add devcards on my own. Hmm... maybe there's a PR or a fork in here...)

```
:cljsbuild {:builds {:app
                       {:source-paths ["src" "env/dev/cljs"]
                        :compiler
                        {:main "statspop.dev"
                         :output-to "public/js/app.js"
                         :output-dir "public/js/out"
                         :asset-path   "js/out"
                         :source-map true
                         :optimizations :none
                         :pretty-print  true}}
                       :devcards
                       {:source-paths ["src" "env/dev/cljs" "test"]
                        :figwheel
                        {:devcards true}
                        :compiler
                        { :main       "statspop.dev"
                         :asset-path "js/devcards_out"
                         :output-to  "public/js/devcards.js"
                         :output-dir "public/js/devcards_out"
                         :optimizations :none
                         :source-map-timestamp true }}
                       :release
                       {:source-paths ["src" "env/prod/cljs"]
                        :compiler
                        {:output-to "public/js/app.js"
                         :output-dir "public/js/release"
                         :asset-path   "js/out"
                         :optimizations :advanced
                         :pretty-print false}}}}
```

Note the devcards build in the middle.  That's where the action happens. 

- First, note that it has "test" as a source path, in addition to the other paths. That's where I've defined the cards.

- Second, note that it has a Figwheel option in the map with `{:devcards true}`.  You can use Devcards without Figwheel, but why are you building Clojurescript projects without Figwheel? (Also, Devcards and Figwheel were written by the same [awesome person](http://rigsomelight.com/), so they're really good at working together.)

- Third, note that it compiles to a totally different Javascript file. It goes to devcards.js rather than app.js. The implication here is that you put your devcards on a totally different html page too. So in the statspop project above, I have index.html in the target "public" directory, which contains the ordinary app (and will actually go away for prod, since this is to be a Chrome extension), but I also have cards.html, which, in relevant part, is just: 

```
<html>
    <head>
	<link rel="stylesheet" href="css/chartist.min.css">
	<script src="js/jstat.min.js" type="text/javascript"></script>
	 <script src="js/chartist.min.js"></script>
    </head>
    <body>
        <script src="js/devcards.js" type="text/javascript"></script>
    </body>
</html>
```

Note how that html just calls the Javascript and CSS libraries I'll be using (this may change, depending on how you set up external dependencies, using CLJSJS etc.), and then calls the devcards script defined in the project.clj.

Unlike normal CLJS libraries, you don't specify a HTML element in your cards page to render the Devcards into. So it doesn't have anything like a `<div id="app">` that you'd normally use with Reagent or something. Indeed, you don't need to add any code to render it at all.  Just construct the cards, and require the namespaces the cards are in from something that actually gets loaded (or something that gets loaded by something that gets loaded, etc.), and Devcards library code will handle the rendering for you.

That namespace point is important and easy to miss. While you don't have to explicitly render Devcards, you do have to have the namespace a card is defined in somewhere in the dependency tree of your main namespace ([main namespace mainly used for no-optimization compilation](https://github.com/clojure/clojurescript/wiki/Compiler-Options#main)). So my dependency tree looks something like this: 

<pre>
statspop.dev  (the main namespace specified in project.clj) 
 -- requires statspop.core
  ---- requires all the application code namespaces
 -- requires statspop.core-test
  ---- requires all the individual test namespaces (where Devcards cards and tests are defined)
</pre>


Speaking of rendering, the Devcards docs suggest setting up your main app rendering with a conditional, so that it only renders if the node it aims at appears. E.g., from the core.cljs one of my projects:

```
(defn mount-root []
  (when-let [app (.getElementById js/document "app")]
    (r/render [home-page] app)))
```

The idea there is that the application code (as opposed to the testing code) won't get run if the application page isn't loaded. This seems like a sensible precaution in view of the risk of side-effects, state pollution, etc.

That's about it for setup. 

## Using devcards. 

I just copypasta-boilerplate import everything I might ever need into every test namespace, e.g., 

```
(ns statspop.download-test
  (:require [statspop.download :as d] ; the functionality I want to test
            [cljs.test :as t :refer-macros [is testing]]
            [devcards.core :as dc :refer-macros [defcard deftest defcard-rg]]
            [reagent.core :as r]
            [cljs.test :as t :refer-macros [is testing]]))
```

After all, this is a dev build, it doesn't need to be light. 

Then usage is super simple.  To define an ordinary test, you just do it exactly as you would with JVM clojure.test. Devcards deftest just shadows the cljs.test symbol and emits both tests for devcards and ordinary cljs tests if you also want to do the test runner thing with Phantom.js and the like for some reason. For example, I've rolled my own quick-and-dirty CSV converter rather than bring in a whole library for one function, so that needs a test: 

```
(deftest nested-vectors-to-csv-string
  (is (=
       (d/format-vec-as-csv [["foo" "bar"] [1 2] ["baz" 3]])
       "foo,bar\n1,2\nbaz,3")))
```

To define a card, you use the defcard macro. This is mostly useful for ui/io/other state-y and side-effect-y-type things where automated testing won't work, you really just need to look and see that the input and output are what they should be.  There's also a defcard-rg macro that is just like defcard, but will take and render Reagent components for you; I tend to use that for all cards just to keep consistency with everything else I'm doing (since I'm aggressively reagent-for-everything). So this bit of code tests the creation of a downloadable csv file based on a Reagent component in my application code: 

```
(defcard-rg download-csv
  "download a csv file named data.csv containing the contents of the csv test above"
  [:div
   [:p
    [d/downloader [["foo" "bar"] [1 2] ["baz" 3]] :csv]]])
```

Note how they have docstrings and cool stuff like that too.  

That's it!  (Obviously, there are advanced functions, but this is the basic stuff.)  Now you can build your devcards build (e.g. `lein figwheel devcards` ---although Figwheel is much nicer wrapped in rlwrap or [used from Emacs](https://paultopia.github.io/posts-output/figwheel-emacs/)), go to your cards.html or whatever page, and you'll get a lovely menu of namespaces; click on a namespace to open it and you'll see all your tests + all your cards on the page. With the examples above, I can check that my csv string conversion test passes, and I can also try out the csv downloader functionality and make sure the csv that gets downloaded looks like it should. 

Sweet, you've got a test system. 
