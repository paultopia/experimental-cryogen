{:title "The Easy Way to Get Your Data from Heroku Postgres (in Python)"
 :layout :post
 :date "2018-04-25"
 :executable false
 :tags  ["python" "heroku" "databases" "postgres"]}
 

Just a tiny little tidbit here. I do a lot of data collection in one-off Heroku apps, and one annoying thing that I find myself having to deal with is getting data in and out.

Generally, if you want to upload or download your data in its entirety, you can either use Heroku's `pg:push` and `pg:pull` commands [see documentation](https://devcenter.heroku.com/articles/heroku-postgresql#pg-push-and-pg-pull) to transmit data between remote and local Postgres instances. You can also use [Postgres backups](https://devcenter.heroku.com/articles/heroku-postgres-import-export). To download a CSV, you can [connect directly to psql](https://jamesbedont.com/2016/08/21/exportpg.html) (psql is the postgres shell). And to upload a CSV, again in psql, you can do something like:

```
\copy yourtable from 'your/absolute/path/filename.csv' with (format csv);
select setval('yourtable_id_seq', max(id)) from yourtable;
```

where the second line (assuming you have a primary key as a serial named "id") resets the primary key to the end of the uploaded CSV, so that you can add more rows without getting key conflicts.

But suppose you have a lot of data, and you don't want to move around the whole thing.  You can copy the results of a specific query to a CSV (see the link above about downloading a csv). And, of course, you can tinker with the application you have on Heroku already to provide some in-application way to access data. But sometimes that's inconvenient, and you want to just interact directly with the database from local code.

Fortunately, it turns out that's pretty easy. [Heroku gives you a database url accessible from the internet](https://devcenter.heroku.com/articles/connecting-to-heroku-postgres-databases-from-outside-of-heroku), and it's pretty trivial to fetch that from local code and run queries right against the database from your local machine.  Here's some example code, using the standard Python Postgres driver, [psycopg2](http://initd.org/psycopg/).

```python

HEROKU_APP_NAME = "your_app_name_in_heroku"
TABLE_NAME = "the_table_you_want_to_query_in_this_example"
import subprocess, psycopg2

conn_info = subprocess.run(["heroku", "config:get", "DATABASE_URL", "-a", HEROKU_APP_NAME], stdout = subprocess.PIPE)
connuri = conn_info.stdout.decode('utf-8').strip()
conn = psycopg2.connect(connuri)
cursor = conn.cursor()
cursor.execute(sql.SQL("SELECT COUNT(*) FROM {};").format(sql.Identifier(TABLE_NAME)))
count = cursor.fetchall()
print(count)

```

And it's that easy! Note that I composed the SQL string using [psycopg2's special tool for that purpose](http://initd.org/psycopg/docs/sql.html#module-psycopg2.sql), mainly because you can only pass values, not table names, with the ordinary [placeholder/string paramaterization functionality](http://initd.org/psycopg/docs/usage.html#passing-parameters-to-sql-queries) and it squicks me out to use basic formatting strings anywhere near SQL.

If you want to make it even easier, you can also do a query right from Pandas and have everything land nice and tidy in a DataFrame, though it requires going through SQLAlchemy first. As follows: 

```python

from sqlalchemy import create_engine
engine = create_engine(connuri)
raw_engine = engine.raw_connection()
my_df = pd.read_sql_query("SELECT * FROM your_table;", raw_engine)

```

and there you go!
