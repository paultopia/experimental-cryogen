{:title "Naive Bayes Speed Test, OR: Everything is a Dot Product", :layout :post, :date "2017-07-25", :executable false, :mathy true, :tags ["python" "math" "machinelearning" "performance" "numpy"], :math-preprocessed true}
 
(With help from [Carlos De La Guardia](http://carlosd.ghost.io/), who was like "dude, vectorize this stuff" after seeing the incredibly slow implementation the first time, and then was kind enough to talk through the problem and [the notebook](https://github.com/paultopia/haskeml/blob/master/naive-bayes-speed-test.ipynb) on which this post is based. All infelicities are mine (and I didn't even implement his wise suggestions for cleaning up the notebook).)

Naive Bayes is the simplest possible machine learning algorithm. In its Bernoulli form, calculation is just a matter of applying probability 101 techniques to calculate the (estimated) conditional probabilities of your predictors given the labels and estimated probability of the labels, then applying Bayes Rule directly to generate a posterior on a label given the data. Trivial. 

"Training" the model, then, is just arithmetic. Supposing we have everything in a pandas dataframe in which the first column is our label, and in which all features and the label are encoded as 1s and 0s. Then is all the code it takes:

```python
prob_y = df["LABEL"].sum() / len(df)
spams = df[df["LABEL"] == 1]
notspams = df[df["LABEL"] == 0]
features = list(df)[1:]

def conditional_probability_dict(column_label, condition_df):
    numerator = condition_df[column_label].sum() + 1 
    denominator = len(condition_df) + 2
    return {column_label: numerator / denominator}

x_probs_conditional_on_spam=[conditional_probability_dict(x, spams) for x in list(spams)[1:]]
x_spam_lookup = toolz.merge(x_probs_conditional_on_spam)

x_probs_conditional_on_notspam=[conditional_probability_dict(x, notspams) for x in list(notspams)[1:]]
x_notspam_lookup = toolz.merge(x_probs_conditional_on_notspam)
```

What this produces is a dict of priors on each x for each state of y, plus a prior on y. Note that we use [Laplace smoothing](https://stats.stackexchange.com/a/171210/69606) to calculate our x priors, otherwise if a feature isn't see for one of our labels, we end up just predicting zero for it. 

Then a "prediction" is simply an application of Bayes Rule to estimate the probability of y=1 and the probability of y=0 for each observation. Recall that Bayes Rule is: 


```nohighlight 
 $$p(y=\phi | x1, x2... xn) = \frac{p(x1|y)p(x2|y)...p(xn|y)p(y)}{p(x1)p(x2)...p(xn)}$$ 
```


Note that the denominator is going to be the same for the y=1 and y=0 calculations for each observation, so we can drop it, since we're just comparing those on an observation-by-observation basis. What we end up with is:


```nohighlight 
 $$p(y=\phi | x_{1..n}) = p(y=\phi)\prod_{i=1}^{n}p(x_i | y=\phi)$$ 
```


So here's the bad and slow way to actually generate predictions on this model. 

```
def slow_predict(row):
    prob_spam = log(prob_y)
    prob_notspam = log(1 - prob_y)
    for feat in features:
        if row[feat] == 1:
            prob_spam = prob_spam + log(x_spam_lookup[feat])
            prob_notspam = prob_notspam + log(x_notspam_lookup[feat])
    if prob_spam >= prob_notspam:
        return 1
    else:
        return 0
```

A few notes about this implementation.  First, it leverages the fact that `$log(xy)=log(x)+log(y)$` to sum the logarithms of the priors rather than multiply them. This is important, because otherwise you get into *really really* small floating point values when you multiply a bunch of already tiny probabilities together; that's a recipe for terrible floating point errors. 

Second, what this is doing is iterating over every cell in every row of the dataset. For each cell, it looks up the value in the dataframe to make sure it's a 1, and if it is, then it goes and looks up the prior for that feature in the dict (hash, for non-Pythonistas), and then adds it to a running total. (For extra special bonus inefficiency, it also calculates the logs over and over and over again, which was dumb.)

For dataset of 9,663 features and 5,574 observations (a cleaned-up and binarized version of the [UCI SMS spam dataset](https://archive.ics.uci.edu/ml/datasets/sms+spam+collection)), that took about fifteen minutes to generate predictions on the original data. 

Needless to say, this is totally outrageous. A little profiling indicated that basically all the time was spent looking up values, i.e., with all that iteration. (Calculating all those unnecessary logs didn't seem to register.)

After some time chatting with Carlos and a whiteboard, the solution came to me: after switching from multiplying probabilities to adding log probabilities, *calculating the posterior is just a dot product of a vector of the priors for x and the binarized features* (plus adding the log probability of y).  That means no explicit iteration is necessary, no repeated lookups, no check to see of the feature is 1 or 0 for a given observation---all that stuff is unnecessary. 

So let's implement that. For ease of prediction, we start by reshaping the data. We'll get our labels out of the dataset for ease of multiplication, and add a column of all ones at the start to facilitate including `$log(p(y=\phi))$` in the dot product rather than adding it later. (Let's just call this a "pseudo-intercept.") We'll also change our feature priors from a lookup dict to a vector so that we can multiply it out without a bunch of lookups. For purposes of consistency and guaranteed playing-nice-with-numpy, we'll keep everything in Pandas datastructures.

```
clean_spamdf = df.iloc[:,1:]
clean_spamdf.insert(0, "pseudo_intercept", 1) 

true_vector =[log(x_spam_lookup[x]) for x in features]
true_vector.insert(0, log(prob_y))
true_vector = pd.Series(true_vector) 

false_vector =[log(x_notspam_lookup[x]) for x in features]
false_vector.insert(0, log(1-prob_y))
false_vector = pd.Series(false_vector)
```

Then we'll bust out our matrix math, with the terrifying speed of numpy:

```python
def fast_predict(df, true_priors, false_priors):
    true_posterior = np.dot(df, true_priors)
    false_posterior = np.dot(df, false_priors)
    combined = np.column_stack((false_posterior, true_posterior))
    return np.argmax(combined, axis=1)
```

The last two lines might be a little opaque. Column_stack does what it says on the label, i.e., sets up a numpy 2d array in which the posterior on y=0 is the first column and the posterior on y=1 is the second; then argmax returns the row index that has the maximum value---thus, conveniently, returns 0 when `$p(y=0) > p(y=1)$` and so forth. 

Running this took, I'm not even making this up, under 2 seconds.  Compare that to about 15 minutes for the slow version.

So when people say you should vectorize stuff, this is why...
