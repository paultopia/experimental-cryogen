{:title "Mathy Ng Lecture 4: Newton's Method, Exponential Family Distributions, GLMs", :layout :post, :date "2017-07-07", :executable false, :mathy true, :math-preprocessed true, :tags ["meta" "test" "math"]}


## Ng Lecture 4: Newton's Method, Exponential Family Distributions, GLMs.

### Newton's Method

Motivation for Newton's method: suppose you have a nonlinear function of `$\theta$` and you want to figure out at what value of `$\theta$` it == 0.

One strategy is just to pick an arbitrary `$\theta^{(0)}$`  calculate `$f(\theta^{(o)})$`  and then compute a derivative there in order to get a linear approximation to f, i.e., get a tangent line at that point.  Then extend the tangent line until it extends to the horizontal axis and call that `$\theta^{(1)}$` (i.e., solve the linear approximation function `$f'(\theta^{(1)}) = 0$` . That's one iteration. And then keep repeating that with a tangent line to `$\theta^{(1)}$` and so forth.  This is Newton's method.

After a little algebra, this gives us an update rule for Newton's method:


```nohighlight 
 $$\theta := \theta - \frac{f(\theta)}{f'(\theta)}$$ 
```


We can apply the same idea to maximizing the log-likelihood. If we have a likelihood function, we want to find the place where its derivative == 0. (Of course, that could also be a minimum. In the case of linear regression and such that shouldn't be a problem, because convex... right?) And we can apply the same update rule to that function ( where "that function" is the derivative of the likelihood function).  So, if `$\lambda$` is the likelihood function, the update rule is:


```nohighlight 
 $$\theta := \theta - \frac{\lambda'(\theta)}{\lambda''(\theta)}$$ 
```


In multiple dimensions, this turns into multiplying a matrix of first derivatives by the inverse of a matrix called the Hessian, which is a matrix of second derivatives.  See page 21 of [lecture notes 1](https://see.stanford.edu/materials/aimlcs229/cs229-notes1.pdf).

Usually this goes faster than gradient descent for small numbers of features, but if there are lots of features, be aware that the Hessian is a n+1/n+1 matrix (remembering that Ng uses n for number of features and m for number of rows), it can be computationally expensive.

### Generalized Linear Models

Some terminology. Bernoulli and Gaussian aren't single distributions but really lasses of distributions, depending on their parameters.  And both classes are in the family of "exponential distributions."  So are lots of others---Poisson, gamma, exponential, beta, Dirichlet are examples that he gives in the notes, but says there are also "many more."

The point, leaving aside the derivations of how the Bernoilli and Gaussian are in the exponential family, is that you can construct a GLM for anything in the exponential family. So if you've got something where what you're trying to predict is well modeled by a Poisson, or a Dirichlet, or whatever, you're good. 

Three assumptions of GLM: 

1.  Given inputs x and parameters `$\theta$`  the response variable y is distributed in the exponential family with some natural parameter (see lecture notes 1 p. 22) `$\eta$`  

2.  Given x, the goal is to output (as h(x)) the expected value of the sufficient statistic of y `$E[T(y)|x]$`   Typically, `$T(y) = y$` (see page 22 of lecture notes 1).

3. There's a linear relationship between the natural parameter and the inputs: `$\eta = \theta^Tx$` (where the elements of `$\theta$` just contain constants, not functions of x or something wild like that, I take it, or it needn't be linear...).

And judging from what happened at around minute 45, I take it that what you really just do is write the distribution as an exponential family, and then substitute `$\theta^Tx$` for `$\eta$`  And then what you get is the function for your hypothesis. See pp. 25-6 for examples of OLS and logistic. The reason we choose the sigmoid function for our hypothesis for the logistic is because we decided that the bernoulli distribution is a natural distribution to use to model these binary choices. 

So: 

1.  Pick a distribution. 

2.  Formulate that distribution as an exponential family distribution.

3.  Make use of assumption 3 above to swap out the eta. 

4.  Then you have your hypothesis.  

5.  Train by maximum likelihood.  How?  Take the log likelihood (reminder: that works because maximizing the likelihood can be done by maximizing a strictly increasing function of the likelihood) of the function giving the probability of response data given feature data paramaterized by theta, as before; see pg. 29-30 for an example re: softmax (Likelihood reminder: the product, over the training set, of probability of label given feature.). And then maximize it, using gradient or newton. 

Where to actually find the likelihood function?  The heart of the idea goes back to pg. 18 of notes 1 in yesterday's material: the likelihood function starts by assuming that the hypothesis is correct, for some theta. That's what "Let us assume that: `$P(y=1 | x;\theta) = h_\theta(x)$` and `$P(y=0 | x;\theta) = 1 - h_\theta(x)$`  says to us. So then in the later equations on pg. 18 we can substitute a function of the hypotheses (in the case of logit, a simple h and 1-h) for the probability of an individual outcome; the probability of all the outcomes at once is their product by standard probability theory.

Softmax, given at the end of the material today, is a generalization of logistic to multiple classes.  The hypothesis is a vector of probabilities for each class, based on the multinomial distribution.


An insight from the study group, discussing the previous lesson: 

Why does the assumption that errors are normally distributed imply that the likelihood function on OLS is this gaussian thing?  Because if you hold x and `$\theta$` constant, then the response has to be distributed the same way as the errors are. 
