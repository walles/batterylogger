This app will log and plot battery drain speed up to one month
back. The plot will include markers for when things like app or OS
upgrades have happened.

The idea then is that using this information it should be possible to
find out what makes battery usage increase.

TODO
----
* Implement the Androidplot quickstart tutorial:
http://androidplot.com/docs/quickstart/

* Make sure we can show a graph with holes in it.

* Make sure we can put markers in the graph.

* Make sure the graph is zoomable (by swiping vertically) and pannable
(by swiping sideways).

* Make a data holder class that supports both receiving data,
persisting it and presenting it to AndroidPlot.

* Make a service that updates the data holder class.

* Make sure the service starts on reboot.

* Make sure the service notifies the data holder about shutdowns and
reboots.

* Make sure the service notifies the data holder about charger plugins
and disconnects.
