package com.example.stone.androidObjectdetection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

public class TFObjectDetectionModel implements Classifier {

    // Only return this many results.
    private static final int MAX_RESULTS = 100;

    // Config values.
    private String inputName;
    private int inputSize;

    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private byte[] byteValues;
    private float[] outputLocations;
    private float[] outputScores;
    private float[] outputClasses;
    private float[] outputNumDetections;
    private String[] outputNames;

    private boolean logStats = false;

    private TensorFlowInferenceInterface tf;

    public static Classifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final int inputSize) throws IOException {
        final TFObjectDetectionModel d = new TFObjectDetectionModel();

        InputStream labelsInput = null;
        String Filename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(Filename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            Log.i("label",line);
            d.labels.add(line);
        }
        br.close();

        d.tf = new TensorFlowInferenceInterface(assetManager,modelFilename);

        final Graph g = d.tf.graph();

        d.inputName = "image_tensor";
        final Operation inputOp = g.operation(d.inputName);
        if (inputOp == null) {
            throw new RuntimeException("Failed to find input Node '" + d.inputName + "'");
        }
        d.inputSize = inputSize;
        final Operation outputOp1 = g.operation("detection_scores");
        if (outputOp1 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_scores'");
        }
        final Operation outputOp2 = g.operation("detection_boxes");
        if (outputOp2 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_boxes'");
        }
        final Operation outputOp3 = g.operation("detection_classes");
        if (outputOp3 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_classes'");
        }
        d.outputNames = new String[] {"detection_boxes", "detection_scores",
                "detection_classes", "num_detections"};
        d.intValues = new int[d.inputSize * d.inputSize];
        d.byteValues = new byte[d.inputSize * d.inputSize * 3];
        d.outputScores = new float[MAX_RESULTS];
        d.outputLocations = new float[MAX_RESULTS * 4];
        d.outputClasses = new float[MAX_RESULTS];
        d.outputNumDetections = new float[1];
        return d;

    }

    public TFObjectDetectionModel() { }

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap) {
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());

        for (int i = 0; i < intValues.length; i++){
            byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            byteValues[i * 3 + 0] = (byte) ((intValues[i] >> 16) & 0xFF);
        }

        tf.feed(inputName,byteValues,1,inputSize,inputSize,3);
        tf.run(outputNames,logStats);

        outputLocations = new float[MAX_RESULTS * 4];
        outputScores = new float[MAX_RESULTS];
        outputClasses = new float[MAX_RESULTS];
        outputNumDetections = new float[1];
        tf.fetch(outputNames[0], outputLocations);
        tf.fetch(outputNames[1], outputScores);
        tf.fetch(outputNames[2], outputClasses);
        tf.fetch(outputNames[3], outputNumDetections);

        final PriorityQueue<Recognition> pq = new PriorityQueue<Recognition>(1, new Comparator<Recognition>() {
            @Override
            public int compare(Recognition recognition, Recognition t1) {
                return Float.compare(recognition.getconfidencee(),t1.getconfidencee());
            }
        });

        for (int j = 0;j < outputScores.length;j++) {
            final RectF detection = new RectF(
                    outputLocations[4 * j + 1] * inputSize,
                    outputLocations[4 * j] * inputSize,
                    outputLocations[4 * j + 3] * inputSize,
                    outputLocations[4 * j + 2] * inputSize);
            pq.add(new Recognition("" + j, labels.get((int) outputClasses[j]), outputScores[j], detection));
        }

            final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
            for (int i=0;i<Math.min(pq.size(),MAX_RESULTS);i++){
                recognitions.add(pq.poll());
            }

        return recognitions;
    }

    @Override
    public String getStatString() {
        return null;
    }

    @Override
    public void close() {

    }
}
