{:title "How to spin up up a long-running background process in Clojure"
 :layout :post
 :date "2017-07-26"
 :executable false
 :tags  ["clojure"]}

Here's a task that doesn't seem to be terribly well documented in one place.  What happens if you want to spin up some long-running background shell process in clojure?  For example, suppose you're trying to communicate with a python script ([example of how to do that](https://github.com/paultopia/snakepit)), and you'd like to spin both the rabbitmq server and the continuously-running python script up from your main clj application? You can't just use clojure.java.shell, because that will hang the application until the other process returns (i.e., it's blocking).

The easiest method to do this that I can discern is to just wrap it in a future.  For example:

```clojure
(ns spinup.core
  (:require [clojure.java.shell :refer [sh]))

(def rabbit (future (sh "/usr/local/sbin/rabbitmq-server")))
(def python (future (sh "python" "talktoclojure.py")))
(do-the-stuff-with-a-running-server)
```

then, when you want to tear down the foreign process, you can kill it using `future-cancel`.  So to take down the python process, `(future-cancel python)` will be quite sufficient.  (Rabbitmq is a bit tougher, see the repo linked above for deets.)

WARNING: this doesn't seem to be totally reliable.  For example, sometimes (like in my rabbitmq tests), I can get it to spin up and shut down processes, but I can't seem to be able to send messages to the background processes.  Other times, I can get it to spin up a process that will write to disk, but won't shut down on using `future-cancel`.
