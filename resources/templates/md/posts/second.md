{:title "Experimenting"
 :layout :post
 :date "2016-10-17"
 :executable true
 :tags  ["meta"]}

This is just a post to experiment with klipse and making it work with cryogen markdown passing. 

the following is done with markdown fenced block: 

```clojure
(take 10 (map inc (range)))
```

the following is done with raw html code tags:

<code class="clojure">
((comp (partial apply +) (partial map (partial * 3))) [1 2 3])
</code>


Doing some haskell to see default highlight.js behavior:


```
plength :: [a] -> Int
plength [] = 0
plength (x:xs) = 1 + plength xs
```


Some python with markdown fenced block:

```python
for x in range(20):
    print x
```


With raw code tag:

<code class="python">print [x.upper() for x in ["foo", "bar"]]
</code>

(need to minimize loading of plugins by having a tag for each language)
