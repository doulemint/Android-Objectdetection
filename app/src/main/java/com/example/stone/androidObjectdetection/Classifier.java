package com.example.stone.androidObjectdetection;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

public  interface Classifier {
    //the class for list
    public class Recognition{
        private final String id;
        private final String title;
        private final Float confidence;

        public Recognition(String id, String title, Float confidence, RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        private RectF location;

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getconfidencee() {
            return confidence;
        }

        public RectF getLocation() {
            return location;
        }
        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }

//methods going to be accomplished class==list<class>
    List<Recognition> recognizeImage(Bitmap bitmap);

    String getStatString();

    void close();

}
