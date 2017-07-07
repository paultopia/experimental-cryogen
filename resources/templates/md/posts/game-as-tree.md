{:title "Translating Game Theory Backward Induction into a Tree Algorithm"
 :layout :post
 :date "2017-07-06"
 :executable false
 :tags  ["game theory" "algorithms" "trees" "python"]}

I know a lot more about game theory than I do about graphs/trees/etc. (Political scientist, yo.) So here's an attempt at a translation of the idea of backwards induction into tree terms. 

First, intuition/background: backwards induction is a method for finding subgame perfect equilibria of a sequential game in extensive form. (For simplicity, let's assume a perfect information game.) The short version is that, starting at the terminal node, you set the last player's choice for that node as the choice that yields the best they can achieve given the history of play leading up to that node. With that information, you now know the values for each choice of the second-to-last node for the second-to-last player, so set their choice to that. And so on, inductively. 

This is pretty easily represented in code terms. An extensive form game is just a [tree](https://stackoverflow.com/questions/7423401/whats-the-difference-between-the-data-structure-tree-and-graph). So you can represent your extensive form game as a tree of depth n (I don't know if "depth" is a term of art for trees, but I mean that a lone node has depth 1, a parent with children that have no children themselves id depth 2, and so forth), such that the first choice is the root node, then its children are the paths from that decision, so forth.  So then you can just take the nodes at depth n-1 and replace them by the max of their children, and keep doing that until you reach the top node. Obviously, you also need to keep a bit of state somewhere that builds up the path through the tree, and there you go, you have your equilibrium. 

The cool thing is that when you get backward induction, the idea generalizes to other kinds of problems. For example, there's a project Euler problem that asks you to find the maximum value path from top to bottom of a pyramid of numbers that maximizes the sum of the numbers in that path.  Something like this: 

<pre>
    4
   5 9
  7 9 5
</pre>

where the answer would be 22 (4-9-9).  Only, you know, much bigger.

When you realize that this is just a tree with the same kind of maximization problem, backwards induction is the obvious solution. Taking the n-1th row, each element can either terminate in the element to its below-left or its below-right; since you want to find the max, you can just update the elements in the n-1th row by the max of what it can terminate in. Then you can forget about the nth row and treat the n-1th row as the terminal row. Then apply the same procedure to update the n-2th row, and so on.

So, in the toy example above, the first round of updating looks like this: 

<pre>
    4
  14 18
</pre>

that is, 5 becomes `5 + max(7, 9)`, 9 becomes `9 + max(9, 5)` and so forth.

In python:

```python
with open("pyramid.txt") as tf:
    pyramid_pre = tf.readlines()

pyramid = [map (int, x.split(" ")) for x in pyramid_pre]

def update_values(pyramid):
    lastrow = len(pyramid) - 2
    for x in range(lastrow, -1, -1):
        row_length = x + 1
        for y in range(row_length):
            pyramid[x][y] = pyramid[x][y] + max(pyramid[x + 1][y], pyramid[x + 1][y + 1])
    return pyramid[0][0]

print(update_values(pyramid))
```

I assume there's a standard tree-traversal algorithm that CS people use for this kind of problem, curious whether it looks like the game theorist's backward induction or there's a better way.  But this way does the job!
