{:title "How to Make Mustache.js Templates Play Nice with LaTeX"
 :layout :post
 :date "2017-06-11"
 :executable false
 :tags  ["mustache" "templates" "latex" "javascript"]}

I'm trying to achieve the holy grail and have a LaTeX cv that gets automatically updated from JSON, like [Jsonresume](https://jsonresume.org/), only with my own custom fields appropriate for someone who does the wild variety of things I do. I'm terrible at LaTeX, but there are plenty of templates floating around, and there's a great new project, [Latexresu.me](https://latexresu.me/), that appeared on show HN recently---it's awesome, try it---and I just used it to shamelessly steal some stubs to build from.

This is part of an overall website overhaul for me: I'm building an entirely new personal website using [Vue](https://vuejs.org) that will allow people to do things like generate bibibtex or ris entries for my publications, etc.---basically all kinds of technological overkill for what most people use a static site for. All client-side rendering, even binary files shoved into the webpack bundle as data URIs, all that ridiculousness. 

So, since I'm deep into javascript-land anyway, I decided to template the .tex file using [Mustache.js](https://www.npmjs.com/package/mustache) and compile it with [Node-Latex](https://www.npmjs.com/package/node-latex). 

It turns out, however, that Mustache does HTML-escaping on every string passed into it. For example, the string `foo/bar` in the JSON data that I'm trying to use to build my cv gets mangled into `foo&#x2F;bar` in the template. And this is, alas, totally unacceptable because `&` is a [reserved character](https://tex.stackexchange.com/questions/34580/escape-character-in-latex) in LaTeX.

The Mustache docs aren't terribly helpful on the subject. They give two ways to unescape strings, both of which are very inconvenient here: 

1.  Use a "triple Mustache" --- i.e. `{{{foo}}}`.  The problem is that if you're templating LaTeX, this isn't going to do a lot of good, because if you're in your right mind at all, you'll have created a custom delimiter, so as to not be forced to distingish between LaTeX curly brackets and Mustache curly brackets.  And there isn't a documented way to change what characters count as a triple mustache (though maybe you can?).

2. Add an ampersand before the string you want to unescape--- i.e. `{{&foo}}`.  This plays nicely with custom delimiters, but has the inconvenient result of forcing you to manually unescape every single string you put in the template---since if you're templating LaTeX, rather than HTML, chances are that you won't want to escape *anything*.

Fortunately, there's an undocumented solution.  After digging through [some](https://github.com/janl/mustache.js/issues/244) [issues](https://github.com/janl/mustache.js/issues/307) with people complaining about this behavior, it turns out that someone kindly decided [expose the function used to escape HTML to users](https://github.com/janl/mustache.js/blob/master/mustache.js#L622), so that you can just override it in client code. 

Since this isn't really documented anywhere, and since even the issues don't make clear how to use the fix that was eventually proposed and accepted (the proposed fix had user code modifying `Mustache.escapeHTML`, but user code actually needs to modify `Mustache.escape`), here's a quick example of how to sensibly use Mustache.js with LaTeX.

```javascript
const latex = require('node-latex');
const fs = require('fs');
const Mustache = require('mustache');

// First, let's change the delimiter so that we can use <<foo>> instead of {{foo}} in LaTeX documents.

Mustache.tags = [ '<<', '>>' ];

// Here's the fix! A simple function to override the escape function provided by Mustache.

Mustache.escape = text => text;

// The greeting would blow up LaTeX rendering if escaped, but now that we've overridden the escape, it will work just fine.
const data = {greeting: "Hello/World"};

const template = "\\documentclass{article}\ \n \\begin{document} \n <<greeting>> \n \\end{document} ";
const input = Mustache.render(template, data);
const output = fs.createWriteStream('hello-tex.pdf');

latex(input).pipe(output);
```

That will just get rid of HTML encoding.  But you're not quite done yet, if you want to use LaTeX.  What if the text you want to put into the template itself has LaTeX reserved characters?

Here's a more robust fix.  Instead of `Mustache.escape = text => text;` in the above, you can set the escape function to something that escapes LaTeX reserved characters. I've defined a `latexEscaper` below that can be dropped right in.:

```javascript
var matches = new Map([["\\", "textbackslash"],
                       ["~","textasciitilde"],
	                   ["^","textasciicircum"]]);

var messytext = "I shouldn't be escaped / \n I should be: $ \n and I should be a special LaTeX command: ^"

function latexEscaper(text){
		return text.replace(/[\\~\^%&$#_{}]/g, 
                    match => "\\" +  (matches.get(match) || match));}
		console.log(latexEscaper(messytext));
```

And then your template should produce valid LaTeX, or at least as valid as LaTeX ever is...

Now, if you'll excuse me, I think I go need to do a PR to turn this from undocumented into documented...
