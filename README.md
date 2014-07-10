This app will log and plot battery drain speed up to one month
back. The plot will include markers for when things like app or OS
upgrades have happened.

The idea then is that using this information it should be possible to
find out what makes battery usage increase.

TODO
----
* Make sure the service notifies the data holder about OS / kernel
upgrades.

* Make sure we rotate the history file when it gets too big. Aim for
keeping a month or more of stats at hand.

* If the log file doesn't exist or the plot is empty for some other
reason, display something explaining that to the user.

* Make a preference for disabling the service.

* Print proper app names and versions when logging app removals.

* Force landscape mode? Drain patterns are more visible if the Y axis
is more compressed.

* See if we can detect enabling / disabling of Google Now and log
that. Or maybe when services start / stop in general?

* Fake the data when running in the emulator?

* Make the points in the graph real big, bigger than the line thickness.
Ref: http://stackoverflow.com/questions/10169080/custom-points-on-graph-using-androidplot

* Think about how to show overlapping info events.

* Detect unclean shutdowns of the EventListener service and do something
appropriate

* Make the Y axis labels look good.

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
