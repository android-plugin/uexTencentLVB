package org.zywx.wbpalmstar.plugin.tencentlvb.util;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.tencentlvb.EUExTencentLVB;

import java.util.ArrayList;

public class LivePublisherActivity extends RTMPBaseActivity implements View.OnClickListener, ITXLivePushListener, SeekBar.OnSeekBarChangeListener/*, ImageReader.OnImageAvailableListener*/ {
    private static final String TAG = LivePublisherActivity.class.getSimpleName();

    private TXLivePushConfig mLivePushConfig;
    private TXLivePusher mLivePusher;
    private TXCloudVideoView mCaptureView;

    private LinearLayout mBitrateLayout;
    private LinearLayout mFaceBeautyLayout;
    private SeekBar mBeautySeekBar;
    private SeekBar mWhiteningSeekBar;
    private SeekBar mExposureSeekBar;
    private ScrollView mScrollView;
    private RadioGroup mRadioGroupBitrate;
    private Button mBtnBitrate;
    private Button mBtnPlay;
    private Button mBtnFaceBeauty;
    private Button mBtnFlashLight;
    private Button mBtnTouchFocus;
    private Button mBtnHWEncode;
    private Button mBtnOrientation;
    private boolean mPortrait = true;//手动切换，横竖屏推流

    private boolean mVideoPublish;
    private boolean mFrontCamera = true;
    private boolean mHWVideoEncode = false;
    private boolean mFlashTurnOn = false;
    private boolean mTouchFocus = true;
    private boolean mHWListConfirmDialogResult = false;
    private int mBeautyLevel = 5;
    private int mWhiteningLevel = 3;

    private Handler mHandler = new Handler();

    private Bitmap mBitmap;
    private int mFilterType = FILTERTYPE_NONE; //滤镜类型
    /**
     * 滤镜定义
     */
    public static final int FILTERTYPE_NONE         = 0;    //无特效滤镜
    public static final int FILTERTYPE_langman      = 1;    //浪漫滤镜
    public static final int FILTERTYPE_qingxin      = 2;    //清新滤镜
    public static final int FILTERTYPE_weimei       = 3;    //唯美滤镜
    public static final int FILTERTYPE_fennen 		= 4;    //粉嫩滤镜
    public static final int FILTERTYPE_huaijiu 		= 5;    //怀旧滤镜
    public static final int FILTERTYPE_landiao 		= 6;    //蓝调滤镜
    public static final int FILTERTYPE_qingliang 	= 7;    //清凉滤镜
    public static final int FILTERTYPE_rixi 		= 8;    //日系滤镜
    // 关注系统设置项“自动旋转”的状态切换
    private RotationObserver mRotationObserver = null;
    private String rtmpUrl;
    ArrayList<String> mFilterList;
    TXHorizontalPickerView mFilterPicker;
    ArrayAdapter<String> mFilterAdapter;

    private Bitmap decodeResource(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.openRawResource(id, value);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inTargetDensity = value.density;
        return BitmapFactory.decodeResource(resources, id, opts);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLivePusher = new TXLivePusher(getActivity());
        mLivePushConfig = new TXLivePushConfig();
        mLivePusher.setConfig(mLivePushConfig);
        mBitmap = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_watermark"));
        rtmpUrl = getArguments().getString(EUExTencentLVB.TEXT_URL);

        mRotationObserver = new RotationObserver(new Handler());
        mRotationObserver.startObserver();

        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    final PhoneStateListener listener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch(state){
                //电话等待接听
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mLivePusher != null) mLivePusher.pausePusher();
                    break;
                //电话接听
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (mLivePusher != null) mLivePusher.pausePusher();
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mLivePusher != null) mLivePusher.resumePusher();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(EUExUtil.getResLayoutID("plugin_uextencentlvb_activity_publish"), null);

        initView(view);
        mCaptureView = (TXCloudVideoView) view.findViewById(EUExUtil.getResIdID("video_view"));
        mCaptureView.disableLog(true);

        mVideoPublish = false;
        mLogViewStatus.setVisibility(View.GONE);
        mLogViewStatus.setMovementMethod(new ScrollingMovementMethod());
        mLogViewEvent.setMovementMethod(new ScrollingMovementMethod());
        mScrollView = (ScrollView) view.findViewById(EUExUtil.getResIdID("scrollview"));
        mScrollView.setVisibility(View.GONE);

        //美颜部分
        mFaceBeautyLayout = (LinearLayout) view.findViewById(EUExUtil.getResIdID("layoutFaceBeauty"));
        mBeautySeekBar = (SeekBar) view.findViewById(EUExUtil.getResIdID("beauty_seekbar"));
        mBeautySeekBar.setOnSeekBarChangeListener(this);

        mExposureSeekBar = (SeekBar) view.findViewById(EUExUtil.getResIdID("exposure_seekbar"));
        mExposureSeekBar.setOnSeekBarChangeListener(this);
        mWhiteningSeekBar = (SeekBar) view.findViewById(EUExUtil.getResIdID("whitening_seekbar"));
        mWhiteningSeekBar.setOnSeekBarChangeListener(this);

        mBtnFaceBeauty = (Button) view.findViewById(EUExUtil.getResIdID("btnFaceBeauty"));
        mBtnFaceBeauty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFaceBeautyLayout.setVisibility(mFaceBeautyLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        mFilterList = new ArrayList<String>();
        mFilterList.add("无滤镜");
        mFilterList.add("浪漫滤镜");
        mFilterList.add("清新滤镜");
        mFilterList.add("唯美滤镜");
        mFilterList.add("粉嫩滤镜");
        mFilterList.add("怀旧滤镜");
        mFilterList.add("蓝调滤镜");
        mFilterList.add("清凉滤镜");
        mFilterList.add("日系滤镜");

        mFilterPicker = (TXHorizontalPickerView) view.findViewById(EUExUtil.getResIdID("filterPicker"));
        mFilterAdapter = new ArrayAdapter<String>(view.getContext(),0,mFilterList){

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                String value = getItem(position);
                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(android.R.layout.simple_list_item_1,null);
                }
                TextView view = (TextView) convertView.findViewById(android.R.id.text1);
                view.setTag(position);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                view.setText(value);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int index = (int) view.getTag();
                        ViewGroup group = (ViewGroup)mFilterPicker.getChildAt(0);
                        for (int i = 0; i < mFilterAdapter.getCount(); i++) {
                            View v = group.getChildAt(i);
                            if (v instanceof TextView) {
                                if (i == index) {
                                    ((TextView) v).setTextColor(Color.GRAY);
                                } else {
                                    ((TextView) v).setTextColor(Color.BLACK);
                                }
                            }
                        }
                        setFilter(index);
                    }
                });
                return convertView;

            }
        };
        mFilterPicker.setAdapter(mFilterAdapter);
        mFilterPicker.setClicked(0);


        //播放部分
        mBtnPlay = (Button) view.findViewById(EUExUtil.getResIdID("btnPlay"));
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mVideoPublish) {
                    stopPublishRtmp();
                } else {
                    FixOrAdjustBitrate();  //根据设置确定是“固定”还是“自动”码率
                    mVideoPublish = startPublishRtmp();
                }
            }
        });


        //log部分
        final Button btnLog = (Button) view.findViewById(EUExUtil.getResIdID("btnLog"));
        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLogViewStatus.getVisibility() == View.GONE) {
                    mLogViewStatus.setVisibility(View.VISIBLE);
                    mScrollView.setVisibility(View.VISIBLE);
                    mLogViewEvent.setText(mLogMsg);
                    scroll2Bottom(mScrollView, mLogViewEvent);
                    btnLog.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_log_hidden"));
                } else {
                    mLogViewStatus.setVisibility(View.GONE);
                    mScrollView.setVisibility(View.GONE);
                    btnLog.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_log_show"));
                }
            }
        });
        boolean isShowLog = getArguments().getBoolean(EUExTencentLVB.BOOLEAN_IS_SHOW_LOG);
        btnLog.setVisibility(isShowLog ? View.VISIBLE : View.GONE);
        TextView btnLogDivider = (TextView) view.findViewById(EUExUtil.getResIdID("btnLogDivider"));
        btnLogDivider.setVisibility(isShowLog ? View.VISIBLE : View.GONE);


        //切换前置后置摄像头
        final Button btnChangeCam = (Button) view.findViewById(EUExUtil.getResIdID("btnCameraChange"));
        btnChangeCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFrontCamera = !mFrontCamera;

                if (mLivePusher.isPushing()) {
                    mLivePusher.switchCamera();
                } else {
                    mLivePushConfig.setFrontCamera(mFrontCamera);
                }
                int resId = mFrontCamera ? EUExUtil.getResDrawableID("plugin_uextencentlvb_camera_change") : EUExUtil.getResDrawableID("plugin_uextencentlvb_camera_change2");
                btnChangeCam.setBackgroundResource(resId);
            }
        });

        //开启硬件加速
        mBtnHWEncode = (Button) view.findViewById(EUExUtil.getResIdID("btnHWEncode"));
        mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
        mBtnHWEncode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean HWVideoEncode = mHWVideoEncode;
                mHWVideoEncode = !mHWVideoEncode;
                mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);

                if (mHWVideoEncode) {
                    if (mLivePushConfig != null) {
                        if(Build.VERSION.SDK_INT < 18){
                            Toast.makeText(getActivity().getApplicationContext(), "硬件加速失败，当前手机API级别过低（最低18）", Toast.LENGTH_SHORT).show();
                            mHWVideoEncode = false;
                        }
                    }
                }
                if (HWVideoEncode != mHWVideoEncode) {
                    mLivePushConfig.setHardwareAcceleration(mHWVideoEncode);
                    if (mHWVideoEncode == false) {
                        Toast.makeText(getActivity().getApplicationContext(), "取消硬件加速", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), "开启硬件加速", Toast.LENGTH_SHORT).show();
                    }
                }
                if (mLivePusher != null) {
                    mLivePusher.setConfig(mLivePushConfig);
                }
            }
        });

        //码率自适应部分
        mBtnBitrate = (Button) view.findViewById(EUExUtil.getResIdID("btnBitrate"));
        mBitrateLayout = (LinearLayout) view.findViewById(EUExUtil.getResIdID("layoutBitrate"));
        mBtnBitrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBitrateLayout.setVisibility(mBitrateLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        mRadioGroupBitrate = (RadioGroup) view.findViewById(EUExUtil.getResIdID("resolutionRadioGroup"));
        mRadioGroupBitrate.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                FixOrAdjustBitrate();
                mBitrateLayout.setVisibility(View.GONE);
            }
        });

        //闪光灯
        mBtnFlashLight = (Button) view.findViewById(EUExUtil.getResIdID("btnFlash"));
        mBtnFlashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLivePusher == null) {
                    return;
                }

                mFlashTurnOn = !mFlashTurnOn;
                if (!mLivePusher.turnOnFlashLight(mFlashTurnOn)) {
                    Toast.makeText(getActivity().getApplicationContext(),
                            "打开闪光灯失败（1）大部分前置摄像头并不支持闪光灯（2）该接口需要在启动预览之后调用", Toast.LENGTH_SHORT).show();
                }

                mBtnFlashLight.setBackgroundResource(mFlashTurnOn ? EUExUtil.getResDrawableID("plugin_uextencentlvb_flash_off") : EUExUtil.getResDrawableID("plugin_uextencentlvb_flash_on"));
            }
        });

        //手动对焦/自动对焦
        mBtnTouchFocus = (Button) view.findViewById(EUExUtil.getResIdID("btnTouchFocus"));
        mBtnTouchFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFrontCamera) {
                    return;
                }

                mTouchFocus = !mTouchFocus;
                mLivePushConfig.setTouchFocus(mTouchFocus);
                v.setBackgroundResource(mTouchFocus ? EUExUtil.getResDrawableID("plugin_uextencentlvb_automatic") :EUExUtil.getResDrawableID("plugin_uextencentlvb_manual"));

                if (mLivePusher.isPushing()) {
                    mLivePusher.stopCameraPreview(false);
                    mLivePusher.startCameraPreview(mCaptureView);
                }

                Toast.makeText(getActivity(), mTouchFocus ? "已开启手动对焦" : "已开启自动对焦", Toast.LENGTH_SHORT).show();
            }
        });

        //锁定Activity不旋转的情况下，才能进行横屏|竖屏推流切换
        mBtnOrientation = (Button) view.findViewById(EUExUtil.getResIdID("btnPushOrientation"));
        if (isActivityCanRotation()) {
            mBtnOrientation.setVisibility(View.GONE);
        }
        mBtnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPortrait = ! mPortrait;
                int renderRotation = 0;
                if (mPortrait) {
                    mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
                    mBtnOrientation.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_landscape"));
                    renderRotation = 0;
                } else {
                    mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT);
                    mBtnOrientation.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_portrait"));
                    renderRotation = 270;
                }
                mLivePusher.setRenderRotation(renderRotation);
                mLivePusher.setConfig(mLivePushConfig);
            }
        });

        view.setOnClickListener(this);
        mLogViewStatus.setText("Log File Path:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/txRtmpLog");
        return view;
    }

    protected void setFilter(int filterType) {
        mFilterType = filterType;
        Bitmap bmp = null;
        switch (filterType) {
            case FILTERTYPE_langman:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_langman"));
                break;
            case FILTERTYPE_qingxin:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_qingxin"));
                break;
            case FILTERTYPE_weimei:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_weimei"));
                break;
            case FILTERTYPE_fennen:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_fennen"));
                break;
            case FILTERTYPE_huaijiu:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_huaijiu"));
                break;
            case FILTERTYPE_landiao:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_landiao"));
                break;
            case FILTERTYPE_qingliang:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_qingliang"));
                break;
            case FILTERTYPE_rixi:
                bmp = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_filter_rixi"));
                break;
            default:
                bmp = null;
                break;
        }
        if (mLivePusher != null) {
            mLivePusher.setFilter(bmp);
        }
    }

    protected void HWListConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LivePublisherActivity.this.getActivity());
        builder.setMessage("警告：当前机型不在白名单中,是否继续尝试硬编码？");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mHWListConfirmDialogResult = true;
                throw new RuntimeException();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mHWListConfirmDialogResult = false;
                throw new RuntimeException();
            }
        });
        builder.setCancelable(false);
        builder.create().show();
        try {
            Looper.loop();
        } catch (Exception e) {
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                mFaceBeautyLayout.setVisibility(View.GONE);
                mBitrateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCaptureView != null) {
            mCaptureView.onResume();
        }

        if (mVideoPublish && mLivePusher != null) {
            mLivePusher.resumePusher();
            mLivePusher.startCameraPreview(mCaptureView);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mCaptureView != null) {
            mCaptureView.onPause();
        }

        if (mVideoPublish && mLivePusher != null) {
            mLivePusher.stopCameraPreview(false);
            mLivePusher.pausePusher();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPublishRtmp();
        if (mCaptureView != null) {
            mCaptureView.onDestroy();
        }
        mRotationObserver.stopObserver();
    }

    private boolean startPublishRtmp() {
        if (TextUtils.isEmpty(rtmpUrl) || (!rtmpUrl.trim().toLowerCase().startsWith("rtmp://"))) {
            Toast.makeText(getActivity().getApplicationContext(), "推流地址不合法，目前支持rtmp推流!", Toast.LENGTH_SHORT).show();
            return false;
        }

        mCaptureView.setVisibility(View.VISIBLE);
        mLivePushConfig.setWatermark(mBitmap, 10, 10);

        int customModeType = 0;

        //【示例代码1】设置自定义视频采集逻辑 （自定义视频采集逻辑不要调用startPreview）
//        customModeType |= TXLiveConstants.CUSTOM_MODE_VIDEO_CAPTURE;
//        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_1280_720);
//        mLivePushConfig.setAutoAdjustBitrate(false);
//        mLivePushConfig.setVideoBitrate(1300);
//        mLivePushConfig.setVideoFPS(25);
//        mLivePushConfig.setVideoEncodeGop(3);
//        new Thread() {  //视频采集线程
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        FileInputStream in = new FileInputStream("/sdcard/dump_1280_720.yuv");
//                        int len = 1280 * 720 * 3 / 2;
//                        byte buffer[] = new byte[len];
//                        int count;
//                        while ((count = in.read(buffer)) != -1) {
//                            if (len == count) {
//                                mLivePusher.sendCustomVideoData(buffer, TXLivePusher.YUV_420SP);
//                            } else {
//                                break;
//                            }
//                            sleep(50, 0);
//                        }
//                        in.close();
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();

        //【示例代码2】设置自定义音频采集逻辑（音频采样位宽必须是16）
//        mLivePushConfig.setAudioSampleRate(44100);
//        mLivePushConfig.setAudioChannels(1);
//        customModeType |= TXLiveConstants.CUSTOM_MODE_AUDIO_CAPTURE;
//        new Thread() {  //音频采集线程
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        FileInputStream in = new FileInputStream("/sdcard/dump.pcm");
//                        int len = 2048;
//                        byte buffer[] = new byte[len];
//                        int count;
//                        while ((count = in.read(buffer)) != -1) {
//                            if (len == count) {
//                                mLivePusher.sendCustomPCMData(buffer);
//                            } else {
//                                break;
//                            }
//                            sleep(10, 0);
//                        }
//                        in.close();
//                    } catch (Exception e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();

        //【示例代码3】设置自定义视频预处理逻辑
//        customModeType |= TXLiveConstants.CUSTOM_MODE_VIDEO_PREPROCESS;
//        String path = this.getActivity().getApplicationInfo().dataDir + "/lib";
//        mLivePushConfig.setCustomVideoPreProcessLibrary(path +"/libvideo.so", "tx_video_process");

        //【示例代码4】设置自定义音频预处理逻辑
//        customModeType |= TXLiveConstants.CUSTOM_MODE_AUDIO_PREPROCESS;
//        String path = this.getActivity().getApplicationInfo().dataDir + "/lib";
//        mLivePushConfig.setCustomAudioPreProcessLibrary(path +"/libvideo.so", "tx_audio_process");


        mLivePushConfig.setCustomModeType(customModeType);

        mLivePushConfig.setPauseImg(300, 10);
        Bitmap bitmap = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_pause_publish"));

        mLivePushConfig.setPauseImg(bitmap);
        mLivePushConfig.setPauseFlag(TXLiveConstants.PAUSE_FLAG_PAUSE_VIDEO | TXLiveConstants.PAUSE_FLAG_PAUSE_AUDIO);

        mLivePushConfig.setBeautyFilter(mBeautyLevel, mWhiteningLevel);
        mLivePusher.setConfig(mLivePushConfig);
        mLivePusher.setPushListener(this);
        mLivePusher.startCameraPreview(mCaptureView);
//        mLivePusher.startScreenCapture();
        mLivePusher.startPusher(rtmpUrl.trim());
        //mLivePusher.setLogLevel(TXLiveConstants.LOG_LEVEL_DEBUG);

        clearLog();
        int[] ver = TXLivePusher.getSDKVersion();
        if (ver != null && ver.length >= 3) {
            mLogMsg.append(String.format("rtmp sdk version:%d.%d.%d ", ver[0], ver[1], ver[2]));
            mLogViewEvent.setText(mLogMsg);
        }

        mBtnPlay.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_play_pause"));

        appendEventLog(0, "点击推流按钮！");

        return true;
    }

    private void stopPublishRtmp() {
        mVideoPublish = false;
        mLivePusher.stopCameraPreview(true);
        mLivePusher.stopScreenCapture();
        mLivePusher.setPushListener(null);
        mLivePusher.stopPusher();
        mCaptureView.setVisibility(View.GONE);

        if(mBtnHWEncode != null) {
            //mHWVideoEncode = true;
            mLivePushConfig.setHardwareAcceleration(mHWVideoEncode);
            mBtnHWEncode.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_quick"));
            mBtnHWEncode.getBackground().setAlpha(mHWVideoEncode ? 255 : 100);
        }
        mBtnPlay.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_play_start"));

        if (mLivePushConfig != null) {
            mLivePushConfig.setPauseImg(null);
        }
    }


    public void FixOrAdjustBitrate() {
        if (mRadioGroupBitrate == null || mLivePushConfig == null || mLivePusher == null) {
            return;
        }

        RadioButton rb = (RadioButton) getActivity().findViewById(mRadioGroupBitrate.getCheckedRadioButtonId());
        int mode = Integer.parseInt((String) rb.getTag());

        switch (mode) {
            case 4: /*720p*/
                if (mLivePusher != null) {
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_720_1280);
//                    mLivePushConfig.setAutoAdjustBitrate(false);
//                    mLivePushConfig.setVideoBitrate(1500);
//                    mLivePusher.setConfig(mLivePushConfig);
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_SUPER_DEFINITION);
                    //超清默认开启硬件加速
                    if (Build.VERSION.SDK_INT >= 18) {
                        mHWVideoEncode = true;
                    }
                    mBtnHWEncode.getBackground().setAlpha(255);
                }
                mBtnBitrate.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_fix_bitrate"));
                break;
            case 3: /*540p*/
                if (mLivePusher != null) {
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960);
//                    mLivePushConfig.setAutoAdjustBitrate(false);
//                    mLivePushConfig.setVideoBitrate(1000);
//                    mLivePusher.setConfig(mLivePushConfig);
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_HIGH_DEFINITION);
                    mHWVideoEncode = false;
                    mBtnHWEncode.getBackground().setAlpha(100);
                }
                mBtnBitrate.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_fix_bitrate"));
                break;
            case 2: /*360p*/
                if (mLivePusher != null) {
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_STANDARD_DEFINITION);
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
                    //标清默认开启了码率自适应，需要关闭码率自适应
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(700);
                    mLivePusher.setConfig(mLivePushConfig);
                    //标清默认关闭硬件加速
                    mHWVideoEncode = false;
                    mBtnHWEncode.getBackground().setAlpha(100);
                }
                mBtnBitrate.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_fix_bitrate"));
                break;

            case 1: /*自动*/
                if (mLivePusher != null) {
//                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
//                    mLivePushConfig.setAutoAdjustBitrate(true);
//                    mLivePushConfig.setAutoAdjustStrategy(TXLiveConstants.AUTO_ADJUST_BITRATE_STRATEGY_1);
//                    mLivePushConfig.setMaxVideoBitrate(1000);
//                    mLivePushConfig.setMinVideoBitrate(400);
//                    mLivePushConfig.setVideoBitrate(700);
//                    mLivePusher.setConfig(mLivePushConfig);
                    mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_STANDARD_DEFINITION);
                    //标清默认关闭硬件加速
                    mHWVideoEncode = false;
                    mBtnHWEncode.getBackground().setAlpha(100);
                }
                mBtnBitrate.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_auto_bitrate"));
                break;
            default:
                break;
        }
    }

    @Override
    public void onPushEvent(int event, Bundle param) {
        String msg = param.getString(TXLiveConstants.EVT_DESCRIPTION);
        appendEventLog(event, msg);
        if (mScrollView.getVisibility() == View.VISIBLE) {
            mLogViewEvent.setText(mLogMsg);
            scroll2Bottom(mScrollView, mLogViewEvent);
        }
//        if (mLivePusher != null) {
//            mLivePusher.onLogRecord("[event:" + event + "]" + msg + "\n");
//        }
        //错误还是要明确的报一下
        if (event < 0) {
            Toast.makeText(getActivity().getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            if(event == TXLiveConstants.PUSH_ERR_OPEN_CAMERA_FAIL){
                stopPublishRtmp();
            }
        }

        if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {
            stopPublishRtmp();
        }
        else if (event == TXLiveConstants.PUSH_WARNING_HW_ACCELERATION_FAIL) {
            Toast.makeText(getActivity().getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            mLivePushConfig.setHardwareAcceleration(false);
            mBtnHWEncode.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_quick2"));
            mLivePusher.setConfig(mLivePushConfig);
            mHWVideoEncode = false;
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_UNSURPORT) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_START_FAILED) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_RESOLUTION) {
            Log.d(TAG, "change resolution to " + param.getInt(TXLiveConstants.EVT_PARAM2) + ", bitrate to" + param.getInt(TXLiveConstants.EVT_PARAM1));
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_BITRATE) {
            Log.d(TAG, "change bitrate to" + param.getInt(TXLiveConstants.EVT_PARAM1));
        }
    }

    @Override
    public void onNetStatus(Bundle status) {
        String str = getNetStatusString(status);
        mLogViewStatus.setText(str);
        Log.d(TAG, "Current status, CPU:"+status.getString(TXLiveConstants.NET_STATUS_CPU_USAGE)+
                ", RES:"+status.getInt(TXLiveConstants.NET_STATUS_VIDEO_WIDTH)+"*"+status.getInt(TXLiveConstants.NET_STATUS_VIDEO_HEIGHT)+
                ", SPD:"+status.getInt(TXLiveConstants.NET_STATUS_NET_SPEED)+"Kbps"+
                ", FPS:"+status.getInt(TXLiveConstants.NET_STATUS_VIDEO_FPS)+
                ", ARA:"+status.getInt(TXLiveConstants.NET_STATUS_AUDIO_BITRATE)+"Kbps"+
                ", VRA:"+status.getInt(TXLiveConstants.NET_STATUS_VIDEO_BITRATE)+"Kbps");
//        if (mLivePusher != null){
//            mLivePusher.onLogRecord("[net state]:\n"+str+"\n");
//        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == EUExUtil.getResIdID("exposure_seekbar")) {
            if (mLivePusher != null) {
                mLivePusher.setExposureCompensation(((float)progress - 10.0f)/10.0f);
            }
            return;
        } else if (seekBar.getId() == EUExUtil.getResIdID("beauty_seekbar")) {
            mBeautyLevel = progress;
        } else if (seekBar.getId() == EUExUtil.getResIdID("whitening_seekbar")) {
            mWhiteningLevel = progress;
        }

        if (mLivePusher != null) {
            if (!mLivePusher.setBeautyFilter(mBeautyLevel, mWhiteningLevel)) {
                Toast.makeText(getActivity().getApplicationContext(), "当前机型的性能无法支持美颜功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        onActivityRotation();
    }

    protected void onActivityRotation()
    {
        // 自动旋转打开，Activity随手机方向旋转之后，需要改变推流方向
        int mobileRotation = this.getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
        switch (mobileRotation) {
            case Surface.ROTATION_0:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_DOWN;
                break;
            case Surface.ROTATION_90:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_RIGHT;
                break;
            case Surface.ROTATION_270:
                pushRotation = TXLiveConstants.VIDEO_ANGLE_HOME_LEFT;
                break;
            default:
                break;
        }
        mLivePusher.setRenderRotation(0); //因为activity也旋转了，本地渲染相对正方向的角度为0。
        mLivePushConfig.setHomeOrientation(pushRotation);
        mLivePusher.setConfig(mLivePushConfig);
    }

    /**
     * 判断Activity是否可旋转。只有在满足以下条件的时候，Activity才是可根据重力感应自动旋转的。
     * 系统“自动旋转”设置项打开；
     * @return false---Activity可根据重力感应自动旋转
     */
    protected boolean isActivityCanRotation()
    {
        // 判断自动旋转是否打开
        int flag = Settings.System.getInt(this.getActivity().getContentResolver(),Settings.System.ACCELEROMETER_ROTATION, 0);
        if (flag == 0) {
            return false;
        }
        return true;
    }

    //观察屏幕旋转设置变化，类似于注册动态广播监听变化机制
    private class RotationObserver extends ContentObserver
    {
        ContentResolver mResolver;

        public RotationObserver(Handler handler)
        {
            super(handler);
            mResolver = LivePublisherActivity.this.getActivity().getContentResolver();
        }

        //屏幕旋转设置改变时调用
        @Override
        public void onChange(boolean selfChange)
        {
            super.onChange(selfChange);
            //更新按钮状态
            if (isActivityCanRotation()) {
                mBtnOrientation.setVisibility(View.GONE);
                onActivityRotation();
            } else {
                mBtnOrientation.setVisibility(View.VISIBLE);
                mPortrait = true;
                mLivePushConfig.setHomeOrientation(TXLiveConstants.VIDEO_ANGLE_HOME_DOWN);
                mBtnOrientation.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_landscape"));
                mLivePusher.setRenderRotation(0);
                mLivePusher.setConfig(mLivePushConfig);
            }

        }

        public void startObserver()
        {
            mResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, this);
        }

        public void stopObserver()
        {
            mResolver.unregisterContentObserver(this);
        }
    }
}