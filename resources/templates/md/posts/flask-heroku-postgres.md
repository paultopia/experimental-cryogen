{:title "The Easiest Possible Way to Throw a Webapp Online (Flask + Heroku + Postgres)", :layout :post, :date "2017-08-25", :executable false, :mathy false, :tags ["python" "flask" "heroku" "postgresql" "web"]}

Here's a situation that comes up a lot.  I want to throw together a quick CRUD app, or, for my uses, more of a C app (i.e., "hey research assistants, dump all this stuff into a database").  I could use a google form or something, but that always leads to weird output, like bizarre formatting, out in google sheets.  

So I'm getting into Flask on Heroku for this kind of thing, for several reasons: 

1. No wrangling around with database connections and such. You don't even need to give your code the environment variable to connect to postgres if you use the flask-heroku library. 

2. Making changes is seriously just pushing to a git remote. 

3. You can use interact with a real database shell via `heroku pg:psql`, *or* you can write canned queries and look at them online, or you can even sync them to google sheets (though that last one seems a bit shaky).

4. Logs are just as simple as `heroku logs` and logging something from the application is just `print()`.

5. No mess with ngnix and other server config stuff. 

6. HTTPS is handled for you too, at least on applications served from herokuapp.com. Which is real nice, obviously.

7. By the way, you get a falls-asleep-a-lot-but-good-enough-for-tiny-projects + 10k postgres rows for free. 

So let's walk through the simplest possible setup.

## Step 1: setup and install all the things.

(Installation details will depend on your platform, so I'll leave that for you to google.)

1. Get a Heroku account and [install the heroku cli](https://devcenter.heroku.com/articles/heroku-cli).  

2.  Make sure all your code is in a git repo, as we'll be using git to push to Heroku.

3. Install postgres locally, so you can use Heroku psql, pull down from Heroku to a local database, etc.

4. Set up a python virtual environment. This is usually a good idea anyway, but particularly important here because you'll be using `pip freeze` to get the requirements for Heroku, so you want to make sure you have a self-contained and reproducible environment. 

I'm a big anaconda fan, so I just use [conda](https://conda.io/docs/user-guide/tasks/manage-environments.html) to handle this for me, but other virtual environment managers should work fine too.

Incidentally, the following code assumes the latest version of Python 3. It'll probably work fine on Python 2 as well, but who really knows?

4. Pip-install the following libraries. 

**Flask** --- of course, this is a Flask-based tutorial. It's about the easiest possible way to get basic web stuff happening in Python.

**Flask-Heroku** --- this is just a very simple library that takes care of getting the heroku environment variables and passing them to your flask application in the right way. Not strictly necessary, but why make life harder for yourself?

**SQLAlchemy** --- the standard library for connecting to a relational database in Python. It's really complicated, but it seems to work fine for me while only brushing the absolute surface, and with the help of the next library. 

It's worth noting that SQLAlchemy, contrary to the Zen of Python, seems to have a million different ways to do everything, so different tutorials and documentation might have slightly different approaches to the table creation syntax and such.

**Flask-SQLAlchemy** --- simplifies the SQLAlchemy API for Flask purposes.

**Psycopg2** --- Postgres database driver.

**Gunicorn** --- just a communication layer between the server and your code, probably not strictly mandatory in quick-and-dirty hack-together apps with only a couple of users, but never hurts and can help manage lots of requests at once should they happen. For more, see [this explanation of WSGI](https://www.fullstackpython.com/wsgi-servers.html), and [this real-life account](https://ironboundsoftware.com/blog/2016/06/27/faster-flask-need-gunicorn/).

## Step 2: Write some code

Let's do a basic app, shall we?  For the purposes of our minimal example, let's stick all of this in a file called app.py.

### A. the basics:

```python
from flask import Flask, render_template, url_for
from flask_sqlalchemy import SQLAlchemy
import sys
import json
from flask_heroku import Heroku
app = Flask(__name__)
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
heroku = Heroku(app)
db = SQLAlchemy(app)
```

Most of the work here is just creating objects in the global namespace that will hold your app and database.  

The `Heroku(app)` line just [puts your environment variables where they need to be](https://github.com/kennethreitz/flask-heroku). 

The `app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False` line is to [fix performance hits from a bad default config (and silence an annoying warning)](https://stackoverflow.com/questions/33738467/how-do-i-know-if-i-can-disable-sqlalchemy-track-modifications/33790196#33790196). 

The rest should be pretty clear.

### B. Set up a database

```python
class Dataentry(db.Model):
    __tablename__ = "dataentry"
    id = db.Column(db.Integer, primary_key=True)
    mydata = db.Column(db.Text())

    def __init__(self, mydata):
        self.mydata = mydata
```

Pretty straightforward. You create a class that inherits from the model class that your db object brought in, and then you put the table schema into the fields of that class. 

SQLAlchemy will [happily auto-increment an integer primary key for you](http://docs.sqlalchemy.org/en/rel_1_1/core/metadata.html#sqlalchemy.schema.Column.params.autoincrement).

Whatever columns you want, you can declare and pass their type.  SQLAlchemy has [a bunch of types available](http://docs.sqlalchemy.org/en/latest/core/types.html), including all the basics, but also database-vendor-specific types, different flavors of datetimes, etc.

Then the constructor ([ok, really the initializer](http://spyhce.com/blog/understanding-new-and-init)) for an object just sets the properties based on data received from users. 

Hey, maybe we should get some data?

### C. Set up a route to receive data

I'm assuming here that we're just using a standard HTML form that generates a post request, so let's set up a route to receive that. 

```python
@app.route("/submit", methods=["POST"])
def post_to_db():
    indata = Dataentry(request.form['mydata'])
    data = copy(indata.__dict__)
    del data["_sa_instance_state"]
    try:
        db.session.add(indata)
        db.session.commit()
    except Exception as e:
        print("\n FAILED entry: {}\n".format(json.dumps(data)))
        print(e)
        sys.stdout.flush()
    return 'Success! To enter more data, <a href="{}">click here!</a>'.format(url_for("enter_data"))
```

Ok, this one is a little more complicated. 

The first line is just a decorator that tells Flask that the function below services a route, gives it the route to service (see the Flask docs for all the interesting stuff you can do with routes), and tells it what methods to accept.

The function initializes that object we created above, and passes it the data we received on our form. 

Note that this is as fields on a `request` object, which is actually a global. This is a Flask thing, and I think it's a real WTF design decision---Flask should make you pass it into the function as a parameter, but the Flask people made the choice to make it a global, so a global it is. 

Then I have some real seat-of-the-pants error handling in here. Heroku doesn't let you save data to a filesystem---your choices are database or nothing. But it does have built-in logging (though it only keeps something like 1500 lines of logs unless you pay for a service from someone to hold onto more), and it makes it real easy to get it: anything your application saves to stdout goes into a log.  

Since database writes can be finicky, I'm just catching all errors and logging them, along with all the data that it attempted to write. (And since users don't need to know that the database write failed, I'm telling them to keep going.)

Some of the slightly more obscure mechanics: 

- `indata.__dict__` is a dictionary containing all the properties of the object. It will include stuff that SQLAlchemy added in too, some of which isn't serializable as JSON, so I just delete it for purposes of logging.

- `sys.stdout.flush()` is advisable [just to make sure everything will land in stdout when you think it will](https://stackoverflow.com/questions/10019456/usage-of-sys-stdout-flush-method).

The return sends some html to display to the user. Here, I just use a raw string in order to give some feedback to confirm to the user that, yes, they actually managed to submit the form, and then prompt them to submit some more data if they want. 

Note that the Flask `url_for` function can take a function corresponding to another route, and then intelligently insert the url there.  So this will insert the url for the route corresponding to the enter_data function, which we should probably write... 

### C. Set up a route for users to enter data

```python
@app.route("/")
def enter_data(): 
    return render_template("dataentry.html")
```

That was pretty easy, wasn't it?  This will render a [Jinja2](http://jinja.pocoo.org/docs/2.9/) template to the user. Which should, obviously, provide them with a form to add the data. Here's an example of a minimal template:

```html
<html>
    <head>
        <title>data entry</title>
    </head>
    <body>
        <form method="POST" action="{{ url_for('post_to_db') }}">
            <label for="mydata">Gimme your data, fool!</label>
            <input type="text" id="mydata" name="mydata">
            <button type="submit">IT FEEDS IT THE DATA</button>
        </form>
    </body>
</html>
```

Nothing should be surprising there, with one exception: note that Flask is kind enough to inject the `url_for` function into the template, so you can decouple your view from whatever you do with routes and whatever server you happen to be running on and so forth.  

### D. Finish it off

```python
if __name__ == '__main__':
    #app.debug = True
    app.run()
```

When it's called from the commandline run the app. 

I stuck the commented-out line in there to reveal one of the other really sweet things about Flask: it has an *amazing* debugger; if you run it in debug mode then when something blows up you'll be able to inspect the state right from the web page it generates. Obviously don't use this in production, unless you *want* to make life easy for malicious actors.

That's it, that's all the code we need for a minimal app!

## Step 3: Setup for heroku

You'll need a requirements.txt file to tell Heroku what libraries you need.

`pip freeze > requirements.txt` 

That was easy.  You'll also need a Procfile to tell Heroku what to run:

`echo "web: gunicorn app:app" > Procfile`

It's nice to have a .gitignore, especially if you're also going to put it on github. Here's my minimal Mac users .gitignore: 

```
*.bak
*.pyc
.DS_Store
```

## Step 4: Deploy!

Now you need to commit all this stuff, and then once it's all committed, just `heroku create CHOOSEYOURNAME`.  That will create an application with whatever name you give it in that last argument.  Then `git push heroku master` will get it on Heroku. 

It really is that easy!  The output of the heroku create command, incidentally, should give the url of your app, which will probably be something like chooseyourname.herokuapp.com.

There's one more step, however, and that's to setup the database. Two substeps: 

- Create the database in Heroku. The free flavor is the hobby-dev one (that's where you have a 10,000 row limit). Everything else is actually quite expensive.  That's as simple as `heroku addons:create heroku-postgresql:hobby-dev` 

(If you have more than one database, you'll have to [do a little bit more work](https://devcenter.heroku.com/articles/heroku-postgresql#establish-primary-db) to connect it, but for a simple thing, you shouldn't have to bother.)

- Create the tables in your new database. The easiest way to do this is to just fire up a Python REPL right on the heroku server and [create the tables from within the app](http://flask-sqlalchemy.pocoo.org/2.1/api/#flask.ext.sqlalchemy.SQLAlchemy.create_all): `heroku run python` from your command line, and then, in the repl, `from app import db` and `db.create_all()`. 

**That's it!  You're done!** If everything went well, the app should be live and functioning.

## What Now?

- If you want to update the (non-databasey) code in the application, it's as simple as pushing new changes to the Heroku remote. 

- To see the logs, go to the repo and do `heroku logs`.

- To get a database shell, do `heroku pg:psql`.

- To see the data online, probably the easiest approach is to use [dataclips](https://devcenter.heroku.com/articles/dataclips) to see saved queries.

- To actually get the data locally, you can use `heroku pg:pull` to pull down data locally. See the Heroku Postgres docs linked below for more details on what to give to that command. 

## Wait a minute, what about local dev, testing, etc.?

I'm not going to cover all that stuff, because this post is too long as is, but it's probably a good idea to set up local Postgres with an actual connection so you can test before deploying. For more information, see the references below. 

Some people create separate git branches for the local version of the application and the Heroku version. 

## Useful References

- [Heroku Python guide](https://devcenter.heroku.com/articles/getting-started-with-python#introduction)

- [Heroku Postgres guide](https://devcenter.heroku.com/articles/heroku-postgresql#provisioning-the-add-on)

- [A really nice tutorial, but slightly obsolete on Heroku stuff](http://blog.sahildiwan.com/posts/flask-and-postgresql-app-deployed-on-heroku/) --- also starts off with using a local postgres installation for testing, which you might want to do. 

- [Explore Flask](http://exploreflask.com/en/latest/deployment.html)

- [A nice Flask + Postgres tutorial with lots of SQLAlchemy troubleshooting info](https://www.theodo.fr/blog/2017/03/developping-a-flask-web-app-with-a-postresql-database-making-all-the-possible-errors/)

- [A detailed but obsolete Heroku/Flask/Postgres tutorial, with migrations!](https://realpython.com/blog/python/flask-by-example-part-2-postgres-sqlalchemy-and-alembic/)

- [A short and easy to read explanation of getting Postgres working with python web frameworks](http://killtheyak.com/use-postgresql-with-django-flask/)

- [The Flask Mega-Tutorial](https://blog.miguelgrinberg.com/post/the-flask-mega-tutorial-part-i-hello-world)

- [Official Flask tutorial](http://flask.pocoo.org/docs/0.12/tutorial/)
