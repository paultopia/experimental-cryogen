{:title "Clojure basics: destructuring"
 :layout :post
 :date "2016-12-17"
 :executable true
 :tags  ["clojure", "python"]}
 
I think destructuring is one of the harder things for total beginners to handle in Clojure, because the syntax is just really terse. When you get used to it, it makes the code more readable, but at the beginning, it makes the code lots less readable. 
 
There are some good destructuring tutorials out there (listed at the end), but none with live code execution (and the ability for readers to play around and tweak stuff) via the magic of [klipse](https://github.com/viebel/klipse).  So here goes! 

## The Basic Idea

Destructuring is a way to concisely create local bindings from complex data structures. Here's an example.  Suppose you have a map, and you want to do something with it. 

```clojure
(def a-triangle {:base 5 :height 10})
(defn area [triangle]
(let [b (:base triangle) h (:height triangle)]
(* b h 0.5)))
(area a-triangle)
```

But that's a lot of typing. Destructuring lets us do it smaller:

```clojure
(defn des1-area [triangle]
(let [{b :base h :height} triangle]
(* b h 0.5)))
(des1-area a-triangle)
```

The let assignment there is our first example of destructuring. It says "pull the values at `:base` and `:height` from the map `triangle`, then assign them to the local symbols `b` and `h`."

But this is still too much typing.  We don't even need a let binding at all, because we can destructure right in a function definition. 

```clojure
(defn des2-area [{b :base h :height}]
(* b h 0.5))
(des2-area a-triangle)
```

That's more like it! Note how this second example of destructuring is just like the first, except that you don't need to pass the local name of the map to it, because the function destructures it before it even gets a local binding. 

If you just want to use the same names as in the underlying map, the `:keys` option allows you to do so:


```clojure
(defn des3-area [{:keys [base height]}]
(* base height 0.5))
(des3-area a-triangle)
```

There, the destructuring statement should be read as "take the map that I'm going to pass into this function, and then give me the keys that are listed in the vector, and locally bind their values to the symbol-ized forms of their keys."

Sadly, the `:keys` option doesn't appear to work with string keys. 

```clojure
(def string-triangle {"base" 5 "height" 10})
(des3-area string-triangle)  ; :-(
```

However, slightly more longwinded destructuring does:

```clojure
(defn des4-area [{b "base" h "height"}]
(* b h 0.5))
(des4-area string-triangle)
```

And there's a happy surprise: 
```clojure
(defn des5-area [{:strs [base height]}]
(* base height 0.5))
(des5-area string-triangle)
```

That's right: `:strs`:strings::`:keys`::keywords.

### Vectors (and other sequential data structures) too

Suppose you want to add the first two numbers from a vector of arbitrary size. Here's a simple non-destructurey way. 

```clojure
(defn add-first-2 [v]
(+ (first v) (second v)))
(add-first-2 [1 2 3 4])
```

With destructuring, you can just take pluck out variables in order from the vector. So our function becomes:

```clojure
(defn add-first-2d [[a b]]
(+ a b))
(add-first-2d [1 2 3 4])
```

So what that destructuring form says is "take the sequential data structure you're passing into this function, pluck out the first two elements, and locally bind them the to the symbols a and b"

Suppose you want the first and the third?  Well, the convention is that you throw away an unused value with an underscore, so:

```clojure
(defn add-first-and-third [[a _ b]]
(+ a b))
(add-first-and-third [1 2 3 4])
```

By the way, I meant it when I said "any sequence"---this includes strings, anything that can behave sequentially.

```clojure
(let [[fst _ thrd] "bar"] (str fst thrd))
```

You can even combine destructuring with the variable arguments `&` trick to collect an indeterminate number of arguments into a list and then immediately destructure them. Though [Stuart Sierra says this is a bad idea](https://stuartsierra.com/2015/06/01/clojure-donts-optional-arguments-with-varargs).

```clojure
(defn add-first-and-third-uncollected [& [a _ b]]
(+ a b))
(add-first-and-third-uncollected 1 2 3 4)
```

Note how this works even though the numbers aren't collected into a vector.  Incidentally, you can also use the fancy ampersand *within* a destructuring statement to collect the tail of a sequence.

```clojure
(let [[v1 v2 & the-rest] (range 10)]
[v1 v2 (apply + the-rest)])
```

While we're at it, let's look at nested data structures. First, we'll use the previous technique to make a vector with sequences in them. And, actually, let's learn a new technique in the process: you can also use `:as` in any destructuring operation to bind the entire destructured data structure to a name.  So:

```clojure
(def nested (let [[v1 v2 & the-rest :as everything] (range 10)]
[v1 v2 the-rest everything]))
(identity nested) ; to get it to display in klipse
```

Now let's grab... say... the 1 from the first two numbers, nothing from the first sequence and the 2 from the second sequence. 

```clojure
(let [[_ theone _ [_ _ thetwo]] nested] (str theone thetwo))
```

That's a little obscure-looking, here's a clearer one, fetching the items at the same positions: 

```clojure
(def nested2 [:a :b [:c :d] [:e :f :g]])
(let [[_ theone _ [_ _ thetwo]] nested2] (str theone thetwo))
```

So what you can see is that the inner vector in the let statement essentially said "let's start destructuring!"  And then from the first four values of the vector that we destructured (`:a:` `:b` and `[:c :d]`) we took only the second---so we threw out the first nested vector as a discard value. Then, continuing through the vector, we got to the second nested vector. Since we wanted to fetch something out of that, we put another inner vector in to say "let's destructure this vector now." And then we grabbed the third item in there.

You can also go a little crazy with the nesting and get maps inside vectors, and so on and so on.  Here's something a little ridiculous...

```clojure
(def ridiculous ["foo" {:bar "baz" :bat [1 2 3]}])
(let [[_ {[_ mynum _] :bat}] ridiculous] mynum)
```

See what I did there?  I took the second item in the vector, which was a map, grabbed the thing attached to `:bat`, which was a vector, and then grabbed the second item in it.

There are more fancy destructuring features, such as the :or keyword to supply default bindings in map destructuring, but this is enough for now. Go read the further reading for all of that. 


## Comparison: Python Tuple Unpacking

For those coming to Clojure from Python, it's interesting to compare destructuring to one of my favorite syntactic features of the latter.  

A classic programming interview-type problem is how to swap the values of two variables without using a third variable. Most solutions involve [silliness with arithmetic](http://www.geeksforgeeks.org/swap-two-numbers-without-using-temporary-variable/), but in python it's as simple as: 

```python
a = 0
b = 1
a, b = b, a
print("a = " + str(a))
print("b = " + str(b))
```

The closest equivalent I can come up with to this in Clojure puts the variables in a vector to be destructured, viz: 

```clojure
(def v [0 1])
(def v (let [[a b] v] [b a]))
(identity v)
```

or as a map, which can do the swapping right in the destructuring statement:

```clojure
(def ab {:a 0 :b 1})
(def ab (let [{a :b b :a} ab] {:a a :b b}))
(identity ab)
```

Though I'm not sure if either technically respects the "no temp variables" rule since they do create temporary bindings in the let statements.  But both versions have the same general form of "unpack the first two variables, swap them without storing either one in a third."  And there's probably a more elegant way to do it. (Something to try at home: write a macro to do this to two variables on their own.)

But anyway, the python idea works the same.  The trick is that the [comma creates a tuple](https://www.tutorialspoint.com/python/python_tuples.htm), and then the [assignment unpacks that tuple from left to right](http://stackoverflow.com/a/14836456/4386239). So the left side of the assignment statement reaches in, and assigns a and b to the elements of the tuple on the right side, which you just created with the values in b and a. Though I think some of this stuff got screwed with in Python 3.


## Further reading

- [Official Clojure guide](http://clojure.org/guides/destructuring)

- [A more abbreviated official Clojure explanation](http://clojure.org/reference/special_forms#binding-forms)

- [Jay Fields tutorial](http://blog.jayfields.com/2010/07/clojure-destructuring.html)

- [John Louis Del Rosario tutorial](https://gist.github.com/john2x/e1dca953548bfdfb9844) and [blog post](http://www.john2x.com/blog/clojure-destructuring.html)

- [Bruno Bonacci tutorial](http://blog.brunobonacci.com/2014/11/16/clojure-complete-guide-to-destructuring/)

- [ClojureBridge destructuring examples](https://clojurebridge.github.io/community-docs/docs/clojure/destructuring/)

- [Clojure for the Brave and True on defining functions](http://www.braveclojure.com/do-things/#Defining_Functions) (scroll down a bit for destructuring section)

- [Sebastian Hennebrueder on destructuring](https://www.laliluna.de/articles/2013/010/29/clojure-destructuring.html)



