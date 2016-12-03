{:title "Figwheel + Spacemacs"
 :layout :post
 :date "2016-12-02"
 :executable false
 :tags  ["emacs" "spacemacs" "clojure" "clojurescript"]}
 
I've been trying to figure out how to get Figwheel and Spacemacs to play nicely together for a while.  It turns out it's much easier than it looks (required knowledge level: reasonable comfort with front-end Clojurescript and leiningen, prior use of Figwheel, successful installation of Spacemacs and use of Clojure mode).

Relevant documentation: [Using the Clojure layer in Spacemacs](https://github.com/syl20bnr/spacemacs/tree/master/layers/%2Blang/clojure), [using Figwheel in nREPL](https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl).

That later document gives a process for integrating Figwheel into Emacs that doesn't seem to work with my Spacemacs stuff. But most of what it says to do isn't actually necessary, and I've extracted from it a method that doesn't actually require any special configuration in your .spacemacs, other than having Clojure mode activated.  Here's the entirety of my clojure configuration from my .spacemacs, none of which is necessary to get figwheel going: 

```
  (spacemacs/toggle-evil-cleverparens-on)
  (add-hook 'clojure-mode-hook #'evil-cleverparens-mode)
  (setq cider-show-error-buffer nil)
  (with-eval-after-load 'clojure-mode
    (dolist (c (string-to-list ":_-?!#*"))
      (modify-syntax-entry c "w" clojure-mode-syntax-table )))
```

So how do we do it?  

**Step 1**: make sure the right stuff is in your project.clj (I have no ideas for boot users).  First, you need all the appropriate configuration for Figwheel. I usually start CLJS projects with the excellent [reagent frontend template](https://github.com/reagent-project/reagent-frontend-template), which handles most of that stuff for you. But in case you need something better, here are some of the basics that I have in my project.cljs: 

*Figwheel in plugins*

```
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.8"]]
```

*Figwheel settings giving nrepl middleware, ports (might not be needed?), etc.*

```
  :figwheel {:http-server-root "public"
             :nrepl-port 7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs ["public/css"]}
```

*CLJS builds giving source paths, e.g.:*

```
  :cljsbuild {:builds {:app
                       {:source-paths ["src" "env/dev/cljs"]  ; needed
                        :compiler
                        {:main "statspop.dev"
                         :output-to "public/js/app.js"
                         :output-dir "public/js/out"
                         :asset-path   "js/out"
                         :source-map true
                         :optimizations :none
                         :pretty-print  true}}

```

*repl-options summoning up piggieback*

```
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

```

5.  and a dev profile calling in piggieback and figwheel-sidecar

```
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.4-5"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [com.cemerick/piggieback "0.2.2-SNAPSHOT"]]}})
```

Not all this stuff may be required (and my process is a little cargo-cultey), but at least the piggieback, sidecar, and source paths elements are required per the figwheel docs. If you work from the reagent frontend template, the only thing you need to add is the :repl-options section in the defproject. 

**Step 2**: then you're actually done!  Now you can activate figwheel from within emacs.  And you don't need to mess around with jacking-in clojurescript separate from clojure or anything silly like that.  Instead, you need only: 

1.  Open a cljs source file from your project.

2.  Start a normal clojure repl with `SPC m s i` 

3.  Open up the CIDER REPL buffer (if it isn't up already, just use `SPC w -` to get a new window, then `SPC b b` to get a menu of buffers).  Now you have a cider repl, a CLJ/JVM one rather than a CLJS/JS one.

4.  In the cider repl, call the following three functions in order:

```
(use 'figwheel-sidecar.repl-api)

(start-figwheel!)

(cljs-repl)
```


And now you have a Figwheel REPL in the buffer.  You can send stuff to the browser just like you can with Figwheel in the terminal, AND you can use the normal spacemacs/cider key bindings to send stuff to the fancy figwheel repl through emacs. 

Even more awesome, you can then quit out of the cljs repl with `:cljs/quit`, at which point you'll have a clj repl again, and you can send CLJ/JVM code to that as normal.  BUT: figwheel will still be autobuilding!  So you can edit your cljs code and it'll shoot right back to the browser.  Then, when you want a figwheel repl again, you can go with `(cljs-repl)` and it'll be right back.

When you kill cider (with `SPC m s q`) it's polite enough to also take out the figwheel autobuild. 

Magic. 
