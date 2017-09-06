{:title "I think I've figured out how to change text encodings in the browser. I'm not sure, because it's text encodings, but maybe?"
 :layout :post
 :date "2017-09-05"
 :executable false
 :tags  ["javascript" "web" "unicode" "browsers" "utf-8" "latin-1"]}
 
So I'm working on a slightly irrational project, namely, extracting citations from MS-Word formatted law review articles and generating bibtex download + on-screen display, in pure client-side javascript.  

This has a lot of steps to it, but the browser is a surprisingly good platform for doing it (and an unbeatable platform for delivery).

Basically, a docx file is a zip file with a different extension, and each of the files inside is an XML file encoded in Latin-1 (or the [slightly different windows-1252](https://stackoverflow.com/a/19111140/4386239), but either way, not nice sensible UTF-8, which is why you get a bunch of [mojibake](https://en.wikipedia.org/wiki/Mojibake) whenever you copy text from Word into anything else).  So this means there are three steps to making it into something useful:

Step 1: Get the contents unzipped out of the docx format.  [Zipjs](https://gildas-lormeau.github.io/zip.js/) is a very good zip library that handles this task with no problem, it's as simple as hanging a listener on a file upload form that converts to a blob and then passes to `zip.BlobReader`.

Step 2: Parse the XML.  It might be surprising, but it shouldn't be, to learn that the browser is a really good platform for XML parsing. Since, after all, parsing XML is [basically the same task](https://softwareengineering.stackexchange.com/questions/93296/relation-and-differences-between-sgml-xml-html-and-xhtml) as parsing HTML. It's as straightforward as:

```javascript
function parseXML(xmlstring){
    var parser = new DOMParser();
    return parser.parseFromString(xmlstring, "text/xml");}
```

and then you can use normal DOM methods and attributes to navigate and get things from the XML, just as if it were a HTML page.

Step 3. Display some intermediate results on a web page. You've got strings, display them.

Step 4. Convert the crazy windows encoding to something more rational. This is the fun part. 

Ideally, we'd do this before step 2, but we can't have everything. As it turns out, the browser is perfectly capable of identifying cray Microsoft encodings when they come from a docx file and parsing the XML/displaying them properly on screen without conversion. I'm not sure whether this is Zipjs or something in Chrome performing this magic (or being told to do so by something in the docx/underlying XML), but it works, so I'm not going to look a gift horse in the mouse.

(Incidentally, if you open up the Javascript console and look at `document.characterSet`, you'll see the character encoding Chrome thinks it's using; for text parsed from at least the docx on my machine it sees windows-1252, and manages to display special characters correctly, including the usually problematic ones like en-dashes.)

However, like I said, I want to ultimately give the user a bibtex file to download. Maybe other downloads too. And I simply will *not* have those things be in windows-1252 or latin-1 or any other such ridiculousness. UTF-8 or bust. 

It turns out that there's no good information on how to do this in the browser. There are a couple of [obsolete Stack Overflow answers](https://stackoverflow.com/a/5396742/4386239) that essentially tell you to use `decodeURIComponent(escape(string));` or maybe `unescape(encodeURIComponent(originalstring));`, but neither actually works. 

Fortunately, it looks like you can coerce a string encoding change in the browser Blob constructor. (At least, this works on my machine, in the latest version of Chrome). 

So here's my solution. This *appears* to work to take an in-memory string in windows-1252 and produce a link to a downloadable text file in UTF-8, as desired: 

```javascript
function experimentalUTF8Download(latin1string){
    var b = new Blob([latin1string], {type: "text/plain;charset=UTF-8"});
    var linkurl = URL.createObjectURL(b);
    var a = document.createElement('a');
    var linkText = document.createTextNode("foo");
    a.appendChild(linkText);
    a.href = linkurl;
    document.body.appendChild(a);
    }
```

The key is in line 2: you coerce the character set of the Blob you're creating to be UTF-8.

Like I said, this *appears* to work. Because there isn't exactly a direct way of telling, I used a couple of different methods to get what I consider to be circumstantial evidence that this is the correct encoding: 

1.  When I open the resulting file in the browser, it comes out garbled (with mojibake in the appropriate places, like those en-dashes I mentioned before), and `document.characterSet` is still "windows-1252". I infer from this that Chrome doesn't know to change the characterset from the previous html file, but it can't parse the new file---because it isn't windows-1252 any more!

2.  Reading the file in Python3 with no arguments to the `open()` constructor [defaults to your platform's default characterset](https://docs.python.org/3/library/functions.html#open) (the output of `locale.getpreferredencoding()`), which in my case is utf-8. 

I can read the file in Python3 without passing any arguments, i.e., 

```python
with open("foo.txt") as f:
    foo = f.read()
foo
```

and the en-dashes come out looking right in the REPL. 

3. When I try to open the file as windows-1252, Python3 throws a `UnicodeDecodeError` error, and when I try to open it as latin-1 (the other obvious suspect) it doesn't throw, but it does mojibake at me. 

These three pieces of evidence seem to very strongly suggest that I've successfully coerced the encodings in the Blob for download.  So: YAY!! Take that, obsolete Microsoft encodings!
