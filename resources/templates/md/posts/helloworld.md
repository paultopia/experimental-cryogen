{:title "Cryogen + Github pages + Klipse"
 :layout :post
 :date "2016-10-17"
 :executable true
 :tags  ["meta" "clojure" "clojurescript"]}

Hello world, and all that jazz.  I've decided to start a tech blog. 

The first challenge: getting it up. Static site generators seem to be fashionable these days, and I'm a big fan of Clojure, so naturally I went with [Cryogen](http://cryogenweb.org/), which is a dead simple static site generator where you literally fire up a lein template and then edit a bunch of markdown files and simple [Selmer](https://github.com/yogthos/Selmer) templates plus a single config edn. And [Github Pages](https://pages.github.com/) is both free and convenient name recognition for code-oriented things, so that's the obvious choice for a host.

The main alternative I considered was [Hakyll](https://jaspervdj.be/hakyll/), mainly because I've been meaning to try to do a Haskell project. But taking one look at the code in the "create compilation rules in a Haskell EDSL" snippet on the front page, and, nopesauce. As much as I'm starting to like Haskell (more later), I still immediately get turned off by the weird [special character-heavy syntax choices](https://twitter.com/PaulGowder/status/783886798350987264). So when I saw `>>=` and `.||.` that was it for me. (Maybe when I finally write my personal course management system.)

So challenge #1 is getting it on github. The official Cryogen docs aren't terribly clear on this point, but github personal/organizational pages appear to want to have the static content at the root of the repo, and Cryogen outputs the content it produces to /resources/public. You can fix this by setting the repo at resources/public rather than in the root directory of the Cryogen project, but then the markdown files, Selmer templates, etc. aren't under version control, and that's obviously a problem.

Fortunately, a nice chap named Mayank Jain cooked up [a solution](http://firesofmay.com/posts/2015-08-26-setup-cryogen.html). The heart of the fix is to create one repo at the root of the Cryogen project, call it "my blog" or something, and then create a second repo, *inside the first one*, with the standard yourname.github.io naming, starting at /resources/public. 

To be honest, this seems super-shady to me. The official way to nest repositories in one another in git is to use a [submodule](https://git-scm.com/docs/git-submodule), but just try to read the documentation for that, I dare you. So what happens if you actually nest repos like this without using submodules?  Does everything horribly break?  Well, not so far... 

Jain, as well as [the person who first discovered this trick but has a less good explanation](https://github.com/tangrammer/cryogen-blog/blob/master/resources/templates/README.md#instructions-to-make-changes), recommend putting /resources/public in the .gitignore of the original repo, presumably as a safety measure to prevent all kinds of weird conflicts from happening. Honestly, I didn't bother. And it still seems ok... I haven't horribly blown up anything so far. (Heaven help me if I try to do anything even remotely fancy with branches, or checking out old commits, or something.)

The second challenge is getting Klipse running. For those who don't know, [Klipse](https://github.com/viebel/klipse) is an amazing embedded clojurescript evaluation environment, basically a modular repl in a page: you can replace code snippets with actual executable code, and even do things like fetch libraries. It is awesomesauce. It also has interpreters for (officially) Javascript, Ruby, and PHP in addition to clojurescript; unofficially it also has Python among others. 

After some hassle, integrating Klipse with Cryogen actually turned out to be dead simple. In Cryogen, all the Selmer templates are stored in subdirectories of your themes, so all I did was [edit base.html](https://github.com/paultopia/experimental-cryogen/blob/master/resources/templates/themes/nucleus/html/base.html) there to add references to the Klipse javascript source.  Viz: 

```
{% if post.executable %}
{% style "css/codemirror.css" %}
<script>
 window.klipse_settings = {
     selector: '.clojure',
     selector_eval_python_client: '.python'
 };
</script>
{% script "js/klipse_plugin.js" %}
{% script "js/skulpt.min.js" %}
{% script "js/skulpt-stdlib.js" %}
{% endif %}

```

A few points to note there: 

1.  The selectors are classes that can be added to a fenced code block in the markdown file just by putting the name of a class directly after the opening symbol, i.e., `~~~clojure`

2. post.executable is a variable that I set at the top of the markdown files. So what the template does is chooses whether or not to include the Klipse stuff depending on whether I've said, in a given post, that there's executable code in it. The rationale here is that all this extra javascript is really heavy, and I don't want people to have to load it if they're just visiting the site to look at a page that doesn't use it.  I'll probably modify this down the road to take a different variable for each language, and then only load the interpretation environment appropriate to that language.

3. Because Python is still an unofficial part of Klipse, this selector isn't actually documented yet, but it's in there and I'll prove it: 

```python
print map(lambda x: x + " World!", ["Hello"])
```

See?

4.  I'm locally serving the Klipse javascript rather than using the one provided by the (awesome) maintainer of the plugin because the provided version doesn't support https. However, it turns out there's [a workaround](https://github.com/viebel/klipse#https), and I'll probably switch to that, because the maintainer told me that he's pushing out frequent releases, and I want to keep up to date.

Other matters of note: 

1. Depending on what languages you want to use, you might need a special build of the highlight.js javascript that comes with cryogen. [Get it here](https://highlightjs.org/download/).

2. All this mucking around with multiple repos and such really demands [a build script](https://github.com/paultopia/experimental-cryogen/blob/master/deploy).  Mostly derived from Jain's. 

3. Note how you can compile Cryogen with `lein run`. This isn't actually in the documentation anywhere, which just recommends compiling with `lein ring server` and actually running a whole webserver (plus auto recompile). But this seems annoying and wasteful to me when you can just add a compilation step to the build script. 

Now let's have a fizzbuzz.

```clojure
(take 100 (map #(cond (= (mod % 15) 0) "fizzbuzz" (= (mod % 3) 0) "fizz" (= (mod % 5) 0) "buzz" :else %) (range 1 101)))
```

bye.
