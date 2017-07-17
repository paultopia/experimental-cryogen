{:title "Avoiding Inheritance Through Really Unidiomatic Python"
 :layout :post
 :date "2017-07-17"
 :executable false
 :tags  ["python" "functional programming" "object-oriented programming" "closures"]}
 
So, confession: my brain works in a functional way, and really doesn't work in an object-oriented way. This can be a bit of an issue when you're using an object-oriented language and trying to avoid excessive code duplication.  

So here's some really unnatural stuff I just did. The problem: I'm writing a Python library to wrap a bunch of legal and political APIs with a simpler interface. ([Extreme work in progress.](https://github.com/paultopia/lawpy))  I had a bunch of code that looked very similar. For example, this was what two of my session objects (interfaces to different APIs) looked like, in relevant part:

```python
class courtlistener(object):
    def __init__(self, api_key="ENV"):
        if api_key == "ENV":
            try:
                self.api_key = os.environ['COURTLISTENER']
            except KeyError as e:
                raise Exception("API key is missing. Please set the COURTLISTENER environment variable or pass the key to the session constructor.") from e
        else:
            self.api_key = api_key
        self.auth_header = {'Authorization': 'Token ' + self.api_key}
        self.total_requests_this_session = 0

    def request(self, endpoint="", headers={}, parameters=None):
        if endpoint.startswith("https://"):
            ep = endpoint
        else:
            ep = "https://www.courtlistener.com/api/rest/v3/" + endpoint
        h = {}
        h = safe_merge(h, headers)
        h = safe_merge(h, self.auth_header)
        result = requests.get(ep, headers=h, params=parameters)
        self.total_requests_this_session += 1
        result.raise_for_status()
        return result.json()

class propublica(object):
    def __init__(self, api_key="ENV"):
        if api_key == "ENV":
            try:
                self.api_key = os.environ['PROPUBLICA']
            except KeyError as e:
                raise Exception("API key is missing. Please set the COURTLISTENER environment variable or pass the key to the session constructor. You can get an API key directly from courtlistner.com by registering on their website.") from e
        else:
            self.api_key = api_key
        self.auth_header = {'X-API-Key': self.api_key}
        self.total_requests_this_session = 0

    def request(self, endpoint="", headers={}, parameters=None):
        if endpoint.startswith("https://"):
            ep = endpoint
        else:
            ep = "https://api.propublica.org/congress/v1/" + endpoint
        h = {}
        h = safe_merge(h, headers)
        h = safe_merge(h, self.auth_header)
        result = requests.get(ep, headers=h, params=parameters)
        self.total_requests_this_session += 1
        result.raise_for_status()
        return result.json()

```

Ugh, horrible, right?  The only difference between those two chunks of code is a URL and the name of an environment variable. Each class, of course, has other stuff too---methods specific to the API that the class wraps, natch---but that stuff is just pure fat that needs to be trimmed.  (FYI, for those who are checking, `safe_merge` is just a utility function I have in there to merge two dicts without overwriting data with empty stuff, mutating the originals, etc.)

So how do we fix this?  I imagine that the standard OOP solution would involve either creating some kind of higher-level class, maybe call it baserequest or something, and give that most of the architecture, and subclass it for courtlistener and propublica. Or, I guess, use some kind of explicit object composition. 

But I'm really only using classes here at all because I want other people to use this library, and I think people kinda expect the surface area of a library like this to be "initialize an object then call methods on it to get and manipulate your data." But for the internal implementation details, I'm a lot more comfortable with functional idioms, even if they're unidiomatic in python. So here's the solution I came up with: 

```python
def session_builder(selfvar, keyenv):
    def class_init(selfvar, api_key="ENV"):
        if api_key == "ENV":
            try:
                selfvar.api_key = os.environ[keyenv]
            except KeyError as e:
                raise Exception("API token is missing. Please set the {} environment variable or pass the token to the session constructor.".format(keyenv)) from e
        else:
            selfvar.api_key = api_key
        selfvar.auth_header = {'X-API-Key': selfvar.api_key}
        selfvar.total_requests_this_session = 0
    return class_init

def request_builder(selfvar, baseurl):
    def request(selfvar, endpoint="", headers={}, parameters=None):
        if endpoint.startswith("https://"):
            ep = endpoint
        else:
            ep = baseurl + endpoint
        h = {}
        h = safe_merge(h, headers)
        h = safe_merge(h, selfvar.auth_header)
        result = requests.get(ep, headers=h, params=parameters)
        selfvar.total_requests_this_session += 1
        result.raise_for_status()
        return result.json()
    return request

class propublica(object):

    def __init__(self):
        session_builder(self, "PROPUBLICA")(self)

    def request(self, endpoint="", headers={}, parameters=None):
        return request_builder(self, "https://api.propublica.org/congress/v1/")(self, endpoint, headers, parameters)

class courtlistener(object):
    def __init__(self):
        session_builder(self, "COURTLISTENER")(self)

    def request(self, endpoint="", headers={}, parameters=None):
        return request_builder(self, "https://www.courtlistener.com/api/rest/v3/")(self, endpoint, headers, parameters)
```

(Please ignore the bug for now in that I forgot that the details of the auth header also change across APIs, that gets fixed below and isn't germane to the key point.)

Can we have a hearty *BWAHAHAHAHAAHAHA!!!!!*?  Look at all that code I get to delete, and I get to leave off lots more code as I add more APIs! 

So what this is doing is that for each of these methods, it's taking an externally defined function and closing over the class-specific data (the URLs, environment variables for API keys, etc.), then immediately invoking them (javascript-style, I suppose). 

However, this solution isn't perfect either.  First of all, it's adding some overhead, since it actually creates a new function every time the request method is called, and immediately invokes it. Apparently there's a version that [leverages more of Python's internals](https://stackoverflow.com/a/38549072/4386239), but that seems, to me, less readable?  (Probably more readable to more pythonic people.)

This is more difficult because python does some kind of magic with passing the instance in with self.  I tried to go around the problem by defining request within the constructor, i.e.: 

```python
def session_builder(selfvar, keyenv, baseurl):
    def class_init(selfvar, api_key="ENV"):
        if api_key == "ENV":
            try:
                selfvar.api_key = os.environ[keyenv]
            except KeyError as e:
                raise Exception("API token is missing. Please set the {} environment variable or pass the token to the session constructor.".format(keyenv)) from e
        else:
            selfvar.api_key = api_key
        selfvar.auth_header = {'X-API-Key': selfvar.api_key}
        selfvar.total_requests_this_session = 0
        selfvar.request = request_builder(selfvar, baseurl)
    return class_init
```

but apparently the python magic didn't go far enough to pass the self variable to a method defined that way: `TypeError: request() missing 1 required positional argument: 'selfvar'`.  Alas.

However, it occurred to me that I don't *really* need the request method to mess with any internal state in the object it belongs to. I was tracking request counts only in order to squash a bug several iterations of the method ago, and that's the only thing that really requires getting at any state. So, why not rewrite to be completely free of internal state, and just close over the authentication information as well?

Thus, the final code! 

```python
def request_builder(auth_header, baseurl):
    def request(endpoint="", headers={}, parameters=None):
        if endpoint.startswith("https://"):
            ep = endpoint
        else:
            ep = baseurl + endpoint
        h = {}
        h = safe_merge(h, headers)
        h = safe_merge(h, auth_header)
        result = requests.get(ep, headers=h, params=parameters)
        result.raise_for_status()
        return result.json()
    return request

def session_builder(selfvar, keyenv, baseurl, keyheader, key_prefix=""):
    def class_init(selfvar, api_key="ENV"):
        if api_key == "ENV":
            try:
                selfvar.api_key = os.environ[keyenv]
            except KeyError as e:
                raise Exception("API token is missing. Please set the {} environment variable or pass the token to the session constructor.".format(keyenv)) from e
        else:
            selfvar.api_key = api_key
        auth_header = {keyheader: key_prefix + selfvar.api_key}
        selfvar.request = request_builder(auth_header, baseurl)
    return class_init
    
class courtlistener(object):

    def __init__(self):
        session_builder(self, "COURTLISTENER", "https://www.courtlistener.com/api/rest/v3/", 'Authorization', 'Token ')(self)

class propublica(object):

    def __init__(self):
        session_builder(self, "PROPUBLICA", "https://api.propublica.org/congress/v1/", 'X-API-Key')(self)
```

Now it's even more stateless and functional, has even fewer lines of code, and allows the effortless creation of wrappers for authentication and basic requests for the other APIs I want to wrap. Plus if I want to add more error handling or whatnot to the requests, I can do it all in one place.  With no subclassing. 

Unidiomatic functional programming in Python! 
