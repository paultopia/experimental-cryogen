{:title "A Debugging Trek, and: (naive) Batch Gradient Descent in Haskell"
 :layout :post
 :date "2017-07-17"
 :executable false
 :mathy false
 :tags  ["haskell", "data", "debugging"]}

So I implemented [batch gradient descent](https://paultopia.github.io/posts-output/ng1/) in Haskell, to simultaneously solidify my understanding of the algorithm and work on learning Haskell. 

It got a bit bumpy. I've preserved my [realtime notes](https://github.com/paultopia/haskeml/blob/master/notes.md) of the mess. But the short version is that after a certain number of iterations that was an increasing function the learning rate, the model would just terminate in weights of Infinity for all features. 

So!  Debugging.  

Step 1: get some more eyeballs. A kindly recurser pointed out that the argument orders to one of my functions was switched. This is the kind of thing that Haskell is supposed to avoid, but when everything is of type [Double] I guess one can hardly rely on the type checker to catch that it's the wrong one, can one?  

Alas, that wasn't the bug. I was just multiplying two lists together elementwise, and since multiplication is commutative, well.

Step 2: implement in an easier language first.  With math-y code that produces wildly incorrect results, there are really two possibilities: either the code is buggy or the code is fine but my understanding of the math is wrong.  (Or both, I suppose.)  The second possibility seemed easier to eliminate first---with correct math, one can get evidence supporting the proposition that the code is correct from getting correct results, but it doesn't really work the other way around unless there's some way of proving code correctness other than testing. So. 

So I [re-implemented the algorithm in python](https://github.com/paultopia/haskeml/blob/master/python-reference-bgd-implementation.ipynb), complete with lots of slow and careful idiot-checking print statements. Somewhere in the middle, I realized that I hadn't scaled the features in the Haskell version (alas, that didn't turn out to be the bug either, but it did kindly turn my lists of `Infinity` into lists of `NaN`). The Python version produced reasonable results. So that's great, my math is good. Back to the Haskell.

3.  Then I dug into the Haskell.  Not knowing how to insert the equivalent of `printf` statements into this language---do you need an IO type or something??-- I was at a loss for a while. 

It turns out that ghci [has a debugger](https://downloads.haskell.org/~ghc/7.4.1/docs/html/users_guide/ghci-debugger.html), which I played around with for a while, but it wasn't terribly enlightening---it's a bit involved, and could use more study. 

Then I discovered that there is a printf equivalent.  Because of course there is.  It's in the magic [Debug.Trace](https://hackage.haskell.org/package/base-4.9.1.0/docs/Debug-Trace.html) library. And it's extra magical: you can just stick a call to `traceShow` in front of whatever code you want to look at, and you'll get to see whatever values you pass it.

With that, it didn't take me long to find my bug.  I successively logged 20 iterations of all the interesting parameters to my gradient descent function, and soon discovered that on each iteration of the gradient, the error was monotonically increasing, rather than decreasing. Which obviously isn't right.  

The obvious hypothesis there is that a sign got flipped somewhere.  And, lo and behold, after looking at the relevant part of the code, inside the gradient descent summation where I meant to be subtracting the label from the hypothesis, I actually was subtracting the hypothesis from the label. 

Subtraction, alas, is not commutative. 

Anyway, here's the fixed code!  

```haskell
module Olsgradient where
import Data.List
default (Double)

addIntercept :: [[Double]] -> [[Double]]
addIntercept = map (\x -> 1.0:x)

predict :: [[Double]] -> [Double] -> [Double]
predict observations weights =
  let mult = map (\x -> zipWith (*) x weights) observations
  in map sum mult

subtractMaker :: Double ->  [Double] -> [Double] -> Double
subtractMaker learnRate costs featureList =
  let costFeatureMult = zipWith (*) costs featureList
  in learnRate * sum costFeatureMult

gradientStep :: Double -> [Double] -> [Double] -> [[Double]] -> [Double]
gradientStep learnRate labels weights observations =
  let preds = predict observations weights
      costs = zipWith (-) preds labels
      featureMatrix = transpose observations
      subtractors = map (subtractMaker learnRate costs) featureMatrix
  in zipWith (-) weights subtractors

innerTrainOLS :: [[Double]] -> [Double] -> [Double] -> Double -> Double -> Double -> Double -> [Double]
innerTrainOLS observations labels weights learnRate threshold maxIter numIter 
  | numIter > maxIter = weights
  | sse < threshold = weights
  | otherwise = innerTrainOLS observations labels newWeights learnRate threshold maxIter (numIter + 1)
  where
    preds = predict observations weights
    sse = sum $ map (**2.0) (zipWith (-) labels preds)
    newWeights = gradientStep learnRate labels weights observations

trainOLS :: [[Double]] -> [Double] -> Double -> Double -> Double -> [Double]
trainOLS observations labels learnRate threshold maxIter =
  let obvs = addIntercept observations
      numFeats = length $ head obvs
      initweights = replicate numFeats 1
  in innerTrainOLS obvs labels initweights learnRate threshold maxIter 0

mean :: [Double] -> Double
mean lst = sum lst / fromIntegral (length lst)

standardDeviation :: [Double] -> Double
standardDeviation lst =
  let m = mean lst
      n = length lst
      squaredErrors = map (\x -> (x - m) ** 2.0) lst
  in sqrt (sum squaredErrors / fromIntegral n)

scale :: [Double] -> [Double]
scale lst =
  let m = mean lst
      stdev = standardDeviation lst
  in map (\x -> (x - m) / stdev) lst
```

Note how smelly it is.  For example, `innerTrainOLS :: [[Double]] -> [Double] -> [Double] -> Double -> Double -> Double -> Double -> [Double]` is like legit stink.  But it does the job for now, and cleanup can come later. :-) 

