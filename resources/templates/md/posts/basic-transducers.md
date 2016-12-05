{:title "Basic Transducers"
 :layout :post
 :date "2016-12-5"
 :executable true
 :tags  ["clojure"]}
 
"Transducer" has to be the most intimidating name in all Clojure-land. For everyday use, however, they're really simple. (I don't get the truly fancy stuff, like writing one's own transducers, or getting them to hold state.)
 
The simple "for beginners" (recommended experience level: you know how to use map and reduce and function composition and the threading macro) flavor of transducers is: like function composition, but with some more fancy. 

This blog post is an attempted translation of the basic parts of the [Clojure documentation page on transducers](http://clojure.org/reference/transducers) into more comprehensible terms that don't require you to be as smart as Rich Hickey to understand.

So one way to think of transducers is just as a different way of doing ordinary function composition, but where you don't have to explicitly call "partial" to do partial application for those sequence functions that support the special one-less-arity-makes-a-transducer powers, and also they go in a different order.

```clojure
(def transducer-math
  (comp
   (map #(+ 5 %))
   (map #(/ % 3))))

(def composed-math
  (comp
   (partial map #(+ 5 %))
   (partial map #(/ % 3))))
```

Compare the two: 

```clojure
(into [] (composed-math [1 2 3]))
```

```clojure
(into [] transducer-math [1 2 3])
```

Why are these different?  Because ordinary function composition runs backwards, like nesting, so it applies the division before the addition.  Transducer composition runs forward, the "natural order," like the arrow threading macro `->` does, so it applies the addition before the division.

`transduce` is like `reduce`, with a bit of magic under the hood. The docs have stuff I don't even begin to understand in there, but what it seems to amount to is that it applies the transducer to the collection before reducing, i.e., you can replace `reduce` with `transduce transducer` and it will otherwise work. 

```clojure
(def xf 
  (comp 
   (filter odd?)
   (filter #(> 5 %))))
```

```clojure
(println (transduce xf + (range 10)))
```

```clojure
(println (reduce + (range 10)))
```

```clojure
(println (transduce xf conj [] (range 10)))
```

```clojure
(println (reduce conj [] (range 10)))
```

`eduction` seems to basically just mean "create a lazy sequence from applying the transducer to the collection" 

```clojure
(take 2 (eduction xf (range 10)))
```

However, `sequence` does the same thing, so I have to confess to a bit of confusion as to what the differences are.

```clojure
(take 2 (sequence xf (range 10)))
```

Other advantages of transducers: you can apply them to core.async channels, so everything that comes in gets transformed. They're also potentially more efficient than ordinary nested calls and such, because they don't create intermediate sequences for all the steps (not even lazy ones).

For future reference, here's the list of the built-in higher-order sequence functions that make transducers when called with one less arity, straight from the docs:

- map 

- cat 

- mapcat 

- filter 

- remove 

- take 

- take-while 

- take-nth 

- drop 

- drop-while 

- replace 

- partition-by 

- partition-all 

- keep 

- keep-indexed 

- map-indexed 

- distinct 

- interpose 

- dedupe 

- random-sample

Christophe Grand has also written [tranducerific support for more core functions](https://github.com/cgrand/xforms). Right now they seem a little hard to understand (especially x/reduce!!) but reading the code is interesting...

Alto, [this is a nice SO](http://stackoverflow.com/questions/26317325/can-someone-explain-clojure-transducers-to-me-in-simple-terms).




