Library: BibTeX on Android
==========================

<a href="https://f-droid.org/app/com.cgogolin.library" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=com.cgogolin.library" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>

* License:

Library is free software published under the
GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007.

* Build (for dummies):

Install and set up the Android SDK. For more information on how to do this please 
refere to

 http://developer.android.com/training/index.html

Then go the base directory of your local working copy and run:

$ android update project --path . --target 1

You can then perform a debug build by running

$ ant debug

You can now copy the resulting *.apk files from 
the bin directory to your Android device and
install them.
