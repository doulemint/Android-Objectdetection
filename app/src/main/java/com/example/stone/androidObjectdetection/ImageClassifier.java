package com.example.stone.androidObjectdetection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ImageClassifier implements Classifier{
    private static final float HRESHOLD = 0.1f;
    private static JSONObject object;
    private static int numClasses = 1000;

    static {
        System.loadLibrary("tensorflow_inference");
    }
    private static final String TAG = "ImageClassifier";

    /** Number of results to show in the UI. */
    private static final int RESULTS_TO_SHOW = 3;

    private String inputName;
    private String outputName ;
    private int inputsize ;
    private int imageMean ;
    private float imageStd ;
    private String[] outputNames;

    private float[] floatValues;
    private float[] outputs;
    private int[] intValues;

    private TensorFlowInferenceInterface tf;

;

    private ImageClassifier() { }

    public static Classifier create(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName){
        ImageClassifier c = new ImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;

        c.tf = new TensorFlowInferenceInterface(assetManager,modelFilename);
        try {
            InputStream jsonStream= assetManager.open(labelFilename);
            byte[] jsonData = new byte[jsonStream.available()];
            jsonStream.read(jsonData);
            jsonStream.close();

            String jsonString = new String(jsonData, "utf-8");
            object = new JSONObject(jsonString);
        }
        catch (Exception e){
        Log.e("getLabel",e+".");
         }

        c.inputsize = inputSize;
        c.imageMean = imageMean;
        c.imageStd = imageStd;
        c.outputNames = new String[] {outputName};
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];
        c.outputs = new float[numClasses];

        return c;
    }
    public List<Recognition> recognizeImage(final Bitmap bitmap){
        bitmap.getPixels(intValues,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
        }

            tf.feed(inputName,floatValues,1,inputsize,inputsize,3);
            tf.run(new String[]{outputName});
            tf.fetch(outputName,outputs);

            PriorityQueue<Recognition> pq = new PriorityQueue<Recognition>(3, new Comparator<Recognition>() {
                @Override
                public int compare(Recognition recognition, Recognition t1) {
                    return Float.compare(recognition.getconfidencee(),t1.getconfidencee());
                }
            });

            //find some larger than 0.3
            for (int j = 0; j< outputs.length;++j){
                if (outputs[j] > HRESHOLD){
                    try {
                        pq.add(
                                new Recognition(""+ j , object.getString(String.valueOf(j)), outputs[j],null));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
            int recongitionSize = Math.min(pq.size(),RESULTS_TO_SHOW);
            for (int i = 0; i < recongitionSize; ++i){
                recognitions.add(pq.poll());
            }
        return recognitions;
    }




    @Override
    public String getStatString() {
        return tf.getStatString();
    }

    @Override
    public void close() {
        tf.close();
    }

}
