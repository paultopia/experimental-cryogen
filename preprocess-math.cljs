(ns preprocess.core
  (:require [planck.core :refer [slurp spit eval file-seq]]
            [clojure.string :refer [replace replace-first ends-with?]]
            [cljs.reader :refer [read-string]]))

;; all this code identifies the correct posts and their filenames, which get stored in matching-posts

(def parse-map (comp read-string eval))

(defn extract-map [post] (re-find #"{[\s\S]*}" post)) ; see: https://stackoverflow.com/questions/1068280/javascript-regex-multiline-flag-doesnt-work -- since this is planck, and hence cljs, it's crippled by javascript.

(defn mathy? [post] (get (parse-map (extract-map post)) :mathy))

(defn math-preprocessed? [post] (get (parse-map (extract-map post)) :math-preprocessed))

(def posts (filter #(ends-with? % ".md")
                  (mapv :path (file-seq "resources/templates/md/posts/"))))

(defn read-post-with-name [postfilename]
  {:text (slurp postfilename) :filename postfilename})

(def posts-text (doall (map read-post-with-name posts)))

(def matching-posts (remove #(math-preprocessed? (:text %)) (filter #(mathy? (:text %)) posts-text)))

;; now fix them

(defn fix-inline-math [post]
  {:text (replace (:text post) #"[^$](\$[^$]+?\$)[^$]" " `$1` ") :filename (:filename post)})

(defn fix-block-math [post]
  {:text 
   (replace (:text post) #"[^$](\$\$[^$]+?\$\$)[^$]" "\n\n```nohighlight \n $1 \n```\n\n")
   :filename (:filename post)})

(defn update-map [post]
  (let [{:keys [text filename]} post
        postmap (parse-map (extract-map text))
        newmap (str (assoc postmap :math-preprocessed true))]
    {:text (replace-first text #"{[\s\S]*?}" newmap) :filename filename}))

(defn fix-post [post] (-> post fix-inline-math fix-block-math update-map))

(def corrected-posts (mapv fix-post matching-posts))

;; and then send it back to the filesystem.

(defn save-corrected-file [post] (spit (:filename post) (:text post)))

(doall (map save-corrected-file corrected-posts))

(println "preprocessed the math for you!")
