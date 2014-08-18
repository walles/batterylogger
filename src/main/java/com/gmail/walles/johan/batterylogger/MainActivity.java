package com.gmail.walles.johan.batterylogger;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {
    public static final String TAG = "BatteryLogger";
    public static final int REFRESH_PLOT_EVERY_MINUTES = 60;

    private long lastShown = -(24 * 60 * 60 * 1000);

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) {
            return;
        }

        refreshBatteryPlotFragmentIfNeeded();
    }

    private void refreshBatteryPlotFragmentIfNeeded() {
        final long now = SystemClock.elapsedRealtime();

        long minutesSinceLastShow = (now - lastShown) / (1000 * 60);
        if (minutesSinceLastShow < REFRESH_PLOT_EVERY_MINUTES) {
            return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new BatteryPlotFragment())
                .commit();
        lastShown = now;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemSamplingService.enable(this);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            refreshBatteryPlotFragmentIfNeeded();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
