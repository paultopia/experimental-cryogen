{:title "Lecture 3 of Andrew Ng's mathier ML course", :layout :post, :date "2017-07-07", :executable false, :mathy true, :math-preprocessed true, :tags ["meta" "test" "math"]}
 
 
## Lecture 3 -- locally weighted regression

Nonparametric algorithms reduce "the need to choose features very carefully" (I guess that makes sense if you think of features as mathematical transformations on stuff observed rather than stuff observed in general... a nonparemetric algorithm surely can't avoid the fact that you left something off, though I guess it can help avoid the fact that you threw a bunch of extra stuff in...)

Formal definition of a nonparametric algorithm is an algorithm where the number of parameters grows with m.  Which also means it needs to hold onto the entire training set even after training. As a student said in questions, "it's like you're not even really building a model at all." You just fit for every training example. (This seems really expensive!!)

### Locally weighted regression (loess/lowss)


Concept: consider a value for x, the vector of features, of a single observation. To make a prediction with OLS, we'd find the vector of weights (parameters) `$\theta$` s.t. they minimize the cost function, then return `$\theta^tx$` as prediction.

For loess, we'd take a region around x, and work on the subset of data around there. So, geometrically, rather than predicting y based a line fitted to the entire dataset, predicts y based a line fitted to a subset of the dataset around x.

formally, in loess we fit `$\theta$` to minimize a weighted version of the same loss function we use in OLS, where the weights are chosen such that we give more weight to training examples closer to what we're trying to predict. 

i.e., OLS minimizes: 


```nohighlight 
 $$\sum_i(y^{(i)}-\theta^Tx^{(i)})^2$$ 
```


while loess minimizes:


```nohighlight 
 $$\sum_iw^{(i)}(y^{(i)}-\theta^Tx^{(i)})^2$$ 
```


the trick is in the definition of the weight function.  As I understand it it the fit is made at the time of prediction, so x without a subscript in the below is the value of the feature *for which you're trying to predict the output* (and that's why you have to keep your training data around even after training, as Ng noted earlier. Do you even train at all in advance? Maybe there's some optimization trick that allows you to pre-train something? Kinda doubting it from the "not even a model" chat above.).  So with that preamble, the weight function is 


```nohighlight 
 $$w^{(i)}=e^{(-\frac{(x^{(i)}-x)^2}{2})}$$ 
```


Actually, he said there are lots of possible weight functions, but the point is to have something that gets close to zero when `$x^{(i)}$` is far from x and close to 1 when they're close together.  Which, obviously, this satisfies.

A more common form of the weight is


```nohighlight 
 $$w^{(i)}=e^{(-\frac{(x^{(i)}-x)^2}{2\tau^2})}$$ 
```


where tau is a "bandwidth parameter" that controls the rate at which the weighting function falls off with distance from x.

Another student question: this is indeed very costly, "every time you make a prediction you need to fit theta to your entire training set again." However, "turns out there are ways to make this much more efficient." He referred to [Andrew Moore's kd-trees](http://www.ri.cmu.edu/pub_files/pub1/moore_andrew_1991_1/moore_andrew_1991_1.pdf) as this method.

### Probabilistic interpretation of linear regression

Why are we minimizing the sum of squared error as opposed to the absolute value or something? Assumptions that make this work. (Oh boy, are we going to do BLUE again?  Might skim past this.)

First we "endow the least squares model with probabilistic semantics."

Yeah, this is the same stuff.  Assume y is a function of the model plus error, assume error is IID and distributed normally with mean zero, all the good social science stats stuff I already know. Then we do the standard probability and algebra and get the maximum likelihood estimator, which turns out to be the OLS cost function.  And that was like a whole class in grad school. The central limit theorem came up, as it does. All the good stuff. For his derivation, see pages 11-13 of [lecture notes 1](https://see.stanford.edu/materials/aimlcs229/cs229-notes1.pdf).

There is one useful notation note though.  Semicolon indicates not a random variable but as something we're trying to estimate in the world, i.e. this: 


```nohighlight 
 $$P(y^{(i)}|x^{(i)};\theta)$$ 
```


indicates "the probability of `$y^{(i)}$` conditioned on `$x^{(i)}$`  as parameterized by `$\theta$` " while this:


```nohighlight 
 $$P(y^{(i)}|x^{(i)},\theta)$$ 
```


means "the probability of `$y^{(i)}$` conditioned on `$x^{(i)}$` and `$\theta$` " which is wrong, because theta isn't a random variable, it's a property of the world we're trying to estimate (in frequentist terms).

The conditional probability (the correct one) = the likelihood function, only y gets an arrow (to indicate it's observed?). So maximum likelihood.

### Classification

Started off with standard stuff, instead of choosing a linear function, we choose a nonlinear function.  And for logistic regression, that's the sigmoid function, a.k.a. the logistic function: 


```nohighlight 
 $$h_\theta(x) = \frac{1}{1 + e^{-\theta^Tx}}$$ 
```


One important point is that for logistic gradient descent we're actually ascending, that is, we're trying to maximize not minimize, so we add the gradient rather than subtract it.

Interestingly, it comes out to the same update rule with the sign swapped when the dust settles. It's not the same math because the function that generates the hypothesis is obviously different (the linear function vs the logistic function), but it has the same functional form.  Logistic gradient ascent update rule:



```nohighlight 
 $$\theta_j : = \theta_j + \alpha (y^{(i)} - h_{\theta}(x^{(i)})) \cdot x_j^{(i)}$$ 
```


Why does maximizing this work exactly?  It's just maximum likelihood again.  It turns out that for OLS, maximizing the likelihood function, after the math dust settles, is the same as minimizing the least squares cost function. (See notes pg. 13) But for logistic regression, when we *maximize* the log likelihood of the parameters, the gradient ascent that we use to directly maximize the likelihood just simplifies to the same form. 

(An explanation of why turned up elsewhere: logistic regression likelihood is concave. page 11 of [this](https://courses.cs.washington.edu/courses/cse547/16sp/slides/logistic-SGD.pdf))

### digression on perceptrons

a perceptron is just the same update rule but with this threshold function that maps positive values to 1 and negative values to 0 rather than logistic function's mapping of everything to the space from 0-1. 

Hard to interpret perceptrons probabilistically though.
