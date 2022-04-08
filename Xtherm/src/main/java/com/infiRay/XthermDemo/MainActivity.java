/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device

 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.infiRay.XthermDemo;

import android.animation.Animator;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.serenegiant.MyApp;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.utils.PermissionCheck;
import com.serenegiant.utils.ViewAnimationHelper;
import com.serenegiant.widget.AutoFitTextureView;
import com.serenegiant.widget.Camera2Helper;
import com.serenegiant.widget.TouchPoint;
import com.serenegiant.widget.UVCCameraTextureView;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.filter.GifSizeFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static java.lang.Math.abs;
import static java.lang.Thread.sleep;

public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    public static final String SYS_EMUI = "sys_emui";
    public static final String SYS_MIUI = "sys_miui";
    public static final String SYS_FLYME = "sys_flyme";
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";
    private static final String KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level";
    private static final String KEY_EMUI_VERSION = "ro.build.version.emui";
    private static final String KEY_EMUI_CONFIG_HW_SYS_VERSION = "ro.confg.hw_systemversion";
    private static final boolean DEBUG = false;    // TODO set false on release
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_CHOOSE = 23;

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     * by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 384;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 292;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 0;

    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private UVCCameraTextureView mUVCCameraView;

    /**
     * for open&start / stop&close camera preview
     */
    //private ToggleButton mCameraButton;
    /**
     * button for start/stop recording
     */
    private boolean settingsIsShow = false;
    private RadioGroup paletteRadioGroup, temperatureUnitsRadioGroup, languageRadioGroup;
    private TextView SN, PN, sotfVersion, productSoftVersion;
    private LinearLayout rightmenu;
    private ImageButton mCaptureButton, mPhotographButton, mPalette, mSetButton;
    private ImageButton pointModeButton, lineModeButton, rectangleModeButton, MakeReportButton, ChangeRangeButton;
    private Switch mSysCameraSwitch, mWatermarkSwitch;
    private ImageView mImageView;
    private ImageButton mThumbnailButton;
    private boolean statusIsOff = true;
    private SurfaceView mSfv, mRightSfv;
    private String brand, model, hardware;
    private SurfaceHolder mSfh, mRightSfh;
    private TextView emissivityText, correctionText, reflectionText, ambtempText, humidityText, distanceText, textMax, textMin;
    private float Fix = 0, Refltmp = 0, Airtmp = 0, humi = 0, emiss = 0;
    private short distance = 0;
    private String stFix, stRefltmp, stAirtmp, stHumi, stEmiss, stDistance, stProductSoftVersion;
    private Button saveButton;
    private View mBrightnessButton, mContrastButton;
    private LinearLayout mMenuRight;
    private ImageView mTempbutton, mZoomButton, mClearButton;
    private View mResetButton;
    private View mToolsLayout, mValueLayout;
    private SeekBar mSettingSeekbar, emissivitySeekbar, correctionSeekbar, reflectionSeekbar, ambtempSeekbar, humiditySeekbar, distanceSeekbar;
    private SeekBar highThrowSeekbar, lowThrowSeekbar, lowPlatSeekbar, highPlatSeekbar, orgSubGsHighSeekbar, orgSubGsLowSeekbar, sigmaDSeekbar, sigmaRSeekbar;
    private TextView highThrowText, lowThrowText, lowPlatText, highPlatText, orgSubGsHighText, orgSubGsLowText, sigmaDText, sigmaRText;
    private int highThrow, lowThrow, lowPlat, highPlat, OrgSubGsHigh, OrgSubGsLow, sigmaD, sigmaR;
    private int mLeft, mRight, mTop, mBottom;
    private int mRightSurfaceLeft, mRightSurfaceRight, mRightSurfaceTop, mRightSurfaceBottom;
    private int indexOfPoint = 0;
    private int intTestx;
    private CopyOnWriteArrayList<TouchPoint> mTouchPoint;
    private int temperatureAnalysisMode;
    private boolean isTemperaturing, isSettingBadPixel, needClearCanvas = false;
    private Bitmap mCursorBlue, mCursorRed, mCursorYellow, mCursorGreen, mWatermarkLogo;
    private Bitmap icon, iconPalette; //建立一个空的图画板
    private Canvas canvas, bitcanvas, paletteCanvas, paletteBitmapCanvas;//初始化画布绘制的图像到icon上
    private Paint photoPaint, palettePaint; //建立画笔
    private Rect dstHighTemp, dstLowTemp;//创建一个指定的新矩形的坐标
    private int x1;
    private int y1;
    int posx, posy;
    private EditText iput0, iput1, iput2, iput3, iput4, iput5;
    private Button butSure0, butSure1, butSure2, butSure3, butSure4, butSure5, butSetSave;
    private ByteUtil mByteUtil = new ByteUtil();
    private PopupWindow popupWindow, temperatureAnalysisWindow, settingsWindows;
    private int from = 0;
    private LinearLayout mContentLayout;
    volatile boolean isOnRecord;
    private byte[] picTakeByteArray = new byte[640 * 512 * 4];


    public int currentapiVersion = 0;//现改用为平台类型
    private Fragment currentFragment = null;
    private SettingFragment settingFragment;
    private ScaleGestureDetector mScaleGestureDetector = null;
    private Context context;
    private InputStream assetsInputStream;
    private boolean isOnOff;
    //	private BitmapDrawable mCursor;l
    private float mFinalScale;
    private SharedPreferences sharedPreferences;
    private int UnitTemperature = 0, palette;
    private int TemperatureRange = 120;
    private boolean IsAlreadyOnCreate = false;
    private sendCommand mSendCommand;
    private AlertDialog ConnectOurDeviceAlert;
    private Timer timerEveryTime;
    private Camera2Helper mCamera2Helper;
    private DisplayMetrics metrics;
    private Configuration configuration;
    private int language, isWatermark;
    private boolean XthermAlreadyConnected = false;
    //    private boolean camerapreview = false;
    private boolean isPreviewing = false;
    private SensorManager mSensorManager;
    private Sensor mSensorMagnetic, mAccelerometer;
    int oldRotation = 0;
    UsbDevice mUsbDevice;
    private AutoFitTextureView mTextureView;
    private boolean isFirstRun, isAgreement;
    LinearLayout rl_tip, rl_tip_kaka, rl_tip_setting, menu_palette_layout, rl_tip_setting1;
    RelativeLayout ll_tip_temp, ll_tip_temp1;
    Boolean isT3 = false;
    String locale_language;
    private int isOpened = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.e(TAG, "onCreating:");
//        Bugly.init(getApplicationContext(), "cd64123500", false);
        if (!IsAlreadyOnCreate) {
            sharedPreferences = getSharedPreferences("setting_share", 0);
            configuration = getResources().getConfiguration();
            metrics = getResources().getDisplayMetrics();

            locale_language = Locale.getDefault().getLanguage();
            language = sharedPreferences.getInt("Language", -1);
//            Log.e(TAG, "Language:" + language);
            switch (language) {
                case -1:
                    if (locale_language == "zh") {
                        sharedPreferences.edit().putInt("Language", 0).commit();
                    } else if (locale_language == "en") {
                        sharedPreferences.edit().putInt("Language", 1).commit();
                    } else if (locale_language == "ru") {
                        sharedPreferences.edit().putInt("Language", 2).commit();
                    }
                    break;
                case 0:
                    sharedPreferences.edit().putInt("Language", 0).commit();
                    configuration.locale = Locale.SIMPLIFIED_CHINESE;
                    configuration.setLayoutDirection(Locale.SIMPLIFIED_CHINESE);
                    getResources().updateConfiguration(configuration, metrics);
                    break;
                case 1:
                    sharedPreferences.edit().putInt("Language", 1).commit();
                    configuration.locale = Locale.ENGLISH;
                    configuration.setLayoutDirection(Locale.ENGLISH);
                    getResources().updateConfiguration(configuration, metrics);
                    break;
                case 2:
                    sharedPreferences.edit().putInt("Language", 2).commit();
                    configuration.locale = new Locale("ru", "RU");
                    getResources().updateConfiguration(configuration, metrics);
                    Log.e(TAG, "Language2:" + language);
                    break;
            }
            IsAlreadyOnCreate = true;
            super.onCreate(savedInstanceState);
            Log.e(TAG, "onCreate:");
            this.context = this;
            Log.e(TAG, "onCreate:" + this);
            System.setProperty("javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
            System.setProperty("javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
            System.setProperty("javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            if (DEBUG) Log.v(TAG, "onCreate:");
            setContentView(R.layout.activity_main);

            mTextureView = (AutoFitTextureView) findViewById(R.id.textureView);
            rightmenu = (LinearLayout) findViewById(R.id.rightmenu_list);
            rightmenu.setVisibility(INVISIBLE);
            mCaptureButton = (ImageButton) findViewById(R.id.button_video);
            mCaptureButton.setOnClickListener(mOnClickListener);
            mCaptureButton.setVisibility(VISIBLE);
            mSetButton = (ImageButton) findViewById(R.id.button_set);
            mSetButton.setOnClickListener(mOnClickListener);
            mSetButton.setVisibility(VISIBLE);
            mThumbnailButton = (ImageButton) findViewById(R.id.imageview_thumbnail);
            mThumbnailButton.setOnClickListener(mOnClickListener);
            mThumbnailButton.setVisibility(VISIBLE);
            saveButton = (Button) findViewById(R.id.save_button);
            saveButton.setOnClickListener(mOnClickListener);
            paletteRadioGroup = (RadioGroup) findViewById(R.id.palette_radio_group);
            paletteRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
            languageRadioGroup = (RadioGroup) findViewById(R.id.language_radio_group);
            languageRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
            temperatureUnitsRadioGroup = (RadioGroup) findViewById(R.id.temperature_units_radio_group);
            temperatureUnitsRadioGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
            emissivitySeekbar = (SeekBar) findViewById(R.id.emissivity_seekbar);
            emissivitySeekbar.setOnSeekBarChangeListener(mOnEmissivitySeekBarChangeListener);
            emissivityText = (TextView) findViewById(R.id.emissivity_text);
            correctionSeekbar = (SeekBar) findViewById(R.id.correction_seekbar);
            correctionSeekbar.setOnSeekBarChangeListener(mOnEmissivitySeekBarChangeListener);
            correctionText = (TextView) findViewById(R.id.correction_text);
            reflectionSeekbar = (SeekBar) findViewById(R.id.reflection_seekbar);
            reflectionSeekbar.setOnSeekBarChangeListener(mOnEmissivitySeekBarChangeListener);
            reflectionText = (TextView) findViewById(R.id.reflection_text);
            ambtempSeekbar = (SeekBar) findViewById(R.id.amb_temp_seekbar);
            ambtempSeekbar.setOnSeekBarChangeListener(mOnEmissivitySeekBarChangeListener);
            ambtempText = (TextView) findViewById(R.id.amb_temp_text);
            humiditySeekbar = (SeekBar) findViewById(R.id.humidity_seekbar);
            humiditySeekbar.setOnSeekBarChangeListener(mOnEmissivitySeekBarChangeListener);
            mSysCameraSwitch = (Switch) findViewById(R.id.sys_camera_swtich);
            mSysCameraSwitch.setOnCheckedChangeListener(mSwitchListener);
            mWatermarkSwitch = (Switch) findViewById(R.id.watermark_swtich);
            mWatermarkSwitch.setOnCheckedChangeListener(mSwitchListener);
            humidityText = (TextView) findViewById(R.id.humidity_text);
            distanceSeekbar = (SeekBar) findViewById(R.id.distance_seekbar);
            distanceSeekbar.setOnSeekBarChangeListener(mOnEmissivitySeekBarChangeListener);
            distanceText = (TextView) findViewById(R.id.distance_text);


            SN = (TextView) findViewById(R.id.product_SN);
            PN = (TextView) findViewById(R.id.product_name);
            sotfVersion = (TextView) findViewById(R.id.soft_version);
            productSoftVersion = (TextView) findViewById(R.id.product_soft_version);

            rl_tip = (LinearLayout) findViewById(R.id.rl_tip);
            rl_tip_kaka = (LinearLayout) findViewById(R.id.rl_tip_kaka);
            rl_tip_setting = (LinearLayout) findViewById(R.id.rl_tip_setting);
            rl_tip_setting1 = (LinearLayout) findViewById(R.id.rl_tip_setting1);
            menu_palette_layout = (LinearLayout) findViewById(R.id.menu_palette_layout);
            ll_tip_temp = (RelativeLayout) findViewById(R.id.ll_tip_temp);
            ll_tip_temp1 = (RelativeLayout) findViewById(R.id.ll_tip_temp1);

            rl_tip.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rl_tip.setVisibility(View.GONE);
//                    if (isT3) {
//                        rl_tip_setting1.setVisibility(View.VISIBLE);
//                    } else {
                    ll_tip_temp.setVisibility(View.VISIBLE);
//                    }
                }
            });
            ll_tip_temp.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ll_tip_temp.setVisibility(View.GONE);
                    rl_tip_kaka.setVisibility(View.VISIBLE);
                }
            });
            rl_tip_kaka.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rl_tip_kaka.setVisibility(View.GONE);
//                    if (isT3) {
//                        ll_tip_temp1.setVisibility(View.VISIBLE);
//                    } else {
                    rl_tip_setting.setVisibility(View.VISIBLE);
//                    }

                }
            });
            rl_tip_setting.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    sharedPreferences.edit().putBoolean("isFirstRun", false).commit();
                    rl_tip_setting.setVisibility(View.GONE);
                }
            });

            rl_tip_setting1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rl_tip_setting1.setVisibility(View.GONE);
                    rl_tip_kaka.setVisibility(View.VISIBLE);
                }
            });

            ll_tip_temp1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    sharedPreferences.edit().putBoolean("isFirstRun", false).commit();
                    ll_tip_temp1.setVisibility(View.GONE);
                }
            });
            mPhotographButton = (ImageButton) findViewById(R.id.button_camera);
            mPhotographButton.setOnTouchListener(mChangPicListener);
            mPhotographButton.setOnClickListener(mOnClickListener);
            mPhotographButton.setVisibility(VISIBLE);
            mImageView = (ImageView) findViewById(R.id.frame_image);
            final UVCCameraTextureView view = (UVCCameraTextureView) findViewById(R.id.camera_view);
            view.setOnLongClickListener(mOnLongClickListener);
            mUVCCameraView = view;
            mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float) PREVIEW_HEIGHT);
            mZoomButton = (ImageView) findViewById(R.id.button_shut);
            mZoomButton.setOnTouchListener(mChangPicListener);
            mZoomButton.setOnClickListener(mOnClickListener);
            mZoomButton.setVisibility(VISIBLE);
            mMenuRight = (LinearLayout) findViewById(R.id.menu_layout);
            mTempbutton = (ImageView) findViewById(R.id.button_temp);
            mTempbutton.setOnClickListener(mOnClickListener);
            mTempbutton.setVisibility(VISIBLE);
            mSfv = (SurfaceView) this.findViewById(R.id.surface_view);
            mSfh = mSfv.getHolder();
            mSfv.setZOrderOnTop(true);
            mSfh.setFormat(PixelFormat.TRANSLUCENT);
            mSfv.setOnTouchListener(listener);
            mTouchPoint = new CopyOnWriteArrayList<TouchPoint>();
            mRightSfv = (SurfaceView) this.findViewById(R.id.surfaceView_right);
            mRightSfh = mRightSfv.getHolder();
            mRightSfv.setZOrderOnTop(true);
            mRightSfh.setFormat(PixelFormat.TRANSLUCENT);
            isTemperaturing = false;
            isOnRecord = false;
            isSettingBadPixel = false;
            mCursorYellow = BitmapFactory.decodeResource(getResources(), R.mipmap.cursoryellow);
            mCursorRed = BitmapFactory.decodeResource(getResources(), R.mipmap.cursorred);
            mCursorBlue = BitmapFactory.decodeResource(getResources(), R.mipmap.cursorblue);
            mCursorGreen = BitmapFactory.decodeResource(getResources(), R.mipmap.cursorgreen);
            mWatermarkLogo = BitmapFactory.decodeResource(getResources(), R.mipmap.xtherm);
            mFinalScale = 1;
            matchBrand();
            mSendCommand = new sendCommand();
            XXPermissions.with(MainActivity.this)
                    .permission(Permission.RECORD_AUDIO)
//                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
//                    .permission(Permission.CAMERA)
                    //.constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
//                    .permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
                    .permission(Permission.RECORD_AUDIO, Permission.WRITE_EXTERNAL_STORAGE, Permission.CAMERA) //不指定权限则自动获取清单中的危险权限
                    .request(new OnPermission() {

                        @Override
                        public void hasPermission(List<String> granted, boolean isAll) {
                            mCameraHandler = UVCCameraHandler.createHandler(MainActivity.this, mUVCCameraView,
                                    USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE, null, currentapiVersion);
                            mUSBMonitor = new USBMonitor(MainActivity.this, mOnDeviceConnectListener);
                        }

                        @Override
                        public void noPermission(List<String> denied, boolean quick) {

                        }
                    });
            //currentapiVersion=android.os.Build.VERSION.SDK_INT;

            //	mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
            //			0, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE,ahITemperatureCallback,currentapiVersion);
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            //获取Sensor
            mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public static boolean isZh(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

    private String getFileName(File[] files) {
        String str = "";
        if (files != null) { // 先判断目录是否为空，否则会报空指针
            for (File file : files) {
                if (file.isDirectory()) {//检查此路径名的文件是否是一个目录(文件夹)
                    Log.e("zeng", "若是文件目录。继续读1"
                            + file.getName().toString() + file.getPath().toString());
                    getFileName(file.listFiles());
                    Log.e("zeng", "若是文件目录。继续读2"
                            + file.getName().toString() + file.getPath().toString());
                } else {
                    String fileName = file.getName();
                    if (fileName.endsWith(".txt")) {
                        String s = fileName.substring(0, fileName.lastIndexOf(".")).toString();
                        Log.e("zeng", "文件名txt：：   " + s);
                        str += fileName.substring(0, fileName.lastIndexOf(".")) + "\n";
                    }
                }
            }

        }
        return str;
    }

    private final View.OnTouchListener mChangPicListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (v.getId()) {
                case R.id.button_camera:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) //按下重新设置背景图片
                    {
                        ((ImageButton) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.mipmap.camera2, null));
                    } else if (event.getAction() == MotionEvent.ACTION_UP) //松手恢复原来图片
                    {
                        ((ImageButton) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.mipmap.camera1, null));
                    }
                    return false;
//				case R.id.button_video:
//					if (event.getAction() == MotionEvent.ACTION_DOWN) //按下重新设置背景图片
//					{
//						((ImageButton) v).setImageDrawable(getResources().getDrawable(R.mipmap.video2));
//					} else if (event.getAction() == MotionEvent.ACTION_UP) //松手恢复原来图片
//					{
//						((ImageButton) v).setImageDrawable(getResources().getDrawable(R.mipmap.video1));
//					}
//					return false;
                case R.id.button_temp:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) //按下重新设置背景图片
                    {
                        ((ImageButton) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.mipmap.temp2, null));
                    } else if (event.getAction() == MotionEvent.ACTION_UP) //松手恢复原来图片
                    {
                        ((ImageButton) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.mipmap.temp1, null));
                    }
                    return false;
                case R.id.button_shut:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) //按下重新设置背景图片
                    {
                        ((ImageButton) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.mipmap.shut2, null));
                    } else if (event.getAction() == MotionEvent.ACTION_UP) //松手恢复原来图片
                    {
                        ((ImageButton) v).setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.mipmap.shut1, null));
                    }
                    return false;
            }
            return false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart:");
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            onStop();
            finish();
            //不执行父类点击事件
            return true;
        }
        //继续执行父类其他点击事件
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop:");
        if (mCamera2Helper != null) {
            if (mCamera2Helper.getState()) {
                mCameraHandler.closeSystemCamera();
//                mSysCameraSwitch.setChecked(false);
            }
        }
        if (mUSBMonitor != null) {
            if (mUSBMonitor.isRegistered()) {
                mUSBMonitor.unregister();
            }
        }
        if (ConnectOurDeviceAlert != null) {
            ConnectOurDeviceAlert.dismiss();
        }
        mSensorManager.unregisterListener(mSensorListener, mSensorMagnetic);
        mSensorManager.unregisterListener(mSensorListener, mAccelerometer);
        //System.exit(0);
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
        needClearCanvas = true;
        isTemperaturing = false;
        //whenCloseClearCanvas();
        if (isOnRecord) {
            isOnRecord = false;
            mCaptureButton.setImageDrawable(getResources().getDrawable(R.mipmap.video1));
            mCameraHandler.stopRecording();
        }
        //setCameraButton(false);
        if (mCameraHandler != null) {
            mCameraHandler.close();
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (mCamera2Helper != null) {
            if (mCamera2Helper.getState()) {
                mCameraHandler.closeSystemCamera();
                mSysCameraSwitch.setChecked(false);
            }
        }
        if (isTemperaturing) {
            mCameraHandler.stopTemperaturing();
            isTemperaturing = false;
            pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point));
            lineModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.line));
            rectangleModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.rectangle));
        }
        Log.e(TAG, "onPause:");
        super.onPause();
    }

    //Activity从后台重新回到前台时被调用
    @Override
    protected void onRestart() {
        Log.e(TAG, "onRestart:");
        if (mUVCCameraView != null)
            mThumbnailButton.setVisibility(VISIBLE);
        currentSecond = 0;
        mUVCCameraView.onResume();
        isOpened = 0;
        super.onRestart();
    }

    static AlertDialog dialog;

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume:");
        if (mUSBMonitor != null) {
            if (!mUSBMonitor.isRegistered()) {
                mUSBMonitor.register();
            }
//            showAgreeMent();

            mTouchPoint.clear();//点测温清屏
            mSensorManager.registerListener(mSensorListener, mSensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            List<UsbDevice> mUsbDeviceList = mUSBMonitor.getDeviceList();
            for (UsbDevice udv : mUsbDeviceList) {
//            System.out.println(udv.toString());
                if (udv.getProductName() != null) {
                    if (udv.getProductName().contains("Xtherm") || udv.getProductName().contains("FX3") || udv.getProductName().contains("S0") || udv.getProductName().contains("T3") || udv.getProductName().contains("DL") || udv.getProductName().contains("DV") || udv.getProductName().contains("T2") || udv.getProductName().contains("DP")) {
                        XthermAlreadyConnected = true;
                        MyApp.deviceName = udv.getProductName();
                        isT3 = MyApp.deviceName.contains("DL") || MyApp.deviceName.contains("DV") || MyApp.deviceName.contains("DP");
                        isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);
                        if (isFirstRun) {
                            if (isZh(this)) {
                                rl_tip.setVisibility(View.VISIBLE);
                            }
                        } else {
                            rl_tip.setVisibility(View.GONE);
                        }
                    }
                }
            }
            if (!XthermAlreadyConnected) {
                ConnectOurDeviceAlert = new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getResources().getString(R.string.Tip_to_connect))
                        .setPositiveButton(getResources().getString(R.string.Tip_to_connect_wait), null)
                        .setNegativeButton(getResources().getString(R.string.Tip_to_connect_cancel), null).create();
                ConnectOurDeviceAlert.setCancelable(false);   //设置点击空白区域不消失
                ConnectOurDeviceAlert.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        //确定按键
                        Button positiveButton = ConnectOurDeviceAlert.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                List<UsbDevice> mUsbDeviceList2 = mUSBMonitor.getDeviceList();
                                for (UsbDevice udv : mUsbDeviceList2) {
                                    Log.e(TAG, "onClick AlertDialog.BUTTON_POSITIVE: " + udv.getProductName());
                                    if (udv.getProductName().contains("FX3") || udv.getProductName().contains("Xtherm") || udv.getProductName().contains("S0") || udv.getProductName().contains("T3") || udv.getProductName().contains("DL") || udv.getProductName().contains("DV") || udv.getProductName().contains("T2") || udv.getProductName().contains("DP")) {
                                        if (mUSBMonitor.hasPermission(udv)) {
                                            XthermAlreadyConnected = true;
                                            Log.e(TAG, "onClick hasPermission ");
                                            ConnectOurDeviceAlert.dismiss();
                                        } else {
                                            mUSBMonitor.requestPermission(udv);
                                        }
                                    }
                                }
                            }
                        });

                        //取消按键
                        Button negativeButton = ConnectOurDeviceAlert.getButton(AlertDialog.BUTTON_NEGATIVE);
                        negativeButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                System.exit(0);
                            }
                        });
                    }
                });

                ConnectOurDeviceAlert.show();
            }
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
//        sharedPreferences.edit().putBoolean("cameraPreview", false).commit();
        Log.e(TAG, "onDestroy:");
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraView = null;
        //mCameraButton = null;
        mCaptureButton = null;
        mSetButton = null;
        //textMax=null;
        //textMin=null;
        mPhotographButton = null;
//        mSysCameraSwitch.setChecked(false);
//        sharedPreferences.edit().putBoolean("cameraPreview", false).commit();
        super.onDestroy();
    }

    private void getTempPara() {
        byte[] tempPara;
        tempPara = mCameraHandler.getTemperaturePara(128);
        Log.e(TAG, "getByteArrayTemperaturePara:" + tempPara[16] + "," + tempPara[17] + "," + tempPara[18] + "," + tempPara[19] + "," + tempPara[20] + "," + tempPara[21]);

        Fix = mByteUtil.getFloat(tempPara, 0);
        Refltmp = mByteUtil.getFloat(tempPara, 4);
        Airtmp = mByteUtil.getFloat(tempPara, 8);
        humi = mByteUtil.getFloat(tempPara, 12);
        emiss = mByteUtil.getFloat(tempPara, 16);
        distance = mByteUtil.getShort(tempPara, 20);
        stFix = String.valueOf(Fix);
        stRefltmp = String.valueOf(Refltmp);
        stAirtmp = String.valueOf(Airtmp);
        stHumi = String.valueOf(humi);
        stEmiss = String.valueOf(emiss);
        stDistance = String.valueOf(distance);
        stProductSoftVersion = new String(tempPara, 128 - 16, 16);
    }

    SeekBar.OnSeekBarChangeListener mOnEmissivitySeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int id = seekBar.getId();
            switch (id) {
                case R.id.emissivity_seekbar:
                    float emiss = progress / 100.0f;
                    String emissString = String.valueOf(emiss);
                    emissivityText.setText(emissString);
                    break;
                case R.id.correction_seekbar:
                    float correction = (progress - 30) / 10.0f;
                    String correctionString = String.valueOf(correction);
                    correctionText.setText(correctionString + "°C");
                    break;
                case R.id.reflection_seekbar:
                    int reflection = (progress - 10);
                    String reflectionString = String.valueOf(reflection) + "°C";
                    reflectionText.setText(reflectionString);
                    break;
                case R.id.amb_temp_seekbar:
                    int ambtemp = (progress - 10);
                    String ambtempString = String.valueOf(ambtemp) + "°C";
                    ambtempText.setText(ambtempString);
                    break;
                case R.id.humidity_seekbar:
                    float humidity = progress / 100.0f;
                    String humidityString = String.valueOf(humidity);
                    humidityText.setText(humidityString);
                    break;
                case R.id.distance_seekbar:
                    int distance = progress;
                    Log.e(TAG, "distance_seekbar:" + distance);
                    String distanceString = String.valueOf(distance) + "m";
                    distanceText.setText(distanceString);
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        //private TextView emissivityText,correctionText,reflectionText,ambtempText,humidityText,distanceText,textMax, textMin;
        //private SeekBar mSettingSeekbar,emissivitySeekbar,correctionSeekbar,reflectionSeekbar,ambtempSeekbar,humiditySeekbar,distanceSeekbar;
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int id = seekBar.getId();
            switch (id) {
                case R.id.emissivity_seekbar:
                    int currentProgressEm = seekBar.getProgress();
                    float fiputEm = currentProgressEm / 100.0f;
                    byte[] iputEm = new byte[4];
                    mByteUtil.putFloat(iputEm, fiputEm, 0);
                    mSendCommand.sendFloatCommand(4 * 4, iputEm[0], iputEm[1], iputEm[2], iputEm[3], 20, 40, 60, 80, 120);
                    break;
                case R.id.correction_seekbar:
                    int currentProgressCo = seekBar.getProgress();
                    float fiputCo = (currentProgressCo - 30) / 10.0f;
                    byte[] iputCo = new byte[4];
                    mByteUtil.putFloat(iputCo, fiputCo, 0);
                    mSendCommand.sendFloatCommand(0 * 4, iputCo[0], iputCo[1], iputCo[2], iputCo[3], 20, 40, 60, 80, 120);
                    break;
                case R.id.reflection_seekbar:
                    int currentProgressRe = seekBar.getProgress();
                    float fiputRe = currentProgressRe - 10.0f;
                    byte[] iputRe = new byte[4];
                    mByteUtil.putFloat(iputRe, fiputRe, 0);
                    mSendCommand.sendFloatCommand(1 * 4, iputRe[0], iputRe[1], iputRe[2], iputRe[3], 20, 40, 60, 80, 120);
                    break;
                case R.id.amb_temp_seekbar:
                    int currentProgressAm = seekBar.getProgress();
                    float fiputAm = currentProgressAm - 10.0f;
                    byte[] iputAm = new byte[4];
                    mByteUtil.putFloat(iputAm, fiputAm, 0);
                    mSendCommand.sendFloatCommand(2 * 4, iputAm[0], iputAm[1], iputAm[2], iputAm[3], 20, 40, 60, 80, 120);
                    break;
                case R.id.humidity_seekbar:
                    int currentProgressHu = seekBar.getProgress();
                    float fiputHu = currentProgressHu / 100.0f;
                    byte[] iputHu = new byte[4];
                    mByteUtil.putFloat(iputHu, fiputHu, 0);
                    mSendCommand.sendFloatCommand(3 * 4, iputHu[0], iputHu[1], iputHu[2], iputHu[3], 20, 40, 60, 80, 120);
                    break;
                case R.id.distance_seekbar:
                    int currentProgressDi = seekBar.getProgress();
                    byte[] bIputDi = new byte[4];
                    mByteUtil.putInt(bIputDi, currentProgressDi, 0);
                    mSendCommand.sendShortCommand(5 * 4, bIputDi[0], bIputDi[1], 20, 40, 60);
                    break;

            }
        }
    };


    public static class Check {
        // 两次点击按钮之间的点击间隔不能少于1000毫秒
        private static final int MIN_CLICK_DELAY_TIME = 1000;
        private static long lastClickTime;

        public static boolean isFastClick() {
            boolean flag = false;
            long curClickTime = System.currentTimeMillis();
            if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
                flag = true;
            }
            lastClickTime = curClickTime;
            return flag;
        }
    }

    private CompoundButton.OnCheckedChangeListener mSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()) {
                case R.id.sys_camera_swtich:
                    if (!Check.isFastClick()) {
                        return;
                    }
                    if (b) {
                        if (mCameraHandler.isOpened()) {
                            if (PermissionCheck.hasCamera(context)) {
                                /*旋转mTextureView*/
                                mTextureView.setPivotX(0);
                                mTextureView.setPivotY(0);
                                mTextureView.setRotation(-90);
//                                mTextureView.setTranslationY(480);

                                mCamera2Helper = Camera2Helper.getInstance();
                                mCamera2Helper.setContext(context);
                                mCamera2Helper.setTexture(mTextureView);
                                mCameraHandler.openSystemCamera();
                                mTextureView.setVisibility(View.VISIBLE);
//                                sharedPreferences.edit().putBoolean("cameraPreview", true).commit();
                                compoundButton.setChecked(true);
                            } else {
                                checkPermissionCamera();
                                compoundButton.setChecked(false);
                                mTextureView.setVisibility(View.INVISIBLE);
//                                sharedPreferences.edit().putBoolean("cameraPreview", false).commit();
                                return;
                            }
                        }

                    } else {
                        if (mCameraHandler.isOpened() && (mCamera2Helper != null)) {
                            mCameraHandler.closeSystemCamera();
                            compoundButton.setChecked(false);
                            mTextureView.setVisibility(View.INVISIBLE);
//                            sharedPreferences.edit().putBoolean("cameraPreview", false).commit();
                        }
                    }
                    break;
                case R.id.watermark_swtich:
                    if (b) {
                        if (mCameraHandler.isOpened()) {
                            isWatermark = 1;
                        }
                    } else {
                        isWatermark = 0;
                    }
                    mCameraHandler.watermarkOnOff(isWatermark);
                    sharedPreferences.edit().putInt("Watermark", isWatermark).commit();
                    break;
            }
        }
    };
    float[] gravity = new float[3];//用来保存加速度传感器的值
    float[] r = new float[9];//
    float[] geomagnetic = new float[3];//用来保存地磁传感器的值
    float[] values = new float[3];//用来保存最终的结果
    private final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[SensorManager.DATA_X];
                float y = event.values[SensorManager.DATA_Y];
                float z = event.values[SensorManager.DATA_Z];
                relayout(x, y, z);
            }
        }
    };

    protected void relayout(float x, float y, float z) {
        Drawable drawable;
        if (x > -2.5 && x <= 2.5 && y > 7.5 && y <= 10 && oldRotation != 270) {
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.camera1left, null);
            mPhotographButton.setImageDrawable(drawable);
            if (mCameraHandler.isRecording()) {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video2up, null);
            } else {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video1left, null);
            }
            mCaptureButton.setImageDrawable(drawable);
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.fileleft, null);
            mThumbnailButton.setImageDrawable(drawable);
            oldRotation = 270;
            rightmenu.setRotation(0);
            menu_palette_layout.setRotation(0);
            if (temperatureAnalysisWindow != null && temperatureAnalysisWindow.isShowing()) {
                temperatureAnalysisWindow.getContentView().setRotation(0);
            }
        } else if (x > 7.5 && x <= 10 && y > -2.5 && y <= 2.5 && oldRotation != 0) {
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.camera1, null);
            mPhotographButton.setImageDrawable(drawable);
            if (mCameraHandler.isRecording()) {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video2, null);
            } else {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video1, null);
            }
            mCaptureButton.setImageDrawable(drawable);
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.file, null);
            mThumbnailButton.setImageDrawable(drawable);
            oldRotation = 0;
            rightmenu.setRotation(oldRotation);
            menu_palette_layout.setRotation(oldRotation);
            if (temperatureAnalysisWindow != null && temperatureAnalysisWindow.isShowing()) {
                temperatureAnalysisWindow.getContentView().setRotation(oldRotation);
            }
        } else if (x > -2.5 && x <= 2.5 && y > -10 && y <= -7.5 && oldRotation != 90) {
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.camera1right, null);
            mPhotographButton.setImageDrawable(drawable);
            if (mCameraHandler.isRecording()) {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video2down, null);
            } else {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video1right, null);
            }
            mCaptureButton.setImageDrawable(drawable);
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.fileright, null);
            mThumbnailButton.setImageDrawable(drawable);
            oldRotation = 90;
            rightmenu.setRotation(180);
            menu_palette_layout.setRotation(180);
            if (temperatureAnalysisWindow != null && temperatureAnalysisWindow.isShowing()) {
                temperatureAnalysisWindow.getContentView().setRotation(180);
            }
        } else if (x > -10 && x <= -7.5 && y > -2.5 && y < 2.5 && oldRotation != 180) {
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.camera1down, null);
            mPhotographButton.setImageDrawable(drawable);
            if (mCameraHandler.isRecording()) {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video2left, null);
            } else {
                drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.video1down, null);
            }
            mCaptureButton.setImageDrawable(drawable);
            drawable = ResourcesCompat.getDrawable(getResources(), R.mipmap.filedown, null);
            mThumbnailButton.setImageDrawable(drawable);
            oldRotation = 180;
            rightmenu.setRotation(oldRotation);
            menu_palette_layout.setRotation(oldRotation);
            if (temperatureAnalysisWindow != null && temperatureAnalysisWindow.isShowing()) {
                temperatureAnalysisWindow.getContentView().setRotation(oldRotation);
            }
        } else {
            return;
        }
        Log.e(TAG, "oldRotation:" + oldRotation);
        mCameraHandler.relayout(oldRotation);
    }

    private final RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.whitehot_radio_button:
                    mCameraHandler.changePalette(0);
                    sharedPreferences.edit().putInt("palette", 0).commit();
                    break;
                case R.id.blackhot_radio_button:
                    mCameraHandler.changePalette(1);
                    sharedPreferences.edit().putInt("palette", 1).commit();
                    break;
                case R.id.iron_rainbow_radio_button:
                    mCameraHandler.changePalette(2);
                    sharedPreferences.edit().putInt("palette", 2).commit();
                    break;
                case R.id.rainbow_radio_button:
                    mCameraHandler.changePalette(3);
                    sharedPreferences.edit().putInt("palette", 3).commit();
                    break;
                case R.id.three_primary_radio_button:
                    mCameraHandler.changePalette(4);
                    sharedPreferences.edit().putInt("palette", 4).commit();
                    break;
                case R.id.iron_gray_radio_button:
                    mCameraHandler.changePalette(5);
                    sharedPreferences.edit().putInt("palette", 5).commit();
                    break;
                case R.id.temperature_units_c_radio_button:
                    if (mUVCCameraView != null) {
                        mUVCCameraView.setUnitTemperature(0);
                        sharedPreferences.edit().putInt("UnitTemperature", 0).commit();
                    }
                    break;
                case R.id.temperature_units_f_radio_button:
                    if (mUVCCameraView != null) {
                        mUVCCameraView.setUnitTemperature(1);
                        sharedPreferences.edit().putInt("UnitTemperature", 1).commit();
                    }
                    break;
                case R.id.chinese_radio_button:
                    language = sharedPreferences.getInt("Language", -1);
                    if (language != 0) {
                        sharedPreferences.edit().putInt("Language", 0).commit();
                        changeAppLanguage(Locale.SIMPLIFIED_CHINESE);
                    }
                    break;
                case R.id.english_radio_button:
                    language = sharedPreferences.getInt("Language", -1);
                    if (language != 1) {
                        sharedPreferences.edit().putInt("Language", 1).commit();
                        changeAppLanguage(Locale.ENGLISH);
                    }
                    break;
            }
        }
    };

    /**
     * event handler when click camera / capture button
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_video:
                    if (!Check.isFastClick()) {
                        return;
                    }
                    if (mCameraHandler.isOpened()) {
                        if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                            Thread thread = new Thread(timeRunable);
                            if (!mCameraHandler.isRecording()) {
                                isOnRecord = true;
                                ((ImageButton) view).setImageDrawable(getResources().getDrawable(R.mipmap.video2));    // turn red
                                mThumbnailButton.setVisibility(INVISIBLE);
                                mCameraHandler.startRecording();
                                thread.start();
                            } else {
                                ((ImageButton) view).setImageDrawable(getResources().getDrawable(R.mipmap.video1));    // return to default color
                                isOnRecord = false;
                                thread.interrupt();
                                mThumbnailButton.setVisibility(VISIBLE);
                                mCameraHandler.stopRecording();
                            }
                        }
                    }
                    break;
                case R.id.button_temp:
                    createTemperaturePopupWindow();
                    if (!temperatureAnalysisWindow.isShowing()) {
                        if (oldRotation == 90 || oldRotation == 180) {
                            int offsetX = -(menu_palette_layout.getWidth() + temperatureAnalysisWindow.getWidth());
                            temperatureAnalysisWindow.showAsDropDown(menu_palette_layout, offsetX, 0, Gravity.START);
                            temperatureAnalysisWindow.getContentView().setRotation(180);
                        }
                        if (oldRotation == 0 || oldRotation == 270) {
                            int offsetX = -temperatureAnalysisWindow.getWidth();
                            temperatureAnalysisWindow.showAsDropDown(menu_palette_layout, offsetX, 0, Gravity.START);
                            temperatureAnalysisWindow.getContentView().setRotation(0);
                        }
                        WindowManager.LayoutParams wlp = getWindow().getAttributes();
                        wlp.alpha = 0.7f;
                        getWindow().setAttributes(wlp);
                    } else {
                        temperatureAnalysisWindow.dismiss();
                    }
                    break;
                case R.id.button_shut:
                    if (isTemperaturing) {
                        whenShutRefresh();
                    }
                    setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
                    break;
                case R.id.button_set:
                    if (settingsIsShow == false) {
                        if (mUsbDevice != null) {
                            settingsIsShow = true;
                            palette = sharedPreferences.getInt("palette", 0);
                            UnitTemperature = sharedPreferences.getInt("UnitTemperature", 0);
                            switch (palette) {
                                case 0:
                                    paletteRadioGroup.check(R.id.whitehot_radio_button);
                                    break;
                                case 1:
                                    paletteRadioGroup.check(R.id.blackhot_radio_button);
                                    break;
                                case 2:
                                    paletteRadioGroup.check(R.id.iron_rainbow_radio_button);
                                    break;
                                case 3:
                                    paletteRadioGroup.check(R.id.rainbow_radio_button);
                                    break;
                                case 4:
                                    paletteRadioGroup.check(R.id.three_primary_radio_button);
                                    break;
                                case 5:
                                    paletteRadioGroup.check(R.id.iron_gray_radio_button);
                                    break;
                            }
                            switch (UnitTemperature) {
                                case 0:
                                    temperatureUnitsRadioGroup.check(R.id.temperature_units_c_radio_button);
                                    break;
                                case 1:
                                    temperatureUnitsRadioGroup.check(R.id.temperature_units_f_radio_button);
                                    break;
                            }
                            switch (language) {
                                case -1:
                                    if (locale_language == "zh") {
                                        languageRadioGroup.check(R.id.chinese_radio_button);
                                    } else if (locale_language == "en") {
                                        languageRadioGroup.check(R.id.english_radio_button);
                                    }
                                    break;
                                case 0:
                                    languageRadioGroup.check(R.id.chinese_radio_button);
                                    break;
                                case 1:
                                    languageRadioGroup.check(R.id.english_radio_button);
                                    break;
                            }
                            if (isWatermark == 1) {
                                mWatermarkSwitch.setChecked(true);
                            } else {
                                mWatermarkSwitch.setChecked(false);
                            }

                            getTempPara();
                            emissivityText.setText(stEmiss);
                            emissivitySeekbar.setProgress((int) (emiss * 100.0f));
                            correctionText.setText(stFix + "°C");
                            correctionSeekbar.setProgress((int) (Fix * 10.0f + 30));
                            reflectionText.setText(stRefltmp + "°C");
                            reflectionSeekbar.setProgress((int) (Refltmp + 10.0f));
                            ambtempText.setText(stAirtmp + "°C");
                            ambtempSeekbar.setProgress((int) (Airtmp + 10.0f));
                            humidityText.setText(stHumi);
                            humiditySeekbar.setProgress((int) (humi * 100.0f));
                            distanceText.setText(stDistance);
                            distanceSeekbar.setProgress((int) distance);
                            PN.setText(mUsbDevice.getProductName());
                            // PID.setText(mUsbDevice.getProductId());
                            SN.setText(mUsbDevice.getSerialNumber());
                            sotfVersion.setText(getVersionName(context));
                            productSoftVersion.setText(stProductSoftVersion);

                            //highThrowSeekbar,lowThrowSeekbar,lowPlatSeekbar,highPlatSeekbar,orgSubGsHighSeekbar,orgSubGsLowSeekbar,sigmaDSeekbar,sigmaRSeekbar;
                            //private TextView  highThrowText,lowThrowText,lowPlatText,highPlatText,orgSubGsHighText,orgSubGsLowText,sigmaDText,sigmaRText;
//                        highThrow=mCameraHandler.getHighThrow();
//                        highThrowSeekbar.setProgress(highThrow);
//                        highThrowText.setText(String.valueOf(highThrow));
//
//                        lowThrow=mCameraHandler.getLowThrow();
//                        lowThrowSeekbar.setProgress(lowThrow);
//                        lowThrowText.setText(String.valueOf(lowThrow));
//
//                        highPlat=mCameraHandler.getHighPlat();
//                        highPlatSeekbar.setProgress(highPlat);
//                        highPlatText.setText(String.valueOf(highPlat));
//
//                        lowPlat=mCameraHandler.getLowPlat();
//                        lowPlatSeekbar.setProgress(lowPlat);
//                        lowPlatText.setText(String.valueOf(lowPlat));
//
//                        OrgSubGsHigh=mCameraHandler.getOrgSubGsHigh();
//                        orgSubGsHighSeekbar.setProgress(OrgSubGsHigh);
//                        orgSubGsHighText.setText(String.valueOf(OrgSubGsHigh));
//
//                        OrgSubGsLow=mCameraHandler.getOrgSubGsLow();
//                        orgSubGsLowSeekbar.setProgress(OrgSubGsLow);
//                        orgSubGsLowText.setText(String.valueOf(OrgSubGsLow));
//
//                        float sigmaDfloat=mCameraHandler.getSigmaD();
//                        sigmaD=(int)(sigmaDfloat*10.0);
//                        sigmaDSeekbar.setProgress(sigmaD);
//                        sigmaDText.setText(String.valueOf(sigmaD));
//
//                        float sigmaRfloat=mCameraHandler.getSigmaR();
//                        Log.e(TAG,"sigmaRfloat"+sigmaRfloat);
//                        sigmaR=(int)(sigmaRfloat*10.0);
//                        sigmaRSeekbar.setProgress(sigmaR);
//                        sigmaRText.setText(String.valueOf(sigmaR));
//                        rightmenu.setAlpha(0.9f);
                            rightmenu.setVisibility(VISIBLE);
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.waittoclick, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        settingsIsShow = false;
                        rightmenu.setVisibility(INVISIBLE);
                        //setValue(UVCCamera.CTRL_ZOOM_ABS, 0x80ff);
                    }

                    /*createSettingsPopupWindow();
                    if (!settingsWindows.isShowing()) {
                        //popupWindow.showAsDropDown(mTitleLayout,mContentLayout.getMeasuredWidth()/2,0);
                        settingsWindows.showAtLocation(mTempbutton, Gravity.LEFT, mTempbutton.getWidth()+PixAndDpUtil.dip2px(context,10), 0);
                        WindowManager.LayoutParams wlp = getWindow().getAttributes();
                        wlp.alpha = 0.7f;
                        getWindow().setAttributes(wlp);
                    } else {
                        settingsWindows.dismiss();
                    }*/
                    break;
                    /*if (mCameraHandler.isOpened()) {
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        if (settingFragment == null) {
                            settingFragment = new SettingFragment();
                            fragmentTransaction.add(R.id.content_layout, settingFragment, "setting");
                            fragmentTransaction.commit();
                            fragmentTransaction.show(settingFragment);
                            isSetting = true;
                            settingFragment.setIsOnSetting(isSetting);
                        } else {
                            Fragment fragment = fragmentManager.findFragmentByTag("setting");
                            fragmentTransaction.show(settingFragment);
                            fragmentTransaction.commit();
                            isSetting = true;
                            settingFragment.setIsOnSetting(isSetting);
                        }
                        //				getFragmentManager().beginTransaction()
                        //						.replace(R.id.content_layout, settingFragment).commit();
                        //				getFragmentManager().beginTransaction().show(settingFragment);


                    }
                    break;*/
                case R.id.save_button:
                    setValue(UVCCamera.CTRL_ZOOM_ABS, 0x80ff);
                    Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.button_camera:
                    if (!Check.isFastClick()) {
                        return;
                    }
                    if (mCameraHandler != null) {
                        if (mCameraHandler.isOpened()) {
                            if (checkPermissionWriteExternalStorage()) {
                                mCameraHandler.captureStill(MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png").toString());
                            }
                        }
                    }
                    break;
                case R.id.make_report_button:
                    if (mCameraHandler.isOpened() && isTemperaturing) {
                        if (checkPermissionWriteExternalStorage()) {
                            //String path=
                            mCameraHandler.makeReport();
                            temperatureAnalysisWindow.dismiss();
                            Toast.makeText(getApplication(), "报告生成成功，请去相册目录查看", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;

                case R.id.point_mode_button:
                    if (!isTemperaturing) {
                        //mUVCCameraView.setVertices(2);
                        //mUVCCameraView.setBitmap(mCursorRed,mCursorGreen,mCursorBlue,mCursorYellow);
                        if (mCameraHandler.isOpened()) {
                            if (!mCameraHandler.isTemperaturing()) {
                                temperatureAnalysisMode = 0;
                                mUVCCameraView.setTemperatureAnalysisMode(0);
                                mCameraHandler.startTemperaturing();
                                isTemperaturing = true;
                                Handler handler0 = new Handler();
                                handler0.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        whenShutRefresh();
                                    }
                                }, 300);

                                Handler handler1 = new Handler();
                                handler1.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        //whenShutRefresh();
                                        setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
                                    }
                                }, 400);

                                pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point1));
                                temperatureAnalysisWindow.dismiss();
                            }
                        }
                    } else if (temperatureAnalysisMode != 0) {
                        mTouchPoint.clear();
                        mUVCCameraView.setTouchPoint(mTouchPoint);
                        temperatureAnalysisMode = 0;
                        isTemperaturing = true;
                        mUVCCameraView.setTemperatureAnalysisMode(0);
                        pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point1));
                        lineModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.line));
                        rectangleModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.rectangle));
                        temperatureAnalysisWindow.dismiss();
                    } else {
                        isTemperaturing = false;
                        needClearCanvas = true;
                        mCameraHandler.stopTemperaturing();
                        mTouchPoint.clear();
                        mUVCCameraView.setTouchPoint(mTouchPoint);
                        pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point));
                        temperatureAnalysisWindow.dismiss();
                    }
                    break;
                case R.id.line_mode_button:
                    if (!isTemperaturing) {
                        //mUVCCameraView.setVertices(2);
                        //mUVCCameraView.setBitmap(mCursorRed,mCursorGreen,mCursorBlue,mCursorYellow);
                        if (mCameraHandler.isOpened()) {
                            if (!mCameraHandler.isTemperaturing()) {
                                temperatureAnalysisMode = 1;
                                mUVCCameraView.setTemperatureAnalysisMode(1);
                                mCameraHandler.startTemperaturing();
                                isTemperaturing = true;
                                Handler handler0 = new Handler();
                                handler0.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        whenShutRefresh();
                                    }
                                }, 300);

                                Handler handler1 = new Handler();
                                handler1.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        //whenShutRefresh();
                                        setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
                                    }
                                }, 400);

                                pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point));
                                lineModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.line1));
                                rectangleModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.rectangle));
                                temperatureAnalysisWindow.dismiss();
                            }
                        }
                    } else if (temperatureAnalysisMode != 1) {
                        mTouchPoint.clear();
                        mUVCCameraView.setTouchPoint(mTouchPoint);
                        temperatureAnalysisMode = 1;
                        mUVCCameraView.setTemperatureAnalysisMode(1);
                        pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point));
                        lineModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.line1));
                        rectangleModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.rectangle));
                        temperatureAnalysisWindow.dismiss();
                    } else {
                        isTemperaturing = false;
                        needClearCanvas = true;
                        mCameraHandler.stopTemperaturing();
                        mTouchPoint.clear();
                        mUVCCameraView.setTouchPoint(mTouchPoint);
                        lineModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.line));
                        temperatureAnalysisWindow.dismiss();
                    }


                    break;
                case R.id.rectangle_mode_button:
                    if (!isTemperaturing) {
                        //mUVCCameraView.setVertices(2);
                        //mUVCCameraView.setBitmap(mCursorRed,mCursorGreen,mCursorBlue,mCursorYellow);
                        if (mCameraHandler.isOpened()) {
                            if (!mCameraHandler.isTemperaturing()) {
                                temperatureAnalysisMode = 2;
                                mUVCCameraView.setTemperatureAnalysisMode(2);
                                mCameraHandler.startTemperaturing();
                                isTemperaturing = true;
                                Handler handler0 = new Handler();
                                handler0.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        whenShutRefresh();
                                    }
                                }, 300);

                                Handler handler1 = new Handler();
                                handler1.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        //whenShutRefresh();
                                        setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
                                    }
                                }, 400);

                                pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point));
                                lineModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.line));
                                rectangleModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.rectangle1));
                                temperatureAnalysisWindow.dismiss();
                            }
                        }
                    } else if (temperatureAnalysisMode != 2) {
                        mTouchPoint.clear();
                        mUVCCameraView.setTouchPoint(mTouchPoint);
                        temperatureAnalysisMode = 2;
                        mUVCCameraView.setTemperatureAnalysisMode(2);
                        pointModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.point));
                        lineModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.line));
                        rectangleModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.rectangle1));
                        temperatureAnalysisWindow.dismiss();
                    } else {
                        isTemperaturing = false;
                        needClearCanvas = true;
                        mCameraHandler.stopTemperaturing();
                        mTouchPoint.clear();
                        mUVCCameraView.setTouchPoint(mTouchPoint);
                        rectangleModeButton.setImageDrawable(getResources().getDrawable(R.mipmap.rectangle));
                        temperatureAnalysisWindow.dismiss();
                    }

                    break;
                case R.id.change_range_button:

                    if (mCameraHandler.isOpened()) {
                        if (TemperatureRange != 400) {
                            TemperatureRange = 400;
                            mCameraHandler.setTempRange(400);
                            isTemperaturing = true;
                            Handler handler1 = new Handler();
                            handler1.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //whenShutRefresh();
                                    setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8021);//400。C
                                }
                            }, 100);
                            Handler handler2 = new Handler();
                            handler2.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //whenShutRefresh();
                                    setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
                                }
                            }, 600);
                            Handler handler4 = new Handler();
                            handler4.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    whenShutRefresh();
                                    //    mCameraHandler.whenChangeTempPara();
                                }
                            }, 1500);
                            ChangeRangeButton.setImageDrawable(getResources().getDrawable(R.mipmap.range_120));
                            temperatureAnalysisWindow.dismiss();
                        } else {
                            TemperatureRange = 120;
                            mCameraHandler.setTempRange(120);
                            isTemperaturing = true;
                            Handler handler1 = new Handler();
                            handler1.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //whenShutRefresh();
                                    setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8020);//120。C
                                }
                            }, 100);
                            Handler handler2 = new Handler();
                            handler2.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //whenShutRefresh();
                                    setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);
                                }
                            }, 600);
                            Handler handler4 = new Handler();
                            handler4.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    whenShutRefresh();
                                    //    mCameraHandler.whenChangeTempPara();
                                }
                            }, 1500);
                            ChangeRangeButton.setImageDrawable(getResources().getDrawable(R.mipmap.range_400));
                            temperatureAnalysisWindow.dismiss();
                        }
                    }
                    break;
                case R.id.imageview_thumbnail:

                    if (PermissionCheck.hasReadExternalStorage(context) && PermissionCheck.hasWriteExternalStorage(context)) {
                        Matisse.from(MainActivity.this)
                                .choose(MimeType.ofAll(), false)
                                .theme(R.style.Matisse_Dracula)
                                .countable(false)
                                .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                                .maxSelectable(9)
                                .originalEnable(true)
                                .maxOriginalSize(10)
                                //.imageEngine(new GlideEngine())
                                .imageEngine(new PicassoEngine())
                                .forResult(REQUEST_CODE_CHOOSE);
                    } else {
                        checkPermissionWriteExternalStorage();
                    }

                    break;
            }

        }

    };

    /**
     * 添加或者显示碎片
     *
     * @param transaction
     * @param fragment
     */
    private void addOrShowFragment(FragmentTransaction transaction,
                                   Fragment fragment) {
        if (currentFragment == fragment)
            return;

        if (!fragment.isAdded()) { // 如果当前fragment未被添加，则添加到Fragment管理器中
            transaction.hide(currentFragment)
                    .add(R.id.content_layout, fragment).commit();
        } else {
            transaction.hide(currentFragment).show(fragment).commit();
        }

        currentFragment = fragment;
    }


    /**
     * 更改应用语言
     *
     * @param locale
     */
    public void changeAppLanguage(Locale locale) {
//        metrics = getResources().getDisplayMetrics();
//        configuration = getResources().getConfiguration();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            configuration.setLocale(locale);
//        } else {
//            configuration.locale = locale;
//        }
//        getResources().updateConfiguration(configuration, metrics);
        //重新启动Activity
//        Log.e(TAG, "changeAppLanguage: ");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.finish();
        startActivity(intent);
    }

    private void createTemperaturePopupWindow() {
        if (temperatureAnalysisWindow == null) {
            View contentView = LayoutInflater.from(MainActivity.this).inflate(R.layout.temperature_analysis_layout, null);
            temperatureAnalysisWindow = new PopupWindow(contentView, (int) (mMenuRight.getHeight() / 5.806f),
                    mMenuRight.getHeight());

//            if (isT3) {
//                contentView.setRotation(oldRotation);
//                Log.e(TAG,"oldRotation1"+oldRotation);
//            }
            //temperatureAnalysisWindow = new PopupWindow(contentView);
            //temperatureAnalysisWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            // temperatureAnalysisWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
            //temperatureAnalysisWindow.setContentView(contentView);
            //设置各个控件的点击响应
            pointModeButton = (ImageButton) contentView.findViewById(R.id.point_mode_button);
            lineModeButton = (ImageButton) contentView.findViewById(R.id.line_mode_button);
            rectangleModeButton = (ImageButton) contentView.findViewById(R.id.rectangle_mode_button);
            ChangeRangeButton = (ImageButton) contentView.findViewById(R.id.change_range_button);

            MakeReportButton = (ImageButton) contentView.findViewById(R.id.make_report_button);
            pointModeButton.setOnClickListener(mOnClickListener);
            lineModeButton.setOnClickListener(mOnClickListener);
            rectangleModeButton.setOnClickListener(mOnClickListener);
            ChangeRangeButton.setOnClickListener(mOnClickListener);
            MakeReportButton.setOnClickListener(mOnClickListener);
            //显示PopupWindow
            temperatureAnalysisWindow.setFocusable(true);
            // temperatureAnalysisWindow.setAnimationStyle(R.style.DialogAnimation);
            temperatureAnalysisWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            temperatureAnalysisWindow.setOutsideTouchable(true);
            temperatureAnalysisWindow.setTouchable(true);
            temperatureAnalysisWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    WindowManager.LayoutParams wlp = getWindow().getAttributes();
                    wlp.alpha = 1.0f;
                    getWindow().setAttributes(wlp);
                }
            });
        }
    }


//			popupWindow.setTouchInterceptor(new View.OnTouchListener() {
//				@Override
//				public boolean onTouch(View v, MotionEvent event) {
//				//	return false;   // 这里面拦截不到返回键
//					switch (v.getId()) {
//						case R.id.palette1:
//							setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8800);
//							popupWindow.dismiss();
//							break;
//						case R.id.palette2:
//							setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8801);
//							popupWindow.dismiss();
//							break;
//						case R.id.palette3:
//							setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8802);
//							popupWindow.dismiss();
//							break;
//						case R.id.palette4:
//							setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8803);
//							popupWindow.dismiss();
//							break;
//					}
//			});




	/*private void whenCloseClearCanvas() {
        canvas = mSfh.lockCanvas();
		try {
			if (canvas != null) {
				canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
				mSfh.unlockCanvasAndPost(canvas);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		paletteCanvas = mRightSfh.lockCanvas();
		try {
			if (paletteCanvas != null) {
				paletteCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
				mRightSfh.unlockCanvasAndPost(paletteCanvas);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}*/
    /**
     * 点测温
     */
    float startX = 0;
    float startY = 0;

    float endX = 0;
    float endY = 0;
    View.OnTouchListener listener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (settingsIsShow) {
                settingsIsShow = false;
                rightmenu.setVisibility(INVISIBLE);
            } else if (isTemperaturing) {
                //mScaleGestureDetector.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        TouchPoint currentPoint = new TouchPoint();
                        currentPoint.x = event.getX();
                        currentPoint.y = event.getY();
//                        Log.e(TAG, "OnTouchListener" + currentPoint.x + ",," + currentPoint.y);
                        float fviewX = currentPoint.x / mSfv.getWidth();
                        float fviewY = currentPoint.y / mSfv.getHeight();
                        currentPoint.x = fviewX;
                        currentPoint.y = fviewY;
                        if (temperatureAnalysisMode == 0) {
                            if (indexOfPoint >= 5) {
                                indexOfPoint = 0;
                            }
                            currentPoint.numOfPoint = indexOfPoint;
                            if (mTouchPoint.size() <= 5) {
                                mTouchPoint.add(currentPoint);
                            } else {
                                mTouchPoint.set(indexOfPoint, currentPoint);
                            }
                            mUVCCameraView.setTouchPoint(mTouchPoint);
                            indexOfPoint++;
                        }
                        if (temperatureAnalysisMode == 1) {
                            mTouchPoint.clear();
                            indexOfPoint = 0;
                            currentPoint.numOfPoint = indexOfPoint;
                            mTouchPoint.add(currentPoint);

                        }

                        if (temperatureAnalysisMode == 2) {
                            mTouchPoint.clear();
                            indexOfPoint = 0;
                            currentPoint.numOfPoint = indexOfPoint;
                            mTouchPoint.add(currentPoint);

                        }
                        if (isSettingBadPixel) {//用户盲元表
                            int viewX1 = (int) (fviewX * mCameraHandler.getWidth());
                            int viewY1 = (int) (fviewY * (mCameraHandler.getHeight() - 4));
                            posx = 0xec00 | (0xffff & viewX1);
                            posy = 0xee00 | (0xffff & viewY1);
                            Handler handler1 = new Handler();
                            handler1.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setValue(UVCCamera.CTRL_ZOOM_ABS, posx);
                                }
                            }, 10);
                            Handler handler2 = new Handler();
                            handler2.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setValue(UVCCamera.CTRL_ZOOM_ABS, posy);
                                }
                            }, 40);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        TouchPoint currentPoint2 = new TouchPoint();
                        currentPoint2.x = event.getX();
                        currentPoint2.y = event.getY();
//                        Log.e(TAG, "OnTouchListener" + currentPoint2.x + ",," + currentPoint2.y);
                        float fviewX2 = currentPoint2.x / mSfv.getWidth();
                        float fviewY2 = currentPoint2.y / mSfv.getHeight();
                        currentPoint2.x = fviewX2;
                        currentPoint2.y = fviewY2;
                        if (temperatureAnalysisMode == 1) {
                            if (indexOfPoint >= 2) {
                                TouchPoint LastTouch = mTouchPoint.get(mTouchPoint.size() - 1);
                                LastTouch.x = currentPoint2.x;
                                LastTouch.y = currentPoint2.y;
                                mUVCCameraView.setTouchPoint(mTouchPoint);
                            } else {
                                currentPoint2.numOfPoint = 1;
                                mTouchPoint.add(currentPoint2);
                                mUVCCameraView.setTouchPoint(mTouchPoint);
                            }

                        }

                        if (temperatureAnalysisMode == 2) {
                            if (indexOfPoint >= 2) {
                                TouchPoint LastTouch = mTouchPoint.get(mTouchPoint.size() - 1);
                                LastTouch.x = currentPoint2.x;
                                LastTouch.y = currentPoint2.y;
                                mUVCCameraView.setTouchPoint(mTouchPoint);
                            } else {
                                currentPoint2.numOfPoint = 1;
                                mTouchPoint.add(currentPoint2);
                                mUVCCameraView.setTouchPoint(mTouchPoint);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        break;

                    default:

                }

            }

            return true;
        }

    };


    /**
     * capture still image when you long click on preview image(not on buttons)
     */
    private final OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(final View view) {
            switch (view.getId()) {
                case R.id.camera_view:


            }
            return false;
        }
    };

    private void setCameraButton(final boolean isOn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//				if (mCameraButton != null) {
//					try {
//						mCameraButton.setOnCheckedChangeListener(null);
//						mCameraButton.setChecked(isOn);
//					} finally {
//						mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
//					}
//				}
                if (!isOn && (mCaptureButton != null)) {
                    if (!isTemperaturing) {
                        mTempbutton.setImageDrawable(getResources().getDrawable(R.mipmap.temp1));
                    }
                    if (!isOnRecord) {
                        mCaptureButton.setImageDrawable(getResources().getDrawable(R.mipmap.video1));
                    }
                    mPhotographButton.setVisibility(INVISIBLE);
                    mSetButton.setVisibility(INVISIBLE);
                    mCaptureButton.setVisibility(INVISIBLE);
                    mZoomButton.setVisibility(INVISIBLE);
                    mTempbutton.setVisibility(INVISIBLE);
                    mClearButton.setVisibility(INVISIBLE);
                    mPalette.setVisibility(INVISIBLE);

                    //textMax.setVisibility(View.INVISIBLE);
                    //textMin.setVisibility(View.INVISIBLE);
                }
            }
        }, 0);
        updateItems();
    }

    private void startPreview() {
        mLeft = mImageView.getLeft();
        mTop = mImageView.getTop();
        mBottom = mImageView.getBottom();
        mRight = mImageView.getRight();

        mRightSurfaceLeft = mRightSfv.getLeft();
        mRightSurfaceRight = mRightSfv.getRight();
        mRightSurfaceTop = mRightSfv.getTop();
        mRightSurfaceBottom = mRightSfv.getBottom();
        mUVCCameraView.iniTempBitmap(mRight - mLeft, mBottom - mTop);
        icon = Bitmap.createBitmap(mRight - mLeft, mBottom - mTop, Bitmap.Config.ARGB_8888); //建立一个空的图画板
        int iconPaletteWidth = abs(mRightSurfaceRight - mRightSurfaceLeft);
        int iconPaletteHeight = abs(mRightSurfaceBottom - mRightSurfaceTop);
        iconPalette = Bitmap.createBitmap(iconPaletteWidth > 0 ? iconPaletteWidth : 10, iconPaletteHeight > 0 ? iconPaletteHeight : 10, Bitmap.Config.ARGB_8888);
        bitcanvas = new Canvas(icon);//初始化画布绘制的图像到icon上
        paletteBitmapCanvas = new Canvas(iconPalette);
        //sfh.lockCanvas()
        photoPaint = new Paint(); //建立画笔
        palettePaint = new Paint();
        //photoPaint.setStyle(Paint.Style.FILL);
        //dstHighTemp=new Rect(0,0,60,60);
        //dstLowTemp=new Rect(0,0,60,60);
        //dstHighTemp.set(20,50,20+mCursor.getWidth(),50+mCursor.getHeight());//int left, int top, int right, int bottom
        //dstLowTemp.set(40,100,40+mCursor.getWidth(),100+mCursor.getHeight());//int left, int top, int right, int bottom

//        Log.e(TAG, "startPreview: getSurfaceTexture");
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        mCameraHandler.startPreview(new Surface(st));
//        Log.e(TAG, "startPreview: getSurfaceTexture2");
        //mCameraHandler.startPreview(null);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPhotographButton.setVisibility(VISIBLE);
                mSetButton.setVisibility(VISIBLE);
                mCaptureButton.setVisibility(VISIBLE);
                mZoomButton.setVisibility(VISIBLE);
                mTempbutton.setVisibility(VISIBLE);

            }
        });
        updateItems();
    }


    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (device.getDeviceClass() == 239 && device.getDeviceSubclass() == 2) {
                //  Toast.makeText(MainActivity.this,device.getProductName(), Toast.LENGTH_SHORT).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mUSBMonitor.requestPermission(device);
                    }
                }, 100);
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            //  Toast.makeText(MainActivity.this,"onConnect", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onConnect:");
            if (isOpened == 0) {
                // Toast.makeText(MainActivity.this, "XthermDemo onConnect", Toast.LENGTH_SHORT).show();
                mCameraHandler.open(ctrlBlock);
                if (!XthermAlreadyConnected) {
                    ConnectOurDeviceAlert.dismiss();
                }
                //	(mOnOff).setImageDrawable(getResources().getDrawable(R.mipmap.open2));
                startPreview();
                isPreviewing = true;
                palette = sharedPreferences.getInt("palette", 0);
                UnitTemperature = sharedPreferences.getInt("UnitTemperature", 0);
                mUVCCameraView.setUnitTemperature(UnitTemperature);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8004);//切换数据输出8004原始8005yuv,80ff保存
                    }
                }, 300);
                mUVCCameraView.setBitmap(mCursorRed, mCursorGreen, mCursorBlue, mCursorYellow, mWatermarkLogo);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCameraHandler.changePalette(palette);
                    }
                }, 200);
//            runOnUiThread(new Runnable() {
//                              @Override
//                              public void run() {
//                                  refreshThumbnail();
//                              }
//                          }
//                    , 500);
                timerEveryTime = new Timer();
                timerEveryTime.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (isPreviewing) {
                            setValue(UVCCamera.CTRL_ZOOM_ABS, 0x8000);//每隔三分钟打一次快门
                            if (isTemperaturing) {
                                whenShutRefresh();
                            }
                            Log.e(TAG, "每隔3分钟执行一次操作");
                        }
                    }
                }, 1000, 380000);

                isWatermark = sharedPreferences.getInt("Watermark", 1);
                sharedPreferences.edit().putInt("Watermark", isWatermark).commit();
                mCameraHandler.watermarkOnOff(isWatermark);
                mUsbDevice = device;
                updateItems();
                isOpened = 1;
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            Log.e(TAG, "onDisconnect:");
            //System.exit(0);
            if (mCameraHandler != null) {
                if (isTemperaturing) {
                    mCameraHandler.stopTemperaturing();
                    isTemperaturing = false;
                }
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCameraHandler.close();
                        isPreviewing = false;
                        //mCameraHandler.stopPreview();
                    }
                }, 0);
                //setCameraButton(false);
                updateItems();
            }
            timerEveryTime.cancel();
            icon.recycle();
            iconPalette.recycle();
            //	Toast.makeText(MainActivity.this, "XthermDemo Disconnect", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Log.e(TAG, "onDettach:");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onPause();
                    onStop();
                    onDestroy();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 100);

        }

        @Override
        public void onCancel(final UsbDevice device) {
            Log.e(TAG, "onCancel:");
            System.exit(0);
        }
    };

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
        if (canceled) {
            //setCameraButton(false);
        } else {
        }
    }

    //================================================================================
    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private void whenShutRefresh() {
        if (mCameraHandler != null) {
            mCameraHandler.whenShutRefresh();
        }
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100);
    }

    /*****************计时器*******************/

    //计时器
    private Handler Timehandle = new Handler();
    private long currentSecond = 0;//当前毫秒数
    private Runnable timeRunable = new Runnable() {
        @Override
        public void run() {
//            if (isOnRecord) {
            String RecordTime = getFormatHMS(currentSecond);
            canvas = mSfh.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }

            if (isOnRecord && (canvas != null)) {

                photoPaint = new Paint(); //建立画笔
                // bitcanvas.drawBitmap(mCursorBlue, icon.getWidth() / 384.0f * minx1 - mCursorBlue.getWidth() / 2.0f, icon.getHeight() / 288.0f * miny1 - mCursorBlue.getHeight() / 2.0f, photoPaint);
                photoPaint.setStrokeWidth(4);
                photoPaint.setTextSize(40);
                photoPaint.setColor(Color.RED);
                Rect bounds = new Rect();
                photoPaint.getTextBounds(RecordTime, 0, RecordTime.length(), bounds);

                if (isT3) {
                    canvas.rotate(180, bounds.height() * 7, bounds.height());
                    canvas.drawText(RecordTime, bounds.height() * 7, bounds.height(), photoPaint);
                } else {
                    canvas.drawText(RecordTime, icon.getWidth() - bounds.height() * 7, icon.getHeight() - bounds.height(), photoPaint);
                }
            }
            if (canvas != null) {
                canvas.save();
                mSfh.unlockCanvasAndPost(canvas);
            }
            // timerText.setText(TimeUtil.getFormatHMS(currentSecond));

            currentSecond = currentSecond + 1000;
            if (currentSecond % 180000 == 0) {
                mCameraHandler.stopRecording();
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mCameraHandler.startRecording();
            }
//            }
            if (isOnRecord) {
                //递归调用本runable对象，实现每隔一秒一次执行任务
                Timehandle.postDelayed(this, 1000);
            } else {
                if (mCameraHandler != null) {
                    mCameraHandler.stopRecording();
                    currentSecond = 0;
                }
            }
        }
    };

    /**
     * 根据毫秒返回时分秒
     *
     * @param time
     * @return
     */
    public static String getFormatHMS(long time) {
        time = time / 1000;//总秒数
        int s = (int) (time % 60);//秒
        int m = (int) (time / 60);//分
        int h = (int) (time / 3600);//秒
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /*****************计时器*******************/

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
//			final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
//			mToolsLayout.setVisibility(visible_active);
//			mBrightnessButton.setVisibility(
//		    	checkSupportFlag(UVCCamera.PU_BRIGHTNESS)
//		    	? visible_active : View.INVISIBLE);
//			mContrastButton.setVisibility(
//		    	checkSupportFlag(UVCCamera.PU_CONTRAST)
//		    	? visible_active : View.INVISIBLE);
        }
    };

    private int mSettingMode = -1;

    /**
     * 設定画面を表示
     *
     * @param mode
     */
    private final void showSettings(final int mode) {
        if (DEBUG) Log.v(TAG, String.format("showSettings:%08x", mode));
        hideSetting(false);
        if (isActive()) {
            switch (mode) {
                case UVCCamera.PU_BRIGHTNESS:
                case UVCCamera.PU_CONTRAST:
                    mSettingMode = mode;
                    mSettingSeekbar.setProgress(getValue(mode));
                    ViewAnimationHelper.fadeIn(mValueLayout, -1, 0, mViewAnimationListener);
                    break;
            }
        }
    }

    private void resetSettings() {
        if (isActive()) {
            switch (mSettingMode) {
                case UVCCamera.PU_BRIGHTNESS:
                case UVCCamera.PU_CONTRAST:
                    mSettingSeekbar.setProgress(resetValue(mSettingMode));
                    break;
            }
        }
        mSettingMode = -1;
        ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
    }

    /**
     * 設定画面を非表示にする
     *
     * @param fadeOut trueならばフェードアウトさせる, falseなら即座に非表示にする
     */
    protected final void hideSetting(final boolean fadeOut) {
        removeFromUiThread(mSettingHideTask);
        if (fadeOut) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
                }
            }, 0);
        } else {
            try {
                mValueLayout.setVisibility(View.GONE);
            } catch (final Exception e) {
                // ignore
            }
            mSettingMode = -1;
        }
    }

    protected final Runnable mSettingHideTask = new Runnable() {
        @Override
        public void run() {
            hideSetting(true);
        }
    };


    private final ViewAnimationHelper.ViewAnimationListener
            mViewAnimationListener = new ViewAnimationHelper.ViewAnimationListener() {
        @Override
        public void onAnimationStart(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
        }

        @Override
        public void onAnimationEnd(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
            final int id = target.getId();
            switch (animationType) {
                case ViewAnimationHelper.ANIMATION_FADE_IN:
                case ViewAnimationHelper.ANIMATION_FADE_OUT: {
                    final boolean fadeIn = animationType == ViewAnimationHelper.ANIMATION_FADE_IN;
//                    if (id == R.id.value_layout) {
//                        if (fadeIn) {
//                            runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
//                        } else {
//                            mValueLayout.setVisibility(View.GONE);
//                            mSettingMode = -1;
//                        }
//                    } else if (!fadeIn) {
////					target.setVisibility(View.GONE);
//                    }
                    break;
                }
            }
        }

        @Override
        public void onAnimationCancel(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
        }
    };

    public static String getSystem() {
        String SYS = "";
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            if (prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                    || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                    || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null) {
                SYS = SYS_MIUI;//小米
            } else if (prop.getProperty(KEY_EMUI_API_LEVEL, null) != null
                    || prop.getProperty(KEY_EMUI_VERSION, null) != null
                    || prop.getProperty(KEY_EMUI_CONFIG_HW_SYS_VERSION, null) != null) {
                SYS = SYS_EMUI;//华为
            } else if (getMeizuFlymeOSFlag().toLowerCase().contains("flyme")) {
                SYS = SYS_FLYME;//魅族
            }
            ;
        } catch (IOException e) {
            e.printStackTrace();
            return SYS;
        }
        return SYS;
    }

    public static String getMeizuFlymeOSFlag() {
        return getSystemProperty("ro.build.display.id", "");
    }

    public void matchBrand() {
        brand = android.os.Build.BRAND;
        model = android.os.Build.MODEL;
        hardware = android.os.Build.HARDWARE;
        Log.e(TAG, "hardware:" + hardware);
        Log.e(TAG, "brand:" + brand);
        Log.e(TAG, "model:" + model);
        if (hardware.matches("qcom")) {//1
            if (true/*brand.contains("Xiaomi") || brand.contains("360") || brand.contains("Meizu")
                    || brand.contains("SMARTISAN")*//*锤子*/) {
                currentapiVersion = 1;
                //System.loadLibrary("c++QCOM");
                // System.loadLibrary("hardwareQCOM");
                //System.loadLibrary("gslQCOM");
                //System.loadLibrary("OpenCLQCOM");
                //System.loadLibrary("GLES_maliQCOM");
            } else {
                currentapiVersion = 0;
            }

        } else if (hardware.matches("mt[0-9]*")) {//2
            currentapiVersion = 0;
        } else if (hardware.matches("hi[0-9]*")) {//3
            Log.d(TAG, "his platform：" + brand);
            if (true/*brand.contains("honor") || brand.contains("Huawei") || brand.contains("HUAWEI")||brand.contains("Honor")*/) {
                currentapiVersion = 3;
                // System.loadLibrary("c++HW");
                //System.loadLibrary("hardwareHW");
                // System.loadLibrary("GLES_maliHW");
                // System.loadLibrary("OpenCLHW");
            } else {
                currentapiVersion = 0;
            }

        }
        if (hardware.contains("cht")) {//x86
            Log.d(TAG, "intel Cherry Trail");
            currentapiVersion = 4;
        }

    }

    @Nullable
    private Bitmap getThumbnail() {
        if (checkPermissionWriteExternalStorage()) {
            Uri uri = MediaStore.Files.getContentUri("external");
            String[] PROJECTION = {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    "duration"};
            String SELECTION_ALL =
                    "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                            + " OR "
                            + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                            + " AND " + "bucket_display_name" + "='XthermDemo'"
                            + " AND " + MediaStore.MediaColumns.SIZE + ">0";
//        String[] SELECTION_ALL_ARGS = {
//                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
//                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
//        };
            String[] SELECTION_ALL_ARGS = {
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                    String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
            };
            String ORDER_BY = MediaStore.Images.Media.DATE_TAKEN + " DESC";
            ;
            Cursor c = this.getContentResolver().query(uri, PROJECTION,
                    SELECTION_ALL, SELECTION_ALL_ARGS, ORDER_BY);
            if (c == null || c.getCount() == 0) {
                return null;
            }
            c.moveToFirst();
            String Id = c.getString(c.getColumnIndex(MediaStore.Files.FileColumns._ID));
            c.close();
            if (Id == null) {
                return null;
            }
            long IdLong = Long.parseLong(Id);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap a = null;
            a = MediaStore.Images.Thumbnails.getThumbnail(this.getContentResolver(), IdLong, MediaStore.Images.Thumbnails.MINI_KIND, options);
            if (a == null) {
                a = MediaStore.Video.Thumbnails.getThumbnail(this.getContentResolver(), IdLong, MediaStore.Images.Thumbnails.MINI_KIND, options);
            }
            return a;
        } else {
            return null;
        }
    }

    /*private void refreshThumbnail() {
        Bitmap thumbnail = getThumbnail();
        int width = mThumbnailButton.getWidth();
        Log.e(TAG, "mThumbnailButton.getWidth():" + width);
        if (thumbnail != null) {
            Bitmap circleBitmap = BitmapUtil.createCircleImage(thumbnail, mCaptureButton.getWidth(), mCaptureButton.getHeight(), mCaptureButton.getHeight() / 6);
            Log.e(TAG, "mCaptureButton.getWidth():" + mCaptureButton.getWidth() + "mCaptureButton.getHeight():" + mCaptureButton.getHeight());
            //mThumbnailButton.setImageBitmap(circleBitmap);
            Bitmap target = Bitmap.createBitmap(270, 360, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(target);
            canvas.drawColor(Color.RED);
            mThumbnailButton.setImageBitmap(circleBitmap);
            mThumbnailButton.setAdjustViewBounds(true);
            mThumbnailButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Log.e(TAG, "mThumbnailButton.getWidth():" + mThumbnailButton.getWidth() + "mThumbnailButton.getHeight():" + mThumbnailButton.getHeight());
        }
    }*/

    private static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            return (String) get.invoke(clz, key, defaultValue);
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public class ScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            //	float scale = getScale();
            float scaleFactor = detector.getScaleFactor();
            mFinalScale = mFinalScale * scaleFactor;
            if (mFinalScale > 4) {
                mFinalScale = 4;
            } else if (mFinalScale < 1) {
                mFinalScale = 1;
            }
            mUVCCameraView.setVertices(mFinalScale);
            return false;

        }


        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // TODO Auto-generated method stub
            //一定要返回true才会进入onScale()这个函数
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // TODO Auto-generated method stub
        }
    }

    public class sendCommand {
        int psitionAndValue0 = 0, psitionAndValue1 = 0, psitionAndValue2 = 0, psitionAndValue3 = 0;

        public void sendFloatCommand(int position, byte value0, byte value1, byte value2, byte value3, int interval0, int interval1, int interval2, int interval3, int interval4) {
            psitionAndValue0 = (position << 8) | (0x000000ff & value0);
            Handler handler0 = new Handler();
            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
                }
            }, interval0);

            psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1);
                }
            }, interval1);
            psitionAndValue2 = ((position + 2) << 8) | (0x000000ff & value2);

            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue2);
                }
            }, interval2);

            psitionAndValue3 = ((position + 3) << 8) | (0x000000ff & value3);

            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue3);
                }
            }, interval3);

            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.whenShutRefresh();
                }
            }, interval4);


        }

        public void sendShortCommand(int position, byte value0, byte value1, int interval0, int interval1, int interval2) {
            psitionAndValue0 = (position << 8) | (0x000000ff & value0);
            Handler handler0 = new Handler();
            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
                }
            }, interval0);

            psitionAndValue1 = ((position + 1) << 8) | (0x000000ff & value1);
            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue1);
                }
            }, interval1);

            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.whenShutRefresh();
                }
            }, interval2);

        }

        private void whenChangeTempPara() {
            if (mCameraHandler != null) {
                mCameraHandler.whenChangeTempPara();
            }
        }

        public void sendByteCommand(int position, byte value0, int interval0) {
            psitionAndValue0 = (position << 8) | (0x000000ff & value0);
            Handler handler0 = new Handler();
            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.setValue(UVCCamera.CTRL_ZOOM_ABS, psitionAndValue0);
                }
            }, interval0);
            handler0.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCameraHandler.whenShutRefresh();
                }
            }, interval0 + 20);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            //mAdapter.setData(Matisse.obtainResult(data), Matisse.obtainPathResult(data));
            Log.e("OnActivityResult ", String.valueOf(Matisse.obtainOriginalState(data)));
        }
    }


    private static class UriAdapter extends RecyclerView.Adapter<UriAdapter.UriViewHolder> {

        private List<Uri> mUris;
        private List<String> mPaths;

        void setData(List<Uri> uris, List<String> paths) {
            mUris = uris;
            mPaths = paths;
            notifyDataSetChanged();
        }

        @Override
        public UriViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new UriViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.uri_item, parent, false));
        }

        @Override
        public void onBindViewHolder(UriViewHolder holder, int position) {
            holder.mUri.setText(mUris.get(position).toString());
            holder.mPath.setText(mPaths.get(position));

            holder.mUri.setAlpha(position % 2 == 0 ? 1.0f : 0.54f);
            holder.mPath.setAlpha(position % 2 == 0 ? 1.0f : 0.54f);
        }

        @Override
        public int getItemCount() {
            return mUris == null ? 0 : mUris.size();
        }

        static class UriViewHolder extends RecyclerView.ViewHolder {

            private TextView mUri;
            private TextView mPath;

            UriViewHolder(View contentView) {
                super(contentView);
                mUri = (TextView) contentView.findViewById(R.id.uri);
                mPath = (TextView) contentView.findViewById(R.id.path);
            }
        }
    }

    public static synchronized String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
