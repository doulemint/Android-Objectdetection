/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.stone.androidObjectdetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import com.example.stone.androidObjectdetection.Classifier.*;

import java.util.List;

/**
 * A simple View providing a render callback to other classes.
 */
public class OverlayView extends View {
    private final Paint paint;
    private int frameWidth;
    private int frameHieght;
    private int INPUT_SIZE;
    private int oritation;
    private List<Recognition> results;
    private static final int[] COLORS = {
            Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE,
            Color.parseColor("#55FF55"), Color.parseColor("#FFA500"), Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"), Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"), Color.parseColor("#0D0068")
    };
    private float resultsViewHeight;
    private Logger logger = new Logger();
    private Matrix frameToCanvasMatrix;



    public OverlayView(final Context context, final AttributeSet attrs) {

        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP
                , 15, getResources().getDisplayMetrics()));
        resultsViewHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                112, getResources().getDisplayMetrics());
    }

    public void setResults(final List<Recognition> results, int INPUT_SIZE, int oritation, Size frame) {
        this.results = results;
        this.INPUT_SIZE = INPUT_SIZE;
        this.oritation = oritation;
        frameWidth = frame.getWidth();
        frameHieght = frame.getHeight();
        postInvalidate();
    }

    @Override
    public synchronized void onDraw(final Canvas canvas) {
        final boolean rotated = oritation % 180 == 90;
        final float multiplier = Math.min(canvas.getHeight() / (float) (rotated ? frameWidth : frameHieght),
                canvas.getWidth() / (float) (rotated ? frameHieght : frameWidth));
        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(frameWidth,
                frameHieght,
                (int) (multiplier * (rotated ? frameHieght : frameWidth)),
                (int) (multiplier * (rotated ? frameWidth : frameHieght)),
                oritation, false);

        int  i = 0;
        if (results != null) {
            for (Recognition result : results) {
                i++;
                if (result.getLocation() == null) {
                    continue;
                    }
                RectF box = reCalcSize(result);
                String title = result.getTitle() + ":" +
                        String.format("%.2f", result.getconfidencee());
                if (i>COLORS.length)
                    i= 0;
                paint.setColor(COLORS[i]);
                canvas.drawRect(box, paint);
                canvas.drawText(title, box.left, box.bottom, paint);
                logger.i("%s:%.2f", result.getTitle(), result.getconfidencee());

            }

        }
    }

    private RectF reCalcSize(Recognition result) {

        RectF box = new RectF();
        frameToCanvasMatrix.mapRect(box, result.getLocation());
        logger.i("Result! Frame: " + result.getLocation() + " mapped to screen:" + box);
        return box;

    }
}
