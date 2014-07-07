This app will log and plot battery drain speed up to one month
back. The plot will include markers for when things like app or OS
upgrades have happened.

The idea then is that using this information it should be possible to
find out what makes battery usage increase.

TODO
----
* Make a service that updates the data holder class with battery level
change events.

* Make sure the service starts on reboot.

* Make sure the service notifies the data holder about charger connects
and disconnects.

* Make sure the service notifies the data holder about shutdowns and
reboots.

* Manually test cross-reboot statistics gathering to see that we can
handle starting, stopping, continuing charging or not-charging while
down.

* Make sure the service notifies the data holder about app upgrades,
installs and uninstalls.

* Make sure the service notifies the data holder about OS / kernel
upgrades.

* See if we can detect enabling / disabling of Google Now.

* Make sure we rotate the history file when it gets too big. Aim for
keeping a month or more of stats at hand.

* Find out how to find model X-coordinate for an in-graph click. This
would be used for showing event details when we click them. Try using
ValPixConverter:
http://androidplot.com/javadoc/0.6.0/com/androidplot/util/ValPixConverter.html

DONE
----
* Make Gradle download Androidplot itself rather than having a static
copy in /libs

* Implement the Androidplot quickstart tutorial:
http://androidplot.com/docs/quickstart/

* Make sure we can push to googledrive.

* Make sure we can show a graph with holes in it.

* Make sure we can label individual points. This would be used for
showing events in the graph. Try the PointLabeler as described here:
http://androidplot.com/point-labeling-tools-in-0-5-1/

* Make sure the graph is zoomable (by dragging vertically) and
pannable (by dragging sideways). Have a look at
http://androidplot.com/docs/how-to-pan-zoom-and-scale/ but use the
system GestureDetector instead of rolling our own.

* Make a data holder class that supports both receiving data,
persisting it and presenting it to AndroidPlot.

* Make sure we can handle the case when the battery gets pulled on us
followed by a restart.
