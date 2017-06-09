{:title "Use Someone Else's React Component in Reagent"
 :layout :post
 :date "2017-04-05"
 :executable false
 :tags  ["clojurescript" "clojure" "javascript" "react" "reagent" "web"]}
 
Today's quest: I have a bunch of [reagent](https://reagent-project.github.io/) projects that involve displaying tabular data. Reagent, for those not in the cool kid club, is a clojurescript wrapper for React. I just learned, thanks to HN, about this awesome-looking new React library (do we call them libraries in react-land?  or just components?) to display a spreadsheet-like interface, [https://github.com/nadbm/react-datasheet](react-datasheet).

The problem: I know jack-all about React, or indeed about Javascript beyond the basic semantics of the language.  I have zero investment in the React toolchain, or indeed the Javascript toolchain.  The only reason I use Reagent in CLJS is because I really like the deref-an-atom-to-update-UI model of state change. Not because I particularly want a virtual dom or anything like that. 

And the Javascript toolchain is, frankly, nuts. Apparently it doesn't even come with a module system (maybe it does with the 2016 version, which sometimes gets called ES6 and sometimes get called ES2016, or maybe those are different things, who the hell knows? Or maybe there's also ES7, and something called Javascript Next, and what the fuck?  Stop it, Javascript, you're drunk.), so there are, like, [dozens of module systems](https://github.com/nadbm/react-datasheet) and compilers and transpilers and apparently a package system built on Node that you still use even if you're not running your server on node and oh my god what? 

Aside: I don't think it's a coincidence that the languages I like most, Clojure and Python, both have a strong BDFL-figure to keep things relatively sane. As much as people often disagree with their decisions, I take it that if people felt the need to produce dozens of totally different third-party solutions to a basic language feature like a module system, either Rich or Guido would quickly step in, bring the best one into the language, and the culture would quickly coalesce around people damn well using that. It's truly ridiculous that you can have `pip install foo` and `import foo` in Python, but that the latest version of Javascript apparently has a [complex module loader API](http://exploringjs.com/es6/ch_modules.html)

Ok, so anyway. I'm gonna try to do this thing. In roughly three steps: 

1.  Get the Javascript bits into my project in a form that Clojurescript might be able to use.

2.  Get Clojurescript to actually see the Javascript bits.

3.  Get the Reagent bits of the Clojurescript bits talking to the React bits of the Javascript bits.

I first tried the tutorial [here](http://blob.tomerweller.com/reagent-import-react-components-from-npm), which just puts everything into a NPM thingie, compiles it with webpack, and then excludes reagent, but I got a classic javascript-compilation-hates-you error in the webpack bundle.js ("Uncaught TypeError: r is not a function"), and I don't even begin to know how to debug that.  So screw it, doing it some other way.  That first fail is [this commit](https://github.com/paultopia/reactinreagent/commit/a8edc242652fbcfdb6545d7447133991bd7ca732), for folks following along at home.

## First Hurdle: Getting the library/component

The docs for React-Datasheet assume that you have some kind of compilation process, presumably the normal compilation process for React, so all they do is tell you to install it via NPM. Needless to say, I'm not using NPM. 

So let's start a new clojurescript project---no point in breaking an existing project by doing this.  It'll live [https://github.com/paultopia/reactinreagent](here), in case you want to follow along.  And then I guess the basic strategy will be to write myself an entry-point in Javascript that just provides the component (class?  apparently [there's no difference](https://reactjsnews.com/composing-components) between react components and react classes?), and then I can call my own JS from CLJS, and include that JS directly in my project and hopefully not need externs or something...

1.  Create cljs project using my favorite template: `lein new reagent-frontend reactinreagent`

2.  Create a javascript project somewhere in the repo.  I don't want it to be on the classpath and compile a bunch of extra garbage in my clojurescript, so I'll just create a directory called "reactstuff" at the top level and put all my javascript in there. My hope is that after I sort out all the react stuff there'll just be a nice compiled/transpiled/javascript-magiced js file that I can dump into my cljs resources and treat like an external javascript library.

```
mkdir reactstuff
cd reactstuff
npm install react-datasheet --save
```

Not even gonna try to figure out what needs to happen to keep this stuff off github.  Don't care, no struggs.

well, that threw a truly astonishing number of warnings and errors: 

```
/Users/myuser
├── UNMET PEER DEPENDENCY react@~0.14.8 || ^15.0.0
├── react-datasheet@1.2.2
└── UNMET PEER DEPENDENCY react-dom@~0.14.8 || ^15.0.0

npm WARN enoent ENOENT: no such file or directory, open '/Users/pauliglot/package.json'
npm WARN react-datasheet@1.2.2 requires a peer of react@~0.14.8 || ^15.0.0 but none was installed.
npm WARN react-datasheet@1.2.2 requires a peer of react-dom@~0.14.8 || ^15.0.0 but none was installed.
npm WARN myuser No description
npm WARN myuser No repository field.
npm WARN myuser No README data
npm WARN myuser No license field.

```

Ok, so I guess I need to install lots of other stuff to make this happen?  I suppose installing all the react crap into this other directory won't pollute my clojurescript project, and hopefully it'll all come out right at the other end.  Let's pray.  (But now I definitely need to add this to the .gitignore). Let's see what happens.  

```
npm install react --save

npm install react --save
npm WARN saveError ENOENT: no such file or directory, open '/Users/myuser/package.json'
/Users/myuser
├─┬ react@15.4.2
│ ├─┬ fbjs@0.8.12
│ │ ├── core-js@1.2.7
│ │ ├─┬ isomorphic-fetch@2.2.1
│ │ │ ├─┬ node-fetch@1.6.3
│ │ │ │ ├─┬ encoding@0.1.12
│ │ │ │ │ └── iconv-lite@0.4.15
│ │ │ │ └── is-stream@1.1.0
│ │ │ └── whatwg-fetch@2.0.3
│ │ ├─┬ promise@7.1.1
│ │ │ └── asap@2.0.5
│ │ ├── setimmediate@1.0.5
│ │ └── ua-parser-js@0.7.12
│ ├─┬ loose-envify@1.3.1
│ │ └── js-tokens@3.0.1
│ └── object-assign@4.1.1
└── UNMET PEER DEPENDENCY react-dom@~0.14.8 || ^15.0.0

npm WARN enoent ENOENT: no such file or directory, open '/Users/myuser/package.json'
npm WARN react-datasheet@1.2.2 requires a peer of react-dom@~0.14.8 || ^15.0.0 but none was installed.
npm WARN myuser No description
npm WARN myuser No repository field.
npm WARN myuser No README data
npm WARN myuser No license field.

```

Ok, so that ain't working.  Apparently I do need, like, an entire fucking react toolchain to use this component.  Let's see what the [react docs](https://facebook.github.io/react/docs/installation.html) have to say about getting one, huh? 

Ok, apparently I need to:

```
npm init
npm install --save react react-dom
```

oh god, npm init is some silly walkthrough that asks you a bunch of questions to create a package.json.  save me?  I'm just taking all the defaults. 

But ok!  Now it'll let me install the things, with only one warning: `npm WARN reactstuff@1.0.0 No repository field.` --- and I'm perfectly happy with this warning, since I don't feel the need for NPM to know where my repo is.

So, next thing, I guess the default for npm.init was to create an entry point at index.js, so I'll do that. 

Hmm.  I'm not actually sure where index.js goes in order to get this stuff to all compile. I guess I need to use webpack?  But how do I get webpack and the npm stuff to play together?  Here's [a really fucking long tutorial for that](https://scotch.io/tutorials/setup-a-react-environment-using-webpack-and-babel), jesus.

No.  This is a horrifying mess.  I'm not going to set up an entire javascript build system for this.  (forget callback hell, how about dependency and build system hell?)  Instead, I'm going to empty the directory and start again, this time using facebook's lazyperson tool, [create-react-app](https://github.com/facebookincubator/create-react-app). 

```
cd ..
rm -rf reactstuff 
mkdir reactstuff
cd reactstuff 
npm install -g create-react-app
create-react-app godhelpme
cd godhelpme
npm install react-datasheet --save
```

Ok, so first thing I'm gonna do is try to get it going with pure javascript, with a placeholder file. 

http://stackoverflow.com/questions/35489797/using-react-components-in-reagent#35734001
