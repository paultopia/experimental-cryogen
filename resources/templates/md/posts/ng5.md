{:title "Mathy Ng Lecture 5: generative learning algorithms, naive bayes", :layout :post, :date "2017-07-17", :executable false, :mathy true, :math-preprocessed true, :tags ["machine learning" "math"]}


## Ng Lecture 4: Newton's Method, Exponential Family Distributions, GLMs.

# Ng lecture 5: generative learning algorithms

## The idea of generative algorithms

Algorithms like logistic regression are like "try to find a straight line that separate the classes best." Those are discriminative learning algorithms.

By contrast, imagine an algorithm that finds all the examples of class x and builds a model of what those look, then finds all the examples of class y and builds a model of what those look like; classification then becomes "let's see what our new observation looks like."

So that's a generative learning algorithm.

More formally: 

Discriminative algorithm learns p(y|x) directly by learning a hypothesis function `$h(\theta)$` that outputs a label in the range of 0 to 1. 

By contrast, a generative algorithm models p(x|y) and p(y).  It builds a model of what the features look like conditioned on the class label.

Then Bayes rule steps in, because of course it does. 

## Gaussian discriminant analysis: 

Assumes input features are continuous-values random variables and p(x|y) is multivariate gaussian. 

Models the response as a bernoulli. 

Features are modeled as a separate distribution for each value of y, i.e., p(x|y=0) is one distribution with mean `$\mu_0$`  p(x|y=1) is another distribution with mean `$\mu_1$` 
Leaving off the likelihood derivations.  The practical implication is that GDA is like logistic regression, but has the stronger assumptions, and if those assumptions are satisfied, is likely to produce better performance.

This is something that's true of generative models in general: you have to make modeling assumptions about your features, and if those are right, then you've got a better fitting model. See pg. 7-8 of [notes 2](https://see.stanford.edu/materials/aimlcs229/cs229-notes2.pdf).  

It turns out that if p(x|y) = 1  and p(x|y) = 0 are both in an exponential family distribution (the same one?), p(y|x) is logistic.

Notation note: the squiggle brackets around the y=1 i the maximum likelihood estimates are indicator notation, they indicate that it's essentially a counter, that increments once for each time that the variable is 1.  In other words, the value within the summation of the squiggly thing is 1 if the appropriate y is 1 for y=1, and so forth.  So it's really just a bunch of averages.

Prediction it's in the lecture at 23:00ish.  You predict the value of y (argmax with respect to y) that maximizes P(y|x), which, by bayes rule and some algebra, turns into argmax y p(x|y)p(y). 

Essentially, you're building a model of p(x|y=1), a model of p(x|y=0), and fit a bernoilli to p(y) basically just by taking the average of y's incidences in the observation. 

Much more simple and straightforward version: what he gives us is just a closed-form solution, like the matrix multiplication in regression.  We have the maximum likelihood function (page 6 of [notes 2](https://see.stanford.edu/materials/aimlcs229/cs229-notes2.pdf)) and we seriously just plug our data into it.  Then we have [an estimate of] the probability distribution of y, and ditto for x|y=0 and x|y=1. Given that information, and some x, we can plug in an observed value for x into the distributions we got from our training, and hence have point estimates on all those distributions. Then from those point estimates, we apply bayes rule.  (Or at least I think this is how it goes.)


## Naive bayes

The conditional independence across the x's assumption is, well, false, like always. (That's the naive part.)  But it works surprisingly well for, e.g., classifying text documents.  

The idea of this kind of text classification is a bag of words might have, like, 50,000 distinct words, far too many features to fit to a multinomial or something in your spam classifier. 

But with the conditional independence assumption you can just assume the probability of all your x's given y is just the product of each individual conditional probability of xi|y.

Unsurprisingly, the maximum likelihood estimate for the word j conditional on being spam is just the proportion of e-mails that had that word and were spam within the e-mails that were spam... this is just basic probability. 

And, again, the probability of y is just the proportion of spam e-mails in the dataset.

and then you compute p(y|x) with bayes rule 

So I'm guessing that to actually predict, you compute the product of the p(x|y) for all x, and that's the joint probability of p(y|x) thanks to the conditional independence assumption, then bayes rule gives you your prediction right away.  This is almost comically easy.

Also, here's [another explanation](https://www.youtube.com/watch?v=TpjPzKODuXo&list=PL6397E4B26D00A269&index=26) of how Naive Bayes works by more Stanford profs Dan Jurafsky & Chris Manning (and [second part](https://www.youtube.com/watch?v=0hxaqDbdIeE&index=27&list=PL6397E4B26D00A269)).

## Laplace smoothing

What happens when you try to classify something with a word that wasn't in your training set?  Oh boy, you're dividing by zero when you apply bayes rule. That's bad.  (The numerator is 0 too thanks to some products.  Bad.)

So what we just do is, instead of calculating the probability p(xi|y=1) with the raw proportion, we add 1 to the numerator and k, where k is the number of possible values for xi (i.e., 2, with all these dichotomous variables we're working with). (or is it possible values for y? should investigate.)  And we do that with our other maximum likelihood estimators too. Essentially we just add 1 to every count, including the counts in the denominator.  That keeps the probabilities of unseen events from being 0, which is nice, but keeps the good statistical properties.

(question: why don't we just drop words that don't appear in the training data from our predictions?)

So I take naive bayes to be, from start to finish:

1.  load documents and labels

2. create set of all words in documents

3. for each word, create feature as appears/does not appear number of times that word appears in the given document.  (In the Dan Jurafsky & Chris Manning version, it's a bit more complex because they use counts rather than indicators.)

4. then for each word, "train" by creating vector of conditional probabilities for each class, i.e., probability of word x given class 1, probability of word x given class 2, etc.  This is calculated by simple proportion, i.e., number of documents in the class in which the word appears, divided by count of documents in the class. See page 10 of [notes 2](https://see.stanford.edu/materials/aimlcs229/cs229-notes2.pdf) for the formal version.  This can probably be stored in some kind of hash. (The Dan Jurafsky & Chris Manning version has slightly different calculations because they're using counts rather than binary features, but I think I like the Ng version better.)

5. Remember to do previous step with laplace smoothing. (add 1 to each numerator, and 2 to each denominator for binary feature)

6. then for a prediction, just take every word, then, for each class multiply out the conditional probabilities for each word given that class and the class, and divide by the overall probability of that data, as shown on the last formula on page 10 of the notes. Then predict the class where that product is highest. If an unknown word appears, it just gets the conditional probability 1/2 or whatever other laplace denominator you use for every class, thanks to smoothing.
