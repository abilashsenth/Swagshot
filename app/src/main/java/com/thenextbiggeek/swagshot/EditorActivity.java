package com.thenextbiggeek.swagshot;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class EditorActivity extends AppCompatActivity {
    Uri imageFileUri;
    String uriAddressString;
    ImageView imgPreview, selector, shuffleButton;
    SeekBar opacitySeekbar, brightnessSeekbar, contrastSeekbar;
    Context context;
    Bitmap finalBitmap;
    int editType = 0, visibilityFlag=0; //0 doubleexpose/shufflemode... 1 is neonEffects/editormode
    int random;
    Bitmap result;
    File file;
    ProgressBar mProgressBar;



    String[] resNameNeon = new String[8];
    String[] resNameDB = new String[35];
    int widthArray[], heightArray[], opacityValue=100, brightnessValue=50, contrastValue=50;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        selector = findViewById(R.id.selector_button);
        imgPreview = findViewById(R.id.imagePreview);
        shuffleButton = findViewById(R.id.shuffleButton);
        opacitySeekbar = findViewById(R.id.opacitySlider);
        brightnessSeekbar = findViewById(R.id.brightnessSlider);
        contrastSeekbar = findViewById(R.id.contrastSlider);
        mProgressBar = findViewById(R.id.progressBar);
        setupSeekbarListeners();
        context = getApplicationContext();
        Bundle extras = getIntent().getExtras();
        uriAddressString = extras.getString("URI", "uri not available");
        Log.e("TAG", uriAddressString + "is the uriAddressString");
        file = new File(uriAddressString);
        imageFileUri = Uri.fromFile(file);
        generateResNames();
        Bitmap mBitmap = null;
        try {
            mBitmap  = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageFileUri);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //TODO create a button so the bitmap can be rotated 90*
        Matrix mMatrix = new Matrix();
        mMatrix.postRotate(90);
        finalBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(),
                mBitmap.getHeight(), mMatrix, false);
        imgPreview.setImageBitmap(finalBitmap);

        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shuffle();
            }
        });

    }


    ImageEditor mImageEditor;
    private void setupSeekbarListeners() {
        opacitySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               opacityValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //refresh
                //double exposure
                int resId = context.getResources().getIdentifier(resNameDB[random], "drawable", context.getPackageName() );
                combineBitmap = BitmapFactory.decodeResource(getResources(),resId);//the filter
                copyBitmap = scaleDown(finalBitmap, combineBitmap.getDensity(), combineBitmap, true);//the image
                mImageEditor = new ImageEditor(combineBitmap, copyBitmap, 3, opacityValue , context );
                mImageEditor.execute();


            }
        });
        brightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightnessValue = progress;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int resId = context.getResources().getIdentifier(resNameDB[random], "drawable", context.getPackageName() );
                combineBitmap = BitmapFactory.decodeResource(getResources(),resId);//the filter
                copyBitmap = scaleDown(finalBitmap, combineBitmap.getDensity(), combineBitmap, true);//the image
                mImageEditor = new ImageEditor(copyBitmap, combineBitmap, 1, brightnessValue , context );
                mImageEditor.execute();
            }
        });
        contrastSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                contrastValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int resId = context.getResources().getIdentifier(resNameDB[random], "drawable", context.getPackageName() );
                combineBitmap = BitmapFactory.decodeResource(getResources(),resId);//the filter
                copyBitmap = scaleDown(finalBitmap, combineBitmap.getDensity(), combineBitmap, true);//the image
                mImageEditor = new ImageEditor(copyBitmap, combineBitmap, 2, contrastValue , context );
                mImageEditor.execute();
            }
        });

    }



    private void generateResNames() {
        for(int i =0;i<8;i++){
            resNameNeon[i]= "v"+(i+1);
        }
        for(int i =0;i<35;i++){
            resNameDB[i]= "t"+(i+1);
        }
    }

    Bitmap combineBitmap;
    Bitmap copyBitmap;
    public void shuffle(){
        //take finalBitmap, double expose with a predefined bitmap, overwrite to the uri,
        // and refresh it to the imageview
        if(editType ==0){
            //double exposure
            random=  getARandomInt(0,34);
            int resId = context.getResources().getIdentifier(resNameDB[random], "drawable", context.getPackageName() );
            combineBitmap = BitmapFactory.decodeResource(getResources(),resId);
            copyBitmap = scaleDown(finalBitmap, combineBitmap.getDensity(), combineBitmap, true);
            BlendAsync mBlendAsync = new BlendAsync(copyBitmap, combineBitmap, context);
            mBlendAsync.execute();

        }else{
            //neon
            random=  getARandomInt(0,7);
            int resId = context.getResources().getIdentifier(resNameNeon[random], "drawable", context.getPackageName() );
            combineBitmap = BitmapFactory.decodeResource(getResources(),resId);
            copyBitmap = scaleDown(finalBitmap, combineBitmap.getDensity(), combineBitmap, true);
            BlendAsync mBlendAsync = new BlendAsync(copyBitmap, combineBitmap, context);
            mBlendAsync.execute();
        }



    }

    ImageEditClass mImageEditClass;

    public void assign(int index){
        if(editType ==0){
            mImageEditClass = new ImageEditClass();



            //blend in another thread
            BlendAsync mBlendAsync = new BlendAsync(copyBitmap, combineBitmap, context);
            mBlendAsync.execute();





        }else{
            //neon
            int resId = context.getResources().getIdentifier(resNameNeon[index], "drawable", context.getPackageName() );
            combineBitmap = BitmapFactory.decodeResource(getResources(),resId);
            copyBitmap = scaleDown(finalBitmap, combineBitmap.getDensity(), combineBitmap, true);


            //blend in another thread
            BlendAsync mBlendAsync = new BlendAsync(copyBitmap, combineBitmap, context);
            mBlendAsync.execute();


        }
    }

    private int getARandomInt(int i, int i1) {
        int random = ThreadLocalRandom.current().nextInt(i, i1+1);
        return  random;
    }


    //bitmap dividing and screening, separate methods, usage differs. MEMORY INTENSIVE
    public Bitmap bitmapScreenBlend(Bitmap baseParam, Bitmap blendParam){

        return null;

    }

    public Bitmap bitmapDivideBlend(Bitmap baseParam, Bitmap blendParam){

        Resources res = getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap base = baseParam;
        Bitmap blend = blendParam;

        // now base and blend are in ARGB8888 mode, which is what you want

        Bitmap result = base.copy(Bitmap.Config.ARGB_8888, true);

        // Same image creation/reading as above, then:
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        p.setShader(new BitmapShader(blend, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        Canvas c = new Canvas();
        c.setBitmap(result);
        c.drawBitmap(base, 0, 0, null);
        c.drawRect(0, 0, base.getWidth(), base.getHeight(), p);

        baseParam.recycle();
        blendParam.recycle();

        return result;

    }


    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, Bitmap base,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        return Bitmap.createScaledBitmap(realImage, base.getWidth(),
                base.getHeight(), filter);
    }


    public void getBackToMainActivity(View v){
        Intent intent = new Intent(EditorActivity.this, MainActivity.class);
        startActivity(intent);
    }


    public void selectorButton(View view) {
        if(editType ==0) {
            editType =1;
            selector.setImageResource(R.drawable.neon_selector);
            shuffle();

        }else{
            editType = 0;
            selector.setImageResource(R.drawable.expose_selector);
            shuffle();
        }

    }


    public void editVisibility(View view) {
        if(visibilityFlag == 0){
            LinearLayout mLinearLayout = findViewById(R.id.scrollerLayout);
            mLinearLayout.setVisibility(View.VISIBLE);
            shuffleButton.setVisibility(View.GONE);
            visibilityFlag = 1;
            //change the icon to back
        }else if(visibilityFlag ==1){
            LinearLayout mLinearLayout = findViewById(R.id.scrollerLayout);
            mLinearLayout.setVisibility(View.GONE);
            shuffleButton.setVisibility(View.VISIBLE);
            visibilityFlag = 0;

        }


    }

    public void saveBitmap(View view) {
        if(result!=null){
            try (FileOutputStream out = new FileOutputStream(file)) {
                result.compress(Bitmap.CompressFormat.JPEG, 70, out);
               //gallery scan yet to do
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public class BlendAsync extends AsyncTask<Bitmap, Integer, Bitmap> {

        Bitmap baseParam, blendParam, product;
        Context context;
        public BlendAsync(Bitmap b1, Bitmap b2, Context c){
            baseParam = b1;
            blendParam = b2;
            context = c;
        }
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            Resources res = context.getResources();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap base = baseParam;
            Bitmap blend = blendParam;

            // now base and blend are in ARGB8888 mode, which is what you want

            Bitmap result = base.copy(Bitmap.Config.ARGB_8888, true);

            // Same image creation/reading as above, then:
            Paint p = new Paint();
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
            p.setShader(new BitmapShader(blend, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

            Canvas c = new Canvas();
            c.setBitmap(result);
            c.drawBitmap(base, 0, 0, null);
            c.drawRect(0, 0, base.getWidth(), base.getHeight(), p);

            baseParam.recycle();
            blendParam.recycle();

            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... integers){
            mProgressBar.setVisibility(View.VISIBLE);
        }
        @Override
        protected void onPostExecute(Bitmap finalBitmap){
            //can use the processed final bitmap to be returned
            mProgressBar.setVisibility(View.INVISIBLE);
            product = finalBitmap;
            imgPreview.setImageBitmap(product);
            imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

        }
    }

    public class ImageEditor extends AsyncTask<Bitmap, Integer, Bitmap> {

        Bitmap baseParam, blendParam, product;
        Context context;
        ImageEditClass imageEditClass;
        int type, quantity;
        public ImageEditor(Bitmap b1, Bitmap b2, int typeFlag, int q, Context c){
            baseParam = b1;
            context = c;
            type = typeFlag;
            quantity = q;
            blendParam = b2;
            imageEditClass = new ImageEditClass();
        }
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {

            switch (type){
                case 1:
                    //brightness change
                    product= imageEditClass.adjustBrightness(baseParam,quantity );
                    break;
                case 2:
                    //contrast change
                    product= imageEditClass.adjustContrast(baseParam,quantity );
                break;

                case 3:
                    //opacity change
                    product= imageEditClass.adjustOpacity(baseParam,quantity );
                break;
            }
        return  product;
        }

        @Override
        protected void onProgressUpdate(Integer... integers){
            mProgressBar.setVisibility(View.VISIBLE);
        }
        @Override
        protected void onPostExecute(Bitmap finalBitmap){
            //can use the processed final bitmap to be returned
            mProgressBar.setVisibility(View.INVISIBLE);
            if(type == 1 || type ==2){
                BlendAsync mBlendAsync = new BlendAsync(finalBitmap, blendParam, context );
                mBlendAsync.execute();
            }else{
                BlendAsync mBlendAsync = new BlendAsync(blendParam, finalBitmap, context );
                mBlendAsync.execute();

            }



        }
    }

}
