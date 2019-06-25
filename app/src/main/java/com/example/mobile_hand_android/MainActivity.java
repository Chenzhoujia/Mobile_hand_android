package com.example.mobile_hand_android;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.tensorflow.lite.Interpreter;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.wonderkiln.camerakit.CameraListener;
import com.wonderkiln.camerakit.CameraView;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private CameraView cameraView;
    private ImageView imageViewResult;

    private int[] intValues;
    private int[] intValues_tip;
    private float[] floatValues;
    private float[] floatValues_tip;
    private float[][] labelProbArray = null;

    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output_new";
    private static final String MODEL_PATH = "ae-basic.lite";
    private static final String TEST_DATA_PATH = "drawable/test_image.txt";
    private float[][] test_data = null;

    /** Dimensions of inputs. */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    static final int DIM_IMG_SIZE_X = 32;
    static final int DIM_IMG_SIZE_Y = 32;
    private static final int INPUT_SIZE = 256;

    private static final String TAG = "Chen Debug info";

    private Interpreter tflite;
    private ByteBuffer imgData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //准备测试数据
        InputStream inputStream = getResources().openRawResource(R.raw.test_image);
        test_data = new float[15][784];
        getString(inputStream);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intValues = new int[INPUT_SIZE * INPUT_SIZE];
        intValues_tip = new int[32 * 32];
        floatValues = new float[INPUT_SIZE * INPUT_SIZE * 3];
        floatValues_tip = new float[32 * 32 * 3];
        labelProbArray = new float[15][784];

        imgData =
                ByteBuffer.allocateDirect(
                        4 * 15 * 784);
        imgData.order(ByteOrder.nativeOrder());

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

        cameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);

                Bitmap bitmap0 = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                Bitmap bitmap = Bitmap.createScaledBitmap(bitmap0, 32, 32, false);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,true);
                float time0 = (float) System.currentTimeMillis();
                //convertBitmapToByteBuffer(bitmap);
                convertarrayToByteBuffer();
                tflite.run(imgData, labelProbArray);
                //Bitmap bitmap_out = stylizeImage(bitmap);
                float time1 = (float) System.currentTimeMillis();

                //bitmap_out = Bitmap.createScaledBitmap(bitmap_out, bitmap0.getWidth(), bitmap0.getHeight(), false);
//                imageViewResult.setImageBitmap(bitmap_out);
//                imageViewResult.setVisibility(View.VISIBLE);
                Log.i(TAG, "Time before: " + time0);
                Log.i(TAG, "Time after: " + time1);
            }
        });

        homeAdapter.buttonSetOnclick(new HomeAdapter.ButtonInterface() {
            @Override
            public void onclick(View view, MainActivity.Model model) {

                Toast.makeText(MainActivity.this, "更换模型中", Toast.LENGTH_SHORT).show();
                if (model.type == 0){
                    cameraView.toggleFacing();
//                  imageViewResult.setVisibility(View.GONE);
                }
                else {
                    try {
                        tflite = new Interpreter(loadModelFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    cameraView.captureImage();
                }
            }
        });
        cameraView.setCropOutput(true);
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
    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        long startTime = SystemClock.uptimeMillis();

        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                float r = ((val >> 16) & 0xFF),g = ((val >> 8) & 0xFF),b = ((val) & 0xFF);
                r = r/255.f-0.5f;
                g = g/255.f-0.5f;
                b = b/255.f-0.5f;
                imgData.putFloat(r);
                imgData.putFloat(g);
                imgData.putFloat(b);

                floatValues[(i*DIM_IMG_SIZE_X+j) * 3 + 0] = r;
                floatValues[(i*DIM_IMG_SIZE_X+j) * 3 + 1] = g;
                floatValues[(i*DIM_IMG_SIZE_X+j) * 3 + 2] = b;
            }
        }

        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }
    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertarrayToByteBuffer() {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        for (int i = 0; i < 15; ++i) {
            for (int j = 0; j < 784; ++j) {
                final float val = test_data[i][j];
                imgData.putFloat(val);
            }
        }
    }
    private void getString(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "gbk");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String line;
        int j=0;
        try {
            while ((line = reader.readLine()) != null) {
                String[] strArray = null;
                strArray = line.split(",");
                for(int i=0;i<784;i++){
                    test_data[j][i] = Float.parseFloat(strArray[i]);
                }
                j++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
