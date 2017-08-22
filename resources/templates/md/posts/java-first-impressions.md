{:title "Java: First Impressions"
 :layout :post
 :date "2017-08-22"
 :executable false
 :tags  ["java"]}
 
 I'm learning Java, for two reasons. 

First, I really like Clojure, but I constantly run into barriers where the only way to do something is to [drop down to Java code that I don't understand](https://groups.google.com/forum/m/#!topic/clojure/gBd6-kY13vA).  

Second, I feel like I should know at least one strongly typed language, and I honestly find Haskell annoying (it's no fun to fight the compiler all the time or to [have to use four libraries, two language extensions, and two monads to read a damn csv](https://github.com/hvr/cassava)). That kind of limits my options to some super-low-level systems programming language like C/C++/etc., something like Haskell but slightly different (some other ML-family language), Swift, or Java. Since I don't want to manage memory by hand, and don't really feel like I have a good use for Swift right now, Java it is. 

So I think I actually... like Java?  [They might take away my functional programmer card for this](https://steve-yegge.blogspot.com/2006/03/execution-in-kingdom-of-nouns.html), but I do.  I'm trying to do the [Matasano Crypto Challenges](http://cryptopals.com) with it, by way of learning, and so far (only a few challenges in) it's actually going ok. Here are my first impressions..

# Java doesn't have a lot of magic

I find that getting Python to do low-ish-level stuff is surprisingly hard. For example, if you want to actually see a string represented as a list of bytes, it involves digging around into libraries that nobody really uses, and you run into issues between Python 2 and Python 3, and, in all honestly, I still don't know how it all works. Like, Python 2 represents all strings internally as bytes by default, but supposedly Python 3 represents strings as unicode internally by default, but *since unicode is also just a bunch of bytes, what does that even mean?*  What it seems to mean in practice is that it's really hard to get to see the bytes.  If, for instance, you follow [this SO](https://stackoverflow.com/questions/7585435/best-way-to-convert-string-to-bytes-in-python-3) and do `'foo'.encode()`, what you get back, at least as it's representated at the repl, is `b'foo'`.  Which is pretty useless.  (What you really need to do is something like ` [int(x) for x in 'foo'.encode()]`.)

By contrast, Java doesn't try to hide the fact that strings are just arrays of characters, which are just numbers, from you.  So it's seamless to convert back and forth between different representations of strings.  In Java, you get a byte array from a string by `byte[] mybytes = "foo".getBytes();`, and you can see it by calling a simple array method, rather than using a weird int coercion like python: `import Java.util.Arrays; Arrays.toString(mybytes);`, which seems much clearer and more straightforward to me.  And then getting a string back out of it is as simple as `new String(mybytes);` 

That straightforwardness makes it nice and easy to do things like implement [a utility class](https://github.com/paultopia/nounnounnoun/blob/b763d62e09ced4c6f83b4a43055ad859214d3b5c/cryptopals/Stringform.java) that works directly with the byte representation internally bit-twiddling crypto operations, but then spits out the string representation in any form one might want, without having to root through a bunch of stuff that the language usually wants to hide.  For Python, the best that I can come up with is `''.join([chr(x) for x in mybytes]`, and, again, that seems less honest to me---a string just is an array/list/whatever of bytes/ints, so why can't I turn the whole thing into a string at once, other than because Python doesn't want me to think about strings that way?  (Apparently [there is a way to do this using the array library](https://www.python.org/doc/essays/list2str/), which I hadn't even heard of until now, because, again, Python doesn't exactly advertise its existence.)

So three cheers for less magic!  (And this is making me wonder if I should really be writing C...)

# This thing is fast!

To be fair, I haven't asked it to do anything actually difficult yet. But the [4th crypto challenge](http://cryptopals.com/sets/1/challenges/4) is at least a *little bit* of computational effort---it does i/o and also has a healthy number of loops to go through; I would have expected like a quarter second hiccup, but Java spits out the answer instantly.  I can't wait to see how it works on more computationally intensive stuff.

# Java has a lot of types.

The primitive types like int, char, all that good stuff, all have ["boxed" versions that serve as objects](https://docs.oracle.com/javase/tutorial/java/data/autoboxing.html).  I assume this is for perf reasons, but it's a little odd.  Thankfully, for the most part, things seem to be pretty seamless here.  

Here, Java does seem to do a little unwelcome magic sometimes.  For example, here's a simplified version of my code to xor two byte arrays, assuming this method belongs to an object that has a byte array stored in `byte_array`: 

```java
public byte[] xor(byte[] other){
    int size = byte_array.length;
    byte[] result = new byte[size];
    for (int i = 0; i < size; i++){
      result[i] = (byte)(byte_array[i] ^ other[i]);
      }
    return result;
}
```

The cast in line 5 is necessary because Java will happily xor two bytes, but will silently up-cast the outcome to an int, and then will throw if you try to stick it in a byte array.  Which seems like a bit of a WTF to me, but ok.

The other thing is that Java has a *lot* of collection types.  For example, there's both a HashMap and a TreeMap---[actually, there are others too](https://stackoverflow.com/questions/2889777/difference-between-hashmap-linkedhashmap-and-treemap)---with different constraints and performance characteristics. And you have to choose between them, you can't (I don't think) just ask for a map and let the compiler choose. 

Sometimes this feels like another primitive/object distinction. For example, there's an Array, which has a literal, and then you can have an ArrayList with the same contents, and not only are these different in terms of implementation ([fixed-length vs variable length](http://www.geeksforgeeks.org/array-vs-arraylist-in-java/), etc), but they also have totally different methods available, and sometimes you have to cast between them in order to get at the desired behavior.  Again, this is something that the languages I'm more familiar with (Python, Clojure) have available buried away, but don't really stick in your face---both of my main languages really expect you to use a small handful of data structures almost all the time (Python: list, dict; Clojure: vector, map, very occasionally list or queue).

But I don't actually mind this too much.  Partly because I rather suspect that it has a lot to do with the previous point about performance...


# Java is weirdly picky about filenames and directories

This is something that just straight-up annoys me.  Java has this concept of a "classpath" that is nowhere really clearly explained. As far as I can from trial and error and reading SO answers, I'm developing a (doubtly highly inaccurate) mental model of the classpath as essentially a brittle virtual filesystem where things start blowing up if everything isn't exactly where it's expected, named what it's expected to be.  (And I can't find anywhere where all the naming and putting things in directories rules are written down.) 

For example, I had a two-hour battle today with the following task: I had the entry point of my code outside the "package" folder where all of the classes doing the work were, and I wanted to move it inside that folder.  Turns out that meant adding package declarations, and removing import declarations, and adding a class to the name of the file I was running... it's crazy.

Incidentally, weird thing I learned in the process of that---the `java` command line tool doesn't actually take the filename of the class you're tying to invoke as an argument.  It takes the classpath representation based on aforementioned brittle virtual filesystem.  So, for a [toy problem](https://stackoverflow.com/questions/45823179/java-newbie-class-with-main-class-in-same-folder-as-subclasses-doesnt-compile?noredirect=1#comment78607107_45823179) I was working with to sort this problem out, I ended up compiling to a file where the entrypoint was `DoTheThing.class`, and the command to invoke it wasn't `java DoTheThing` or `java DoTheThing.class` but `java mypackage.DoTheThing`---because I'd put the entry point class inside a package declaration---and then java used this classpath thingey in order to find the main method in there.  

Here, I'm not at all happy with Java.  I don't mind the Kingdom of Nouns stuff, but this classpath stuff is bloody terrible and I hate it.  Absolutely my least favorite thing about Java, and why the next thing I'm going to do is go learn Gradle so as to offload all this ugliness to a build tool.

# Java People are really unfriendly on Stack Overflow

When I [asked](https://stackoverflow.com/questions/45823179/java-newbie-class-with-main-class-in-same-folder-as-subclasses-doesnt-compile) about how to make the file moving thing work on SO, within about 2 minutes I had 3 downvotes and 2 hostile comments.  Now, I'm not a SO super-user, but I've been using it for a while, I know how to ask a decent question, and while I don't have a ton of reputation, I at least have 700-some, which is more than total beginner.  So I don't think I asked a total boner of a question.  I think that Java people on SO are just much, much more unfriendly than Python/Clojure/Javascript/R people (my usual SO crowd). 

The snotty functional programming person in me wonders if maybe this is because PG is right, and Java is full of [blub programmers](http://www.paulgraham.com/avg.html) who have successfully alienated the SO crowd over the years with terrible questions.

Anyway, I ultimately rewrote the question and got enough hints to sort the problem out, but it was a shocking blast of cold water in the face to get there, and felt quite weird, to be honest. 

There's undoubtedly an explanation somewhere in the middle, i.e., neither "I somehow forgot how to ask SO questions" nor "Java people are just bitter jerks."  But I'm not sure what that explanation is.  Maybe "Java people tend to be using the language at work, as opposed to for fun, so they tend to be more busy and impatient?"  That seems like a pretty plausible guess.
