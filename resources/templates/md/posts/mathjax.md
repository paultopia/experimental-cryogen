{:title "Getting Mathjax to Play Nicely with Markdown and Highlight.js", :layout :post, :date "2017-07-24", :executable false, :mathy false, :tags ["meta" "markdown" "javascript" "math" "cryogen" "web"]}

Mathjax and markdown tend to fight with one another a bit.  When I started blogging math notes on here, the combination of Mathjax and Cryogen's markdown parser and Highlight.js fought with one another a lot.  So here's a quick tutorial on fixing it.

The assumption here is that you want to write in normal markdown, i.e., the kind of thing that you could convert to a PDF with pandoc.  And you want to write latex math.  But you observe that doing so blows up when you convert to html and use mathjax.  

My fix definitely works for me, using [Cryogen](http://cryogenweb.org/), but YMMV if you use some other markdown parser/static site generator. 

### Step 1: Basic Setup

Mathjax isn't set up out of the box to recognize the delimiters typically used in Markdown for latex blocks.  So you need to tweak the Mathjax config. [Here's how I did it](https://github.com/paultopia/experimental-cryogen/blob/master/resources/templates/themes/nucleus/html/base.html#L105):

```javascript
<script type="text/x-mathjax-config">
 MathJax.Hub.Config({tex2jax: {inlineMath: [['$','$']],
                               displayMath: [['$$','$$']],
                               processEscapes: true,
                               skipTags: ["script","noscript","style","textarea"]
 }});
</script>
```

Important parts: pass it the $ delimiters for both inline and display (block) math.  Also, change the skipTags setting, because the [default](http://docs.mathjax.org/en/latest/options/tex2jax.html) skips pre and code blocks, which you don't want --- the next step will have you putting latex in a code block, and if you don't change this setting, then Mathjax will decline to process those blocks.


### First Problem: Superscript and Such.  Stick it in a code block.

A number of the characters used in LaTeX (look, I gave it the silly capitalization! No more.) have their own meaning in Markdown; I have particular problems with superscript and subscript. One possible solution is to just escape them all, but that gets really ugly really quick.  An easier fix is just to stick everything in a code block. 

### Second problem: syntax highlighting. 

So if you also use Highlight.js, then it turns out that putting latex in codeblocks means that it'll try to identify the language (incorrectly) and add a bunch of highlighter classes for the css.  Which, naturally, again blows up mathjax rendering.  

The solution there is to slap a nohighlight class on the block-level code blocks (blessedly, highlight.js doesn't seem to tamper with inline code blocks). 

### Third problem: what if you want to mangle your markdown by hand?

All this stuff seems like extra typing.  I don't like extra typing.  So ultimately, what I did was write [a preprocessor](https://github.com/paultopia/experimental-cryogen/blob/master/preprocess-math.cljs) that takes a normal markdown file (plus the cryogen header information). It's really quite simple, it just loads the file, looks for a "mathy" header, and, if it finds one, sticks all the latex into appropriate code blocks.

Here are the guts:

```clojure
(defn fix-inline-math [post]
  {:text (replace (:text post) #"[^$](\$[^$]+?\$)[^$]" " `$1` ") :filename (:filename post)})

(defn fix-block-math [post]
  {:text 
   (replace (:text post) #"[^$](\$\$[^$]+?\$\$)[^$]" "\n\n```nohighlight \n $1 \n```\n\n")
   :filename (:filename post)})
```

Easy.
