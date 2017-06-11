{:title "Python and R Resources for text-mining"
 :layout :post
 :date "2017-06-08"
 :executable false
 :tags  ["python" "r" "text-mining" "machine-learning" "datascience"]}
 
This is just a growing collection of useful Python and R packages and resources for text-mining. It's the flavor of ML I'm working on the most, so this is mostly just for my own hassle-avoidance. 
 
Work in progress.
 
# Python
 
## Libraries

- [NLTK](http://www.nltk.org) --- the 1000-pound gorilla here.

- [Gensim](https://radimrehurek.com/gensim/) --- very frequently used topic modeling library, also has word2vec wrapper that seems to get a lot of usage.

- [Textmining](http://www.christianpeccei.com/textmining/) -- a very small library that creates DTMs and stuff.  A nice alternative to wading through tons of NLTK documentation.

- [spaCY](https://spacy.io) --- NLTK alternative, I've not tried it.

- [TextBlob](https://textblob.readthedocs.io/en/dev/) --- not used it, but it looks to do a very significant subset of common text processing tasks with a relatively rational-looking API.

- [BeautifulSoup](https://www.crummy.com/software/BeautifulSoup/bs4/doc/) --- the standard python library for getting your texts out of web pages (agonizingly complicated API, but, then again, that's probably the DOM's fault). Incidentally, you should really be using [requests](http://docs.python-requests.org/en/master/) to actually send http requests for scraping.

- [Scrapy](https://scrapy.org) --- another important Python webscraping tool, I've actually never used it.

- [wordcloud](https://github.com/amueller/word_cloud) --- word clouds are always fun.  Kinda useless, but fun.

- [Pattern](http://www.clips.ua.ac.be/pattern) --- a library that combines web-scraping with some standard NLP tools.

- [CoreNLP Wrappers](https://stanfordnlp.github.io/CoreNLP/other-languages.html#python) Python wrappers for Stanford Core NLP.

## Publications, Tutorials, etc.

(to be added)
 
# R
 
## Libraries

[TM](https://cran.r-project.org/web/packages/tm/index.html) --- The classic package, but I hate it like sin. On the other hand, its agonizingly horrible API is the source of my [most-upvoted Stack Overflow answer](https://stackoverflow.com/questions/24771165/r-project-no-applicable-method-for-meta-applied-to-an-object-of-class-charact/29529990#29529990), so, thanks?  This package and strings-as-factors together drove my abandonment of R for Python.

- [Quanteda](https://github.com/kbenoit/quanteda) --- Ken Benoit's TM alternative.

- [tidytext](https://github.com/juliasilge/tidytext) --- I haven't used this yet, but given the authors (Robinson and Silge), it's probably amazing.

- [ToPan](https://github.com/ThomasK81/ToPan) --- a cool batteries included topic modeling Shiny app specifically designed for topic modeling in ancient languages.

- [LDA](https://cran.r-project.org/web/packages/lda/index.html)

- [Structural Topic Model](http://www.structuraltopicmodel.com) -- Molly Roberts, Brandon Stewart and Dustin Tingley package widely used among political scientists.

- [Topicmodels](ftp://cran.r-project.org/pub/R/web/packages/topicmodels/vignettes/topicmodels.pdf)

- [wordcloud](https://cran.r-project.org/web/packages/wordcloud/)

- [WordVectors](https://github.com/bmschmidt/wordVectors) --- word2vec implementation.

## Publications, Tutorials, etc.
 
- [Introduction to Text Analysis Using R](https://github.com/kbenoit/ITAUR) --- Benoit
 
- [Text Mining with R, A Tidy Approach](http://tidytextmining.com) --- Julia Silge and David Robinson, open web version of their book.

- [A gentle introduction to text mining using R](https://eight2late.wordpress.com/2015/05/27/a-gentle-introduction-to-text-mining-using-r/) --- Kailash Awati

- [Topic Modeling of Historical Languages in R](http://www.dh.uni-leipzig.de/wo/topic-modelling-of-historical-languages-in-r/)

 
