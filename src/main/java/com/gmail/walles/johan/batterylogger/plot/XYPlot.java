/*
 * Copyright 2016 Johan Walles <johan.walles@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.walles.johan.batterylogger.plot;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class XYPlot extends View {
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private boolean showDrainDots = true;

    public XYPlot(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setXRange(double minX, double maxX) {
        this.minX = minX;
        this.maxX = maxX;
    }

    public void setYRange(double minY, double maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    public void setShowDrainDots(boolean yesOrNo) {
        showDrainDots = yesOrNo;
    }

    /**
     * Convert an X pixel value into a plot X value.
     */
    public double getXVal(float pixelX) {
        // FIXME: Code missing here
    }
}
