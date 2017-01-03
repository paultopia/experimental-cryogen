{:title "Starting to distrust Homebrew..."
 :layout :post
 :date "2016-12-28"
 :executable false
 :tags  ["homebrew" "osx" "package management"]}
 
 
Is it just me, or has Homebrew been getting super-bossy recently about how you run your machine?  
 
## Round 1: Update XCode you must, padawan...

The problems started with [this issue I filed](https://github.com/Homebrew/brew/issues/1131). The story behind that issue is this: I upgraded my machine to Sierra, and brew immediately started throwing a fatal error. It wouldn't allow any use of brew at all unless one's XCode is updated to at least the latest version. 
 
Now, that's a problem, for several reasons. First, XCode is a massive, multi-gigabyte, failure-prone download that can take days to actually get if you live, as I do, in the rural Midwest with a corrupt internet service oligopoly. I'm convinced that installation the first time around actually crashed my SSD.
 
Second, it turns out, after discussion in that thread, that XCode *isn't actually a dependency* for the vast majority of Homebrew packages. So this forced multi-gigabyte constantly breaking download is for a tiny handful of packages. 

This forced upgrade only applies if you actually have XCode on your system. So the maintainers of actually decided that their users are to have exactly two choices: have no XCode at all, or have the latest version.  And they made that decision based on a dependency that only applies to a small percentage of their packages (which, of course, aren't disclosed in any place I can find). 

Ok, so that was really annoying.  But, ultimately, Homebrew is just a bunch of Ruby scripts. And while I've never written a line of Ruby in my life, it's pretty easy to read, and I do know Python, which is close enough.  So I went digging. 

I found the file where this behavior is specified. And with a one-line edit to convince Homebrew into thinking that the current installed version of XCode is the installed version (all described in that issue linked at the top), all was well again. For a while.

## Round 2: Update Homebrew you must, young Sith...

Fast forward a few weeks. I did install XCode 8, which surprisingly worked. And I went to upgrade a package. Oops, homebrew now wants XCode 8.1.  And I don't have time to download more endless gigs in order to upgrade a like 5mb package. So I head on over to /usr/local/Homebrew/Library/Homebrew/os/mac/xcode.rb, fire up vim, and edit the script again in order to convince brew that XCode 8 is the latest version.  Then I re-run my command. 

*!^%$!!^$&% WTF? It's still throwing an XCode version error?*  Yep, now it turns out that Homebrew auto-updates itself *every time you use it,* as the first task, and then the auto-updating overwrites the changes I made to the XCode version file. 

"Ok, fine, this is an easy fix," I think.  "I'll just set xcode.rb to be not writable." 

*!^%$!!^$&% WTF? It's still throwing an XCode version error?!!?*  Go back, check xcode.rb, and discover the write bit has been set again. Brew ever-so-helpfully elevated its own permissions in order to boss me around. 

"Ok, a little bit harder of a fix," I think. "I'll just change the owner too.  Try and change files owned by root, I dare you." 

*GARGHHG!!!  STILL THROWING THE XCODE ERROR!* And the ownership has been changed back. (I always thought [part of the goddamn point of Homebrew was that it didn't need root privs to run it](https://github.com/Homebrew/brew/blob/master/docs/FAQ.md#why-does-homebrew-say-sudo-is-bad-)? And indeed, [it supposedly can't even be run as root](https://github.com/Homebrew/brew/pull/1452). But yet it seems to magically help itself to quite privileged behavior when it wants to force update itself without the user's consent.)

This seems *really* problematic. Users don't get to choose to use an older version of the tool anymore? And don't get to restrict its behavior at all? This strikes me as a major security problem.

## Round 3: package management, except when it doesn't.

Fast forward a few more weeks. So today, I decided to try ProjectLibre. It has a Homebrew cask, so let's live dangerously.  First, I decided to look in on my currently installed casks with `brew cask list`.  Here's what I get: 
 
> Warning: The default Caskroom location has moved to /usr/local/Caskroom.

>Please migrate your Casks to the new location and delete /opt/homebrew-cask/Caskroom,
>or if you would like to keep your Caskroom at /opt/homebrew-cask/Caskroom, add the
>following to your HOMEBREW_CASK_OPTS:
>
>  --caskroom=/opt/homebrew-cask/Caskroom
>
>For more details on each of those options, see https://github.com/caskroom/homebrew-cask/issues/21913.
>beaker                                                       java                                                         xquartz

Ok, what's this?  First of all, I don't even remember installing Java via brew, but maybe I did. It would have made sense, given how [terrifying](http://opensource.stackexchange.com/a/1422) Oracle [is being](http://www.theregister.co.uk/2016/12/16/oracle_targets_java_users_non_compliance) about Java, and the damn near impossibility even for a [*actual lawyer*](http://paul-gowder.com/) of figuring out from the [java download page](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) which version of what I'm downloading and what contracts I'm agreeing to by doing so.  

Looking at [the noted github issue](https://github.com/caskroom/homebrew-cask/issues/21913), there is, no explanation of this change. But there is new information: apparently migrating your casks to the new location, as suggested by the warning, isn't so easy as all that. If you installed your packages *after* a commit that was apparently merged on May 31, 2016, then you're ok, you can just move the directory the casks are located in.  But if you installed anything before that, it's symlinked, and you have to manually uninstall and reinstall.

So apparently it wants me to delete the entire (200mb-ish) JDK and re-install it because the maintainers decided to change the location where packages were installed (*multiple times, recently*, if you count the symlink change) and aren't willing to provide an automated migration solution, or set some option in an environment variable that will probably get deprecated (or just flat-out dropped) in the next version? Which, remember, you can't choose not to use, because now it force auto-updates. 
 
## Adding privacy violations to security violations.

Also, Homebrew [evidently tracks its users with google analytics](https://chr4.org/blog/2016/04/26/homebrew-betrayed-us-all-to-google/) as of April or thereabouts. And, this [opt-out behavior isn't disclosed to users](https://www.reddit.com/r/programming/comments/4gj664/homebrew_enabled_tracking_with_google_analytics/), and I only found out about it by poking through a Hacker News thread. (There's some suggestion that maybe it got disclosed eventually, in some update, but I don't remember seeing the disclosure...)

Valeri Karpov has some [strong words about Homebrew](http://thecodebarbarian.com/i-dont-want-to-hire-you-if-you-cant-reverse-a-binary-tree), and I confess, I'm starting to feel the same way. Maybe it's time to try to figure out how to migrate to Nix...

