package android.example.challenge1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class cameraActivity extends Activity{

    private Camera mCamera;
    private cameraPreview mPreview;
    Bitmap bitmap;
    String pathToFile;
    ImageView ImageView;
    Button TakePic;
    FrameLayout preview;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCamera = safeCameraOpen();

        mPreview = new cameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.cameraArea);
        ImageView = findViewById(R.id.imageView);
        TakePic = findViewById(R.id.takePic);

        preview.addView(mPreview);

        TakePic.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                mCamera.takePicture(null, null, mPicture);

                setNewView();
            }
        });
    }

    public void setNewView(){
        preview.setVisibility(View.INVISIBLE);
        ImageView.setVisibility(View.VISIBLE);
        TakePic.setVisibility(View.INVISIBLE);
    }

    public Camera safeCameraOpen(){
        Camera opened = null;
        try{
            opened = Camera.open(currentCameraId);
        }
        catch (Exception e){
            Log.e(TAG,"Failed to open camera");
            e.printStackTrace();
        }
        return opened;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                pathToFile = pictureFile.getAbsolutePath();
                Log.d("        My App", "file path: " + pathToFile);

                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            bitmap = BitmapFactory.decodeFile(pathToFile);
            ImageView.setImageBitmap(bitmap);
        }
    };

    private File getOutputMediaFile(int type){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Path");

        if (Environment.getExternalStorageState() == null) {
            Log.d("MyCameraApp", "failed to create directory");
        }

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        File mediaFile = null;
        try {
            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = File.createTempFile(timeStamp, ".jpg", mediaStorageDir);
            } else {
                return null;
            }
        }
        catch(IOException e){
            Log.d("couldn't create file", "Excep: " + e.toString());
        }

        return mediaFile;
    }

    public int getCurrentCameraId() {
        return currentCameraId;
    }
}
