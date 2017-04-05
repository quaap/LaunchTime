LaunchTime
=========

LaunchTime is an alternative home/homescreen/launcher for Android devices. Its main feature is a
side menu used to organize your apps into common-sense and configurable categories. It also
features support for widgets, text search for apps, a QuickBar, links/shortcuts, recent apps list, 
app hiding, color selcetion, and portrait and landscape support.


Version 0.5.2 is available in [F-Droid](https://f-droid.org/repository/browse/?fdid=com.quaap.launchtime).

Update 2017-04-05: If you downloaded the app from the Google Play Store from "A-Z Man": That is NOT me. MY app contains NO ads. That person added them and is not complying with the GPL by not release the source, among other things.  My app is offered through Fdroid.

Features
----------
* Configurable app categories in a scrollable side menu.
* Scrolling QuickBar on the bottom for easy access to your favorite apps.
* Text search to find apps.
* List of recent apps.
* Some widget support (no resizing yet)
* Rearrange/sort icons and menu items.
* Shortcut/link support.
* Supports both portrait and landscape mode.
* Can hide apps you don't want but can't uninstall.
* Back up and restore your settings.

Categorization
----------
On the first run, or when a new app is installed, an attempt is made to place each app into its
proper category.  However,  many apps could go into several categories, and Android provides no
way to categorize apps, so, many apps will end up miscategorized, or in the "Other" category.
If you'd like to help the situation, please categorize your apps, go to the settings menu and 
click "Send app data", which will send me information about what apps you have installed and 
how you have categorized them.

Permissions
----------
* Internet permission for (manual) usage reporting and feedback, and (manual) crash reporting.
* Storage permission for backups.

Data is never submitted without explicit user permission. Submitted data is as anonymized as
possible, and no one but the app author sees the submitted data. It is only used to improve the
app, never sold or used nefariously. (I'm just some guy who programs for fun.)
If you don't want to use those featues you can skip them, and you can disable the "storage" 
permissions in the Application Manager.


QuickBar
----------
The QuickBar is located across the bottom of the screen.
* Populated automatically on install.
* Long click icon and move to rearrange or remove items.
* Long click icons and drag icons to add.
* Can scroll horizontally to add as many as desired.


Categories
----------
* Long click and move to rearrange.
* On any category, click the up arrow button at the bottom right to show options:
  * Add widgets.
  * Add a new category.
  * Rename/Edit the current category.
  * Delete the current category (if available).
* The category list will scroll vertically if needed.


Apps
----------
* To move an app, long click and hold/drag.
* If you drop the app...
  * On a category: moves the app to the category.
  * On the QuickBar: creates an quick link in the QuickBar.
  * On the uninstall/remove area: if the source was the QuickBar or the Recent list, the link is
  removed, otherwise, the app is uninstalled.

Known issues
----------
* Not all apps are automatically sorted properly, or at all.
* Auto-screen-scrolling while dragging is a bit choppy.
* Widgets cannot be manually resized.
* No labels on widgets.
* Widget needing configuration don't always work.
* Labels on apps don't always reflect changes when the app upgrades and changes the label.


Credits
--------
I developed this after testing and using several alternative FOSS Launchers, the most influential
was @Silverfish, from which I borrowed some ideas and some code (mainly widget and package
categorization code).  I decided to develop my own instead of adding to it because we have
different design goals for a few important features (ie single vs multiple widgets,
unified homescreen vs app-drawer, etc)  If this launcher doesn't suit you, I encourage you
try it!
