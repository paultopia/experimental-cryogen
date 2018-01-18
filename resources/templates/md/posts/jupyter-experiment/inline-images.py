import re

def makeb64img(filename):
    with open(filename, "rb") as ju:
        data = ju.read()
        b64string = data.encode("base64").replace('\n', '')
    return b64string

def swapout(match):
    #print match.group(1)
    #print match.group(2)
    #print match.group(3)
    newstring = "data:image/png;base64," + makeb64img(match.group(2))
    #print(newstring)
    return match.group(1) + newstring + match.group(3)

with open("jupyter-experiment.md") as je:
    post = je.read()

regex = r'(!\[png\]\()(.*png)(\))'

newpage = re.sub(regex, swapout, post)

with open("jupyter-experiment-with-image.md", "w") as je2:
    je2.write(newpage)
