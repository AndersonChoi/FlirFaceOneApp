package rescuer.ski.movement.coala.com.skirescuerwithflirone;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.graphics.drawable.BitmapDrawable;


import com.google.android.gms.vision.face.*;


import com.flir.flironesdk.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;


public class MainActivity extends Activity implements Device.PowerUpdateDelegate, Device.Delegate, FrameProcessor.Delegate, Device.StreamDelegate {
    private RelativeLayout cover, share, back;
    private ImageView shareimage, backimage, emotion_state, toastimage;
    private static boolean isDeviceConnected = false;
    private static boolean isShareLayout = false;
    private static int count = 0;
    private static float possible = 0;

    private FrameProcessor frameProcessor;
    private volatile Bitmap thermalBitmap;
    private volatile Bitmap thermalBitmapForSave;
    private Device flirDevice;
    private RelativeLayout rlConnectLayout, rlDisconnectLayout;
    private ImageView iv_flir;
    private TextView levelTextView, GPSTextView;
    private volatile boolean imageCaptureRequested = false;
    private String lastSavedPath;
    static boolean isFaceDetect = false;
    private int flag = 0, bntflag = 0;
    private float emotion = 0;
    private BitmapDrawable Drawable;
    private Bitmap happy, bored, frighrened, resultBmp;
    private MediaPlayer happy_sound,bored_sound,frighrened_sound;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        happy_sound = new MediaPlayer();
        bored_sound = new MediaPlayer();
        frighrened_sound = new MediaPlayer();

        cover = (RelativeLayout) findViewById(R.id.cover);
        share = (RelativeLayout) findViewById(R.id.share);
        back = (RelativeLayout) findViewById(R.id.back);
        shareimage = (ImageView) findViewById(R.id.shareimage);
        backimage = (ImageView) findViewById(R.id.backimage);
        emotion_state = (ImageView) findViewById(R.id.emotion_state);
        toastimage = (ImageView) findViewById(R.id.toastimage);

        GPSTextView = (TextView) findViewById(R.id.gps_tv);
        levelTextView = (TextView) findViewById(R.id.batteryLevelTextView);
        frameProcessor = new FrameProcessor(this, this, EnumSet.of(RenderedImage.ImageType.BlendedMSXRGBA8888Image));
        rlConnectLayout = (RelativeLayout) findViewById(R.id.flir_connect_layout);
        rlDisconnectLayout = (RelativeLayout) findViewById(R.id.flir_disconnect_layout);
        rlConnectLayout.setVisibility(View.VISIBLE);
        rlDisconnectLayout.setVisibility(View.VISIBLE);
        iv_flir = (ImageView) findViewById(R.id.iv_flir);
        iv_flir.clearColorFilter();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        Drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.happy);
        happy = Bitmap.createScaledBitmap(Drawable.getBitmap(), 250, 250, false);
        Drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.bored);
        bored = Bitmap.createScaledBitmap(Drawable.getBitmap(), 250, 250, false);
        Drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.frighrened);
        frighrened = Bitmap.createScaledBitmap(Drawable.getBitmap(), 250, 250, false);
        happy_sound = MediaPlayer.create(MainActivity.this, R.raw.happy);
        bored_sound = MediaPlayer.create(MainActivity.this, R.raw.bored);
        frighrened_sound = MediaPlayer.create(MainActivity.this, R.raw.frighrened);

        backimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibe.vibrate(50);
                isShareLayout=false;
                Handler hd = new Handler();
                hd.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (bntflag == 1) {
                            emotion_state.setVisibility(View.GONE);
                            shareimage.setImageResource(R.drawable.share_s);
                            backimage.setImageResource(R.drawable.back_s);
                            cover.setVisibility(View.GONE);
                            toastimage.setImageResource(0);
                            bntflag = 0;
                        }
                    }
                }, 1000);

            }
        });
        shareimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bntflag == 1) {

                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibe.vibrate(50);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/jpeg");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.parse(Environment.getExternalStorageDirectory() + "/.FLIR_FACE/temp.jpg"));
                    startActivity(Intent.createChooser(share, "Share Image"));


                }
            }
        });
        findViewById(R.id.btn_shutter).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (flirDevice == null && lastSavedPath != null) {
                    File file = new File(lastSavedPath);
                    LoadedFrame frame = new LoadedFrame(file);
                    onFrameReceived(frame);
                } else {
                    imageCaptureRequested = true;

                }

            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        Device.startDiscovery(this, this);

        if (isDeviceConnected) {
            rlConnectLayout.setVisibility(View.VISIBLE);
            rlDisconnectLayout.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Device.stopDiscovery();

    }


    @Override
    public void onTuningStateChanged(Device.TuningState tuningState) {

    }

    @Override
    public void onAutomaticTuningChanged(boolean b) {

    }

    @Override
    public void onDeviceConnected(Device device) {


        flirDevice = device;
        isDeviceConnected = true;


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isDeviceConnected) {
                    rlConnectLayout.setVisibility(View.VISIBLE);
                    rlDisconnectLayout.setVisibility(View.INVISIBLE);
                }

            }
        });


        device.startFrameStream(new Device.StreamDelegate() {
            @Override
            public void onFrameReceived(Frame frame) {


                if (count == 0)
                    frameProcessor.processFrame(frame);
                if (count == 2)
                    frameProcessor.processFrame(frame);
                if (count == 4)
                    frameProcessor.processFrame(frame);
                if (count == 5) {
                    frameProcessor.setImageTypes(EnumSet.of(RenderedImage.ImageType.VisualJPEGImage));
                    frameProcessor.processFrame(frame);
                }
               /* if (count == 6) {
                    frameProcessor.setImageTypes(EnumSet.of(RenderedImage.ImageType.ThermalLinearFlux14BitImage));
                    frameProcessor.processFrame(frame);
                }*/
                if (count == 6) {
                    count = 0;
                    frameProcessor.setImageTypes(EnumSet.of(RenderedImage.ImageType.BlendedMSXRGBA8888Image));
                }

                count++;

            }
        });
    }

    @Override
    public void onFrameProcessed(final RenderedImage renderedImage) {


        isDeviceConnected = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rlConnectLayout.setVisibility(View.VISIBLE);
                rlDisconnectLayout.setVisibility(View.INVISIBLE);
            }
        });


        if (renderedImage.imageType() == RenderedImage.ImageType.VisualJPEGImage) {
            thermalBitmap = renderedImage.getBitmap();


            Bitmap bitmap565 = convert(thermalBitmap, Bitmap.Config.RGB_565);

            FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                    .setTrackingEnabled(false)
                    .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                    .build();

            com.google.android.gms.vision.Frame frame = new com.google.android.gms.vision.Frame.Builder().setBitmap(bitmap565).build();

            final SparseArray<Face> faces = detector.detect(frame);


            if (faces.size() == 1) {
                isFaceDetect = true;
                Face face = faces.valueAt(0);
                possible = face.getIsSmilingProbability();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GPSTextView.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                isFaceDetect = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GPSTextView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }

        if (renderedImage.imageType() == RenderedImage.ImageType.BlendedMSXRGBA8888Image) // 열화상 이미지 뿌려줌
        {
            thermalBitmap = renderedImage.getBitmap();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    iv_flir.setImageBitmap(thermalBitmap);
                }
            });

        }


        if (this.imageCaptureRequested && !isFaceDetect) {
            imageCaptureRequested = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,
                            "Face is not detected.", Toast.LENGTH_LONG).show();
                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibe.vibrate(200);
                }
            });
        }

        if (this.imageCaptureRequested && renderedImage.imageType() == RenderedImage.ImageType.BlendedMSXRGBA8888Image && isFaceDetect) {
            imageCaptureRequested = false;

            thermalBitmapForSave = renderedImage.getBitmap();
            isShareLayout=true;
            new Thread(new Runnable() {
                public void run() {
                    File storagePath = new File(Environment.getExternalStorageDirectory() + "/.FLIR_FACE/");
                    storagePath.mkdirs();
                    emotion = possible;
                    String fileName = "/temp.jpg";

                    try {
                        lastSavedPath = storagePath + fileName;
                        File checkFile = new File(lastSavedPath);

                        if (checkFile.exists())
                            checkFile.delete();

                        resultBmp = Bitmap.createBitmap(thermalBitmapForSave.getWidth() + 0,
                                thermalBitmapForSave.getHeight() + 0,
                                thermalBitmapForSave.getConfig());
                        Canvas canvas = new Canvas(resultBmp);
                        canvas.drawBitmap(thermalBitmapForSave, 0, 0, null);
                        if (emotion >= 0.7) {
                            canvas.drawBitmap(happy, 0, 400, null);
                        } else if (emotion >= 0.3) {
                            canvas.drawBitmap(bored, 0, 400, null);
                        } else if (emotion >= -1.0) {
                            canvas.drawBitmap(frighrened, 0, 400, null);
                        }

                        Paint paint = new Paint();
                        paint.setColor(Color.WHITE);
                        paint.setTextSize(24);
                        canvas.drawText("FACE ONE with FLIR ONE", 24, 24, paint);
                        SaveBitmapToFileCache(resultBmp, Environment.getExternalStorageDirectory() + "/.FLIR_FACE/", fileName);
                        // renderedImage.getFrame().save(new File(lastSavedPath), RenderedImage.Palette.Iron, RenderedImage.ImageType.BlendedMSXRGBA8888Image);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            iv_flir.animate().setDuration(70).scaleY(0).withEndAction((new Runnable() {
                                public void run() {
                                    iv_flir.animate().setDuration(70).scaleY(1);
                                    cover.setVisibility(View.VISIBLE);

                                    Toast toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);
                                    ImageView imageView = new ImageView(getApplicationContext());
                                    if (emotion >= 0.7) {
                                        happy_sound.start();
                                        imageView.setImageResource(R.drawable.happy);
                                        toast.setView(imageView);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.setMargin(0, 0);
                                        toast.show();
                                    } else if (emotion >= 0.3) {
                                        bored_sound.start();
                                        imageView.setImageResource(R.drawable.bored);
                                        toast.setView(imageView);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.setMargin(0, 0);
                                        toast.show();
                                    } else if (emotion >= -1.0) {
                                        frighrened_sound.start();
                                        imageView.setImageResource(R.drawable.frighrened);
                                        toast.setView(imageView);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.setMargin(0, 0);
                                        toast.show();
                                    }
                                    Handler hd2 = new Handler();
                                    hd2.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast toast = Toast.makeText(getApplicationContext(), "Share your photo!!!", Toast.LENGTH_SHORT);
                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                            toast.setMargin(0, 0);
                                            toast.show();
                                            Bitmap bmp = BitmapFactory.decodeFile(lastSavedPath);
                                            emotion_state.setImageBitmap(bmp);
                                            emotion_state.setVisibility(View.VISIBLE);
                                        }
                                    }, 4000);

                                    Handler hd1 = new Handler();
                                    hd1.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            shareimage.setImageResource(R.drawable.share);
                                            backimage.setImageResource(R.drawable.back);
                                            flag = 0;
                                            bntflag = 1;
                                        }
                                    }, 6000);

                                }
                            }));
                        }
                    });
                }
            }).start();


        }


    }

    public void SaveBitmapToFileCache(Bitmap bitmap, String strFilePath,
                                      String filename) {

        File file = new File(strFilePath);

        if (!file.exists()) {
            file.mkdirs();
        }

        File fileCacheItem = new File(strFilePath + filename);
        OutputStream out = null;

        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ColorFilter originalChargingIndicatorColor = null;

    @Override
    public void onBatteryChargingStateReceived(final Device.BatteryChargingState batteryChargingState) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView chargingIndicator = (ImageView) findViewById(R.id.batteryChargeIndicator);
                if (originalChargingIndicatorColor == null) {
                    originalChargingIndicatorColor = chargingIndicator.getColorFilter();
                }
                switch (batteryChargingState) {
                    case FAULT:
                    case FAULT_HEAT:
                        chargingIndicator.setColorFilter(Color.RED);
                        chargingIndicator.setVisibility(View.VISIBLE);
                        break;
                    case FAULT_BAD_CHARGER:
                        chargingIndicator.setColorFilter(Color.DKGRAY);
                        chargingIndicator.setVisibility(View.VISIBLE);
                    case MANAGED_CHARGING:
                        chargingIndicator.setColorFilter(originalChargingIndicatorColor);
                        chargingIndicator.setVisibility(View.VISIBLE);
                        break;
                    case NO_CHARGING:
                    default:
                        chargingIndicator.setVisibility(View.GONE);
                        break;
                }
            }
        });
    }

    @Override
    public void onBatteryPercentageReceived(final byte percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                levelTextView.setVisibility(View.VISIBLE);
                levelTextView.setText("BAT:" + String.valueOf((int) percentage) + "%");
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onFrameReceived(Frame frame) {

    }


    @Override
    public void onDeviceDisconnected(Device device) {

        isDeviceConnected = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                iv_flir.clearColorFilter();

                if (!isDeviceConnected) {
                    rlConnectLayout.setVisibility(View.INVISIBLE);
                    rlDisconnectLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }


    private Bitmap convert(Bitmap bitmap, Bitmap.Config config) {
        Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return convertedBitmap;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if(!isShareLayout)
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}