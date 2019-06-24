package com.example.mobile_hand_android;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.lite.Interpreter;
import android.support.v4.os.TraceCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.wonderkiln.camerakit.CameraListener;
import com.wonderkiln.camerakit.CameraView;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private CameraView cameraView;
    private ImageView imageViewResult;

    private static final int INPUT_SIZE = 256;
    private int[] intValues;
    private int[] intValues_tip;
    private float[] floatValues;
    private float[] floatValues_tip;

    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output_new";
    private static final String MODEL_PATH = "graph.lite";

    private Interpreter tflite;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intValues = new int[INPUT_SIZE * INPUT_SIZE];
        intValues_tip = new int[32 * 32];
        floatValues = new float[INPUT_SIZE * INPUT_SIZE * 3];
        floatValues_tip = new float[32 * 32 * 3];
        final Matrix matrix=new Matrix();
        matrix.postScale(1f, 1f);
        matrix.postRotate(90);

        cameraView = (CameraView) findViewById(R.id.camera);
        imageViewResult = (ImageView) findViewById(R.id.Result);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mRecyclerView.setLayoutManager(linearLayoutManager);
        ArrayList<Model> datas = initData();
        HomeAdapter homeAdapter = new HomeAdapter(MainActivity.this, datas);
        mRecyclerView.setAdapter(homeAdapter);


        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    /** Closes tflite to release resources. */
    public void close() {
        tflite.close();
        tflite = null;
    }
    protected ArrayList<Model> initData() {
        ArrayList<Model> mDatas = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Model model = new Model();
            model.type = i;
            model.resId = resIds[i];
            mDatas.add(model);
        }
        return  mDatas;
    }
    private int[] resIds= {R.drawable.ic_launcher, R.drawable.bossk, R.drawable.crayon, R.drawable.cubist,
            R.drawable.denoised_starry, R.drawable.feathers, R.drawable.ink, R.drawable.mosaic, R.drawable.scream,
            R.drawable.udnie, R.drawable.wave};
    public class Model {
        public int type;
        public int resId;
    }
    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }
}
