package org.tensorflow.lite.examples.classification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MyActivity extends AppCompatActivity {
    private static final int TAKE_PICTURE = 1;
    private Uri imageUri;
    private Uri uri=null;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {



            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);


        }
    }

    private void cropPhotoAndZoom() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        Uri uri1 = uri;
        intent.setDataAndType(uri1, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", "true");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri1);
        intent.putExtra("return-data", "false");
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", "true"); // no face detection
        startActivityForResult(intent, 2);
    }

    public static Bitmap toBigZoom(String path, float x, float y) {
        Log.e("bitmaputil", "path---" + path + "--x--y--" + x + "--" + y);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            float sx = 0;
            float sy = 0;
            if ((float) w / h >= 1) {
                sx = (float) y / w;
                sy = (float) x / h;
                Log.e("bitmaputil---", "w/h--->=1");
            } else {
                sx = (float) x / w;
                sy = (float) y / h;
                Log.e("bitmaputil---", "w/h---<1");
            }
            Matrix matrix = new Matrix();
            matrix.postScale(sx, sy); // 长和宽放大缩小的比例
            Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
            Log.e("bitmaputil---", "w---" + resizeBmp.getWidth() + "h--" + resizeBmp.getHeight());
            return resizeBmp;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Button jumpButton = (Button) findViewById(R.id.button);
        Button cropButton =(Button) findViewById(R.id.button3);


        //申请权限
        FileManerger fileManerger = new FileManerger(MyActivity.this);
        fileManerger.requestFilePermission();


        jumpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    int checkCallPhonePermission = ContextCompat.checkSelfPermission(MyActivity.this, Manifest.permission.CAMERA);
                    if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MyActivity.this,new String[]{Manifest.permission.CAMERA},1);
                        return;
                    }
                    else{
                        dispatchTakePictureIntent();
                    }
                } else {
                    dispatchTakePictureIntent();
                }
            }
        });

        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    int checkCallPhonePermission = ContextCompat.checkSelfPermission(MyActivity.this, Manifest.permission.CAMERA);
                    if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MyActivity.this,new String[]{Manifest.permission.CAMERA},1);
                        return;
                    } else{
                        cropPhotoAndZoom();
                    }
                } else {
                    cropPhotoAndZoom();
                }
                Intent intent = new Intent();
                intent.setClass(MyActivity.this,ClassifierActivity.class);
                startActivity(intent);
            }
        });


    }


    private static final String SEP1 = "#";
    private static final String SEP2 = "|";
    private static final String SEP3 = "=";
    public static String ListToString(List<?> list) {
        StringBuffer sb = new StringBuffer();
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null || list.get(i) == "") {
                    continue;
                }
                // 如果值是list类型则调用自己
                if (list.get(i) instanceof List) {
                    sb.append(ListToString((List<?>) list.get(i)));
                    sb.append(SEP1);
                } else if (list.get(i) instanceof Map) {
                    sb.append(MapToString((Map<?, ?>) list.get(i)));
                    sb.append(SEP1);
                } else {
                    sb.append(list.get(i));
                    sb.append(SEP1);
                }
            }
        }
        return "L" + sb.toString();
    }
    public static String MapToString(Map<?, ?> map) {
        StringBuffer sb = new StringBuffer();
        // 遍历map
        for (Object obj : map.keySet()) {
            if (obj == null) {
                continue;
            }
            Object key = obj;
            Object value = map.get(key);
            if (value instanceof List<?>) {
                sb.append(key.toString() + SEP1 + ListToString((List<?>) value));
                sb.append(SEP2);
            } else if (value instanceof Map<?, ?>) {
                sb.append(key.toString() + SEP1
                        + MapToString((Map<?, ?>) value));
                sb.append(SEP2);
            } else {
                sb.append(key.toString() + SEP3 + value.toString());
                sb.append(SEP2);
            }
        }
        return "M" + sb.toString();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                }
            case 2:
                if (resultCode == Activity.RESULT_OK) {
                    uri=data.getData();
                    Intent i=new Intent(MyActivity.this,ClassifierActivity.class);//MainActivity和Main2Ativity连接起来 允许他们之间传递数据
                    i.putExtra("dizhi",uri.toString());//用putExtra把内容传送到另一个Activity,名字是data，值是nihao
                    startActivity(i);//启动第二个activity并把i传递过去
                    List<Classifier.Recognition> results =(List<Classifier.Recognition> )getIntent().getSerializableExtra("results");
                    String str;
                    str = ListToString(results);
                    TextView lblTitle=(TextView)findViewById(R.id.resistor);
                    lblTitle.setText(str);
                }
        }
    }


}
