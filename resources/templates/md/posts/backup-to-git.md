{:title "Backup Dropbox, iCloud, etc. to git"
 :layout :post
 :date "2017-01-12"
 :executable false
 :tags  ["shell" "git" "python" "system"]}
 
According to [the Wisdom of the Internet](http://stackoverflow.com/questions/1960799/using-git-and-dropbox-together-effectively), you shouldn't put git repos into Dropbox (at least not without doing too-fancy things like turning Dropbox itself into a git remote, which, no), because everything changes everything else in funny complex ways and things blow up. 
 
But if you're me, sometimes you store, like writing and such, a bunch of stuff in Dropbox (or in iCloud, where I imagine similar issues arise, or in Google Drive, or in Box, or even in bloody Microsoft's OneDrive monstrosity), and want to keep doing so because of nice things like automatic multiple-device syncronization. 

But at the same time, you want version control. And you don't want to rely on Dropbox's opaque versioning system that you have to pay for and where the terms and the UI change all the time. You want GIT, damnit. 

Why not just automatically copy all the files from the Dropbox/iCloud/whev folder to your git repo?  I wrote a quick python script to do so. You can get it [from this gist](https://gist.github.com/paultopia/9b91a9ca00ed489d0d820e76d201dcca).  Just stick it in a cron job or launchd or whatever.  I think I will have mine run every hour when the computer is on.

Now to figure out launchd on mac. [Here's a tutorial](http://alvinalexander.com/mac-os-x/mac-osx-startup-crontab-launchd-jobs) some kind person wrote, let's see how this works...

(I suppose this would be more useful with a separate config file and the ability to pass multiple source directories, maybe even multiple target directories, but that's more work, and this is all I need, so, oh hey open source. :-) ...maybe later).

*Edit*: figured out launchctl. That tutorial above is pretty good and has more details, but here are the basics. 

First `cd $HOME/Library/LaunchAgents`

Then create a plist file in funny apple format. Let's say your filename for the plist is `com.yourname.yourcommand.plist`. And let's say your version of that python script is located at `/Users/yourname/github/yourfile/backup-to-git`. And say you want it to run every hour.  Then this is your plist:

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>com.yourname.yourcommand</string>
  <key>ProgramArguments</key>
  <array>
    <string>/Users/yourname/github/yourfile/backup-to-git</string>
  </array>
  <key>StartInterval</key>
  <integer>3600</integer>
  <key>RunAtLoad</key>
  <true/>
</dict>
</plist>
```

Note that the StartInterval value is the number of seconds between runs. 

Then to get it going without reboot, run `launchctl load com.yourname.yourcommand.plist` (and replace load with unload to stop it).  

Supposedly this will continue working after reboot too. I have faith.
