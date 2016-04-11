This app will log and plot battery drain speed up to one month
back. The plot will include markers for when things like app or OS
upgrades have happened.

The idea then is that using this information it should be possible to
find out what makes battery usage increase.


Installing
----------
Easiest way is to get it from Google Play:

https://play.google.com/store/apps/details?id=com.gmail.walles.johan.batterylogger


Building
--------
Before building the project you need to run render-graphics.sh to
convert gfx/logo.xcf into icon resources.

You also need to add a `fabric.properties` file in the root of the
project:
```
apiSecret=YOUR_BUILD_SECRET
apiKey=YOUR_API_KEY
```
The values can be retrieved from https://fabric.io/settings/organizations
by clicking the (very small) `API Key` and `Build Secret` links.

Keys are available for free, just get yourself a pair.


Releasing
---------
1. Do ```git tag``` and think about what the next version number should be.
2. Do ```git tag version-1.2.3``` to set the next version number.
3. ```env JAVA_OPTS=-Xmx512m ./gradlew --no-daemon build```
4. ```git push --tags```
5. Upload ```build/outputs/apk/BatteryLogger-release.apk``` to Google Play


TODO
----
* Enable making debug builds without Crashlytics keys.

* Add a tab where we list installed apps and the average drain speed
over the time each app has been installed (much like the built-in
battery app). Clicking on an app should expand that line and show
per-app-version stats, as well as not-installed stats if available.

* Hide individual samples if we're zoomed out enough. Otherwise, when
samples are plotted really close together they form lines that stand
out more than the averages that people are supposed to look at
primarily.

* Add a .travis.yml to run the unit tests on pull requests and pushes.

* If we change the system clock, think about how that would affect
boot timestamps, the system sampling's reboot detection and the timestamp
logging in general, and the drawing of median lines.

* See if we can detect enabling / disabling of Google Now and log
that. Or maybe when services start / stop in general?

* Make sure there's room for displaying a text event at the far left of
the graph. Right now, any leftmost event is printed mostly outside of the
visible area.

* Collect application-data-cleared events if possible.

* When drawing the drain lines, try to guess the initial charging state by
counting backwards from the first change in charging state.

* Make the Y axis labels look good.


DONE
----
* Make Gradle download Androidplot itself rather than having a static
copy in /libs

* Implement the Androidplot quickstart tutorial:
<http://androidplot.com/docs/quickstart/>

* Make sure we can push to Google Drive.

* Make sure we can show a graph with holes in it.

* Make sure we can label individual points. This would be used for
showing events in the graph. Try the PointLabeler as described here:
<http://androidplot.com/point-labeling-tools-in-0-5-1/>

* Make sure the graph is zoomable (by dragging vertically) and
pannable (by dragging sideways). Have a look at
<http://androidplot.com/docs/how-to-pan-zoom-and-scale/> but use the
system GestureDetector instead of rolling our own.

* Make a data holder class that supports both receiving data,
persisting it and presenting it to AndroidPlot.

* Make sure we can handle the case when the battery gets pulled on us
followed by a restart.

* Make a service that updates the data holder class with battery level
change events.

* Make the main activity plot actual events from the log file.

* Make sure the service notifies the data holder about charger connects
and disconnects.

* Don't keep track of charger state; just store start/stop charging as
informative events and don't report negative drain.

* Make sure the service starts on reboot.

* Make sure the service notifies the data holder about shutdowns and
reboots.

* Tune the zoom sensitivity.

* Make double clicking the graph zoom out as much as possible.

* Make sure the service notifies the data holder about app upgrades,
installs and uninstalls.

* Don't print numbers at the points in the graph

* Zoom around the finger, not around the middle

* Remove the lines between the vertices

* Make the X axis labels look sane at different zoom levels

* Rewrite the data collection service to sample the system state
every 15 minutes and deduct data points from the differences between
those states. The reason is that we can't trust our service to stay
alive (real-life observation).

* Make sure the service notifies the data holder about OS / kernel
upgrades.

* Print proper app names and versions when logging app removals.

* Think about how to show overlapping info events. With the new
sample-based approach we simply don't generate any overlapping
events.

* Draw a median line across each series.

* If the log file doesn't exist or the plot is empty for some other
reason, display something explaining that to the user.

* Make sure we rotate the history file when it gets too big. When it
gets bigger than 400kb, we drop the first 25% of all events.

* Switch to pinch-zoom; the current scheme makes it too hard to scroll
sideways. Try using ScaleGestureDetector for this!

* Think about font size for the what-happened texts in the graph. Should
they be resizable? They should at least default to some size related to
the system font size setting.

* Fake the data when running in the emulator

* Right now if you "exit" the app by pressing the Home button, it will
show stale data the next time you activate it. What should we do about
that? When becoming visible, if the most recent data point is older than
1h or so, reload the history.

* If the sampling service throws an exception, store the exception stack
trace in a world readable file in a world readable directory.

* Make History.createHistory() create its history by synthetic SystemStates
rather than just adding history events.

* Replace the start/stop-charging messages by green lines at Y=0 while
charging?

* Find out why we don't get any lines on the device (but we do get dots)

* Verify that sampling actually starts after rebooting the device.

* Make sure the y=0 lines are visible

* Make an icon.

* Remove the Settings thing, it's empty.

* Make sure the Y axis has units.

* Replace the medians by something else? Averages? Least square approximated
lines?

* Find out why the simulator doesn't show any data. It was because there were
one or more samples, and then we showed them instead.

* Only show text events if we're zoomed in enough, either by the
number of visible events or by a certain amount of time (one day?).

* Make dots less visible; either smaller or with alpha. The point is
that we want the drain lines to be more visible than the dots, even
with a lot of data in the graph.

* Try turning the phone with an almost-empty history. Note how you get
  the same dialog over and over. Fix that so that the dialog only
  shows up once.

* Make sure there's enough room for the tick labels at the bottom;
there wasn't on one of the beta devices.

* Hint people about using two finger zoom get the details back.

* Add a legend

* Double tapping should zoom out fully, or if we're already zoomed out fully we
should zoom in to two days resolution.

* Double tapping should animate the zoom operation so that it's obvious to the
user what's happening.

* Make sure the legend is visible at once during startup. Right now it pops
into view a bit late, forcing a resize of the plot.

* When starting the app, animate a zoom out from about 24h to max to hint users
that zoom affects which in-graph labels are visible.

* When the zoom animation is done, log the time, number of frames and
frames per second.

* Try removing the sample series while zooming and see if that improves
zoom performance.

* Mark restarts by vertical red lines. That would make the
  increasing-battery-use-until-reboot pattern on my phone more
  visible.

* Make sure we're actually removing samples older than one month

* Auto generate version code and version number from git describe.
