{:title "Lecture 2 of Andrew Ng's mathier ML course"
 :layout :post
 :date "2017-07-07"
 :executable false
 :mathy true
 :tags  ["meta" "test" "math"]}
 
One of the things I'm doing at [RC](https://www.recurse.com/) is working through [the mathier version](https://see.stanford.edu/Course/CS229/54) of Andrew Ng's famous machine learning course. Here are my notes from the first substantive lecture (lecture 2). (I'm not sure if the math will work, I just tried to fire up Mathjax...)

(Edit: for future reference, [underscores for subscripts need to be escaped out to get them through the markdown parser to mathjax](https://github.com/mathjax/MathJax/issues/329).)

## Lecture 2

Linear regression, mostly stuff I've already seen a bunch of times, but with the derivations using weirder linear algebra than usual, and with more gradient descent and less closed-form solutions.

His notation: 

m = number of training examples

x = features

y = output (labels/target)

(x, y) = training example

superscript i for indexing over examples.

h for model hypothesis --- it's the function mapping x->y 

representing weights as $\theta$

n is the number of features

(Aaah, why not use n for the number of observations like scientists do?  Why can we not have consistent naming and notation in this world? Also, at some point when I was in grad school, we made a Stanford polisci t-shirt that had the slogan "it's not the size of your n, it's how you use it." Where did that go? I miss that shirt.)

$J(\theta)$ is the sum of squared errors / 2.  (Apparently, from discussion afterward with mathier people, these get divided by 2 in order to make differentiation cleaner.)

Gradient descent algorithm:

update $$\theta\_i := \theta\_i - \partial \frac{\partial}{\partial \theta} J(\theta)$$

that is, update the ith weight by subtracting the partial derivative of the cost (J(θ)) with respect to the ith weight (30:29 in video 2).

Calculus reminder: that [partial derivative](https://www.khanacademy.org/math/multivariable-calculus/multivariable-derivatives/partial-derivative-and-gradient-articles/a/introduction-to-partial-derivatives) is essentially the magnitude of the change in the cost given an epsilon change in the weight at the given value of θ. 

This [lecture](https://www.youtube.com/watch?v=i94OvYb6noo) (suggested by a batch-mate) is also good on the gradients:

(Ng uses `:=` to mean "update the variable on the left by the algorithm on the right")

Ultimately, it's just taking a step in the direction of the (local) minimum error.

After doing the calculus, that turns into: $$\theta_i : = \theta_i - \alpha (h_{\theta}(x) - y) \cdot x_i$$ 

where alpha is the learning rate (a model parameter). This is for one training example.

which is super convenient, since $(h_{\theta}(x) - y)$ is just the straightforward error at a given step.

### Batch gradient descent

For m training examples, the algorithm just sums the error over the training examples, i.e.,

$$\theta_i : = \theta_i - \alpha \sum_{j=1}^{m} (h_{\theta}(x^{(j)}) - y^{(j)}) \cdot x_i^{(j)}$$ 

and then repeat until convergence

For OLS, there's only one global minimum, it's just a quadratic function that (therefore?) is "bow-shaped" (I've never understood these visual analogies on functions, but I take it that this is a good thing, and maybe means the same thing as convex??  Convexity is the property we discussed in our little group afterwards, so probably.  Note to self, really need to understand convexity and what it entails lots better.), and so no nasty local minima.

(Terminology reminder: gradient = derivative. [Essentially](https://math.stackexchange.com/questions/1519367/difference-between-gradient-and-jacobian).)

It turns out that this computation is the direction of steepest descent, for reasons that Ng doesn't feel like proving. 

This is called "batch gradient descent" because at every step of gradient descent you look at the whole dataset, perform a sum over m training examples.

That's problematic if you have a ton of training examples.  So there's an alternative:

**Stochastic Gradient Descent**
(a.k.a. "incremental descent")

repeat until convergence: 
```
for j = 1 to m: 
    perform an update using just the jth training example (for each i) 
```

that update is just  $$\theta_i : = \theta_i - \alpha (h_{\theta}(x^{(j)}) - y^{(j)}) \cdot x_i^{(j)}$$ 

in practice, this tends to go rather faster for large datasets. It doesn't actually converge exactly to the global minimum, but they tend to wander close to it. 

Question I had: what's stochastic about this? It's not like it's actually randomly sampling the data or anything. In discussion afterward, someone said that it's called stochastic because it's based on the idea that the expectation of the update on a single observation is the same as the expectation on the update on the whole thing, which makes sense well enough to me.

### Closed form solution of theta

More new notation, for matrix derivatives. I mostly let this go by, because I feel like I've learned this derivation once already, courtesy of grad school, and I don't feel the need to do it again with different linear algebra. But reference: [part 1 of his lecture notes](https://see.stanford.edu/materials/aimlcs229/cs229-notes1.pdf) on pg. 8.

A few points of interest: explanation of the stuff in the notes:

delta J, J is a function of vector parameters theta, recall. The derivative of J with respect to theta is itself a vector of partial derivatives, a n+1 dimensional vector. So then we can rewrite the batch gradient example as theta (not subscripted---it's the whole thing, update the whole paramerer) minus that big gradient, i.e., $$\theta := \alpha \nabla_\theta J$$ --and all of those quantities are N+1dimensional vectors. (except alpha, obvs)

Definition that feel like a bit of linear algebra I skipped: if A is a square matrix, the *trace* of A is the sum of A's diagonal elements. $tr A = \sum_{i=1}^n A_{ii}$ Which sounds like skipped-over linear algebra to me.

Ultimately this leads to the classic closed form solution to OLS, which shows up on pg. 11 of part 1 of lecture notes. 

Also might be worth noting (from video at 1:00) that the "design matrix" is a matrix that has the training examples input values on the rows.  In notes and on chalkboards, there's a very confusing notation with dashes and an unexplained superscript with a  T in it... but I take it that the first row is the vector of features for first training example, second row is for second draining example (from video at 1:01).

Then design matrix multiplied by theta vector is just the hypotheses for a given set of weights.  And the error is going to be elementwise subtracting the elements of the y vector (label vector, which gets an arrow over it in the notes like $\overrightarrow{y}$).

Anyway, classic closed form: 

$$\theta = (X^TX)^{-1}X^T\overrightarrow{y}$$

This is our old friend OLS. Hello OLS. You're also [enjoyably easy to implement in clojurescript](https://github.com/paultopia/browser-stats/blob/master/statspop/src/statspop/math/regression.cljs#L15).

Note at the end of the lecture in response to a student question: usually, if $X^Tx$ isn't invertible, it's because you've got dependent features in there, like repeating the same feature twice or something. (or linear combination, I take it? Standard OLS blow-up...)

that's it!
