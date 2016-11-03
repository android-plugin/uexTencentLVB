package org.zywx.wbpalmstar.plugin.tencentlvb.util;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;
import com.tencent.rtmp.ui.TXCloudVideoView;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.tencentlvb.EUExTencentLVB;

public class LivePublisherActivity extends RTMPBaseActivity implements View.OnClickListener, ITXLivePushListener, SeekBar.OnSeekBarChangeListener/*, ImageReader.OnImageAvailableListener*/ {
    private static final String TAG = LivePublisherActivity.class.getSimpleName();

    private TXLivePushConfig mLivePushConfig;
    private TXLivePusher mLivePusher;
    private TXCloudVideoView mCaptureView;

    private LinearLayout mBitrateLayout;
    private LinearLayout mFaceBeautyLayout;
    private SeekBar mBeautySeekBar;
    private SeekBar mWhiteningSeekBar;
    private ScrollView mScrollView;
    private RadioGroup mRadioGroupBitrate;
    private Button mBtnBitrate;
    private Button mBtnPlay;
    private Button mBtnFaceBeauty;
    private Button mBtnFlashLight;
    private Button mBtnTouchFocus;
    private Button mBtnHWEncode;

    private boolean mVideoPublish;
    private boolean mFrontCamera = true;
    private boolean mHWVideoEncode = false;
    private boolean mFlashTurnOn = false;
    private boolean mTouchFocus = true;
    private boolean mHWListConfirmDialogResult = false;
    private int mBeautyLevel = 0;
    private int mWhiteningLevel = 0;
    private Bitmap mBitmap;
    private String rtmpUrl;


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
        mBitmap = decodeResource(getResources(), EUExUtil.getResDrawableID("plugin_uextencentlvb_watermark"));
        rtmpUrl = getArguments().getString(EUExTencentLVB.TEXT_URL);


    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(EUExUtil.getResLayoutID("plugin_uextencentlvb_activity_publish"), null);

        initView(view);
        mCaptureView = (TXCloudVideoView) view.findViewById(EUExUtil.getResIdID("video_view"));

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

        mWhiteningSeekBar = (SeekBar) view.findViewById(EUExUtil.getResIdID("whitening_seekbar"));
        mWhiteningSeekBar.setOnSeekBarChangeListener(this);

        mBtnFaceBeauty = (Button) view.findViewById(EUExUtil.getResIdID("btnFaceBeauty"));
        mBtnFaceBeauty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFaceBeautyLayout.setVisibility(mFaceBeautyLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        //播放部分
        mBtnPlay = (Button) view.findViewById(EUExUtil.getResIdID("btnPlay"));
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mVideoPublish) {
                    stopPublishRtmp();
                    mVideoPublish = false;
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
                        if (Build.VERSION.SDK_INT < 18) {
                            Toast.makeText(getActivity().getApplicationContext(), "硬件加速失败，当前手机API级别过低（最低16）", Toast.LENGTH_SHORT).show();
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
        mBtnTouchFocus = (Button) view.findViewById(EUExUtil.getResIdID("btnTouchFoucs"));
        mBtnTouchFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFrontCamera) {
                    return;
                }

                mTouchFocus = !mTouchFocus;
                mLivePushConfig.setTouchFocus(mTouchFocus);
                v.setBackgroundResource(mTouchFocus ? EUExUtil.getResDrawableID("plugin_uextencentlvb_automatic") :EUExUtil.getResDrawableID("drawable.plugin_uextencentlvb_manual"));

                if (mLivePusher.isPushing()) {
                    mLivePusher.stopCameraPreview(false);
                    mLivePusher.startCameraPreview(mCaptureView);
                }

                Toast.makeText(getActivity(), mTouchFocus ? "已开启手动对焦" : "已开启自动对焦", Toast.LENGTH_SHORT).show();
            }
        });

        view.setOnClickListener(this);
        mLogViewStatus.setText("Log File Path:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/txRtmpLog");
        return view;
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
    }

    private boolean startPublishRtmp() {
        if (TextUtils.isEmpty(rtmpUrl) || (!rtmpUrl.trim().toLowerCase().startsWith("rtmp://"))) {
            mVideoPublish = false;
            Toast.makeText(getActivity().getApplicationContext(), "推流地址不合法，目前支持rtmp推流!", Toast.LENGTH_SHORT).show();
            return false;
        }

        mCaptureView.setVisibility(View.VISIBLE);
        mLivePushConfig.setWatermark(mBitmap, 10, 10);

        int customModeType = 0;

        mLivePushConfig.setVideoFPS(25);

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
        ;
        mLivePushConfig.setPauseImg(bitmap);
        mLivePusher.setConfig(mLivePushConfig);
        mLivePusher.setPushListener(this);
        mLivePusher.startCameraPreview(mCaptureView);
//        mLivePusher.startScreenCapture();
        mLivePusher.startPusher(rtmpUrl.trim());
        mLivePusher.setLogLevel(TXLiveConstants.LOG_LEVEL_DEBUG);

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
        mLivePusher.stopCameraPreview(true);
        mLivePusher.stopScreenCapture();
        mLivePusher.setPushListener(null);
        mLivePusher.stopPusher();
        mCaptureView.setVisibility(View.GONE);

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
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_720_1280);
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(1500);
                    mLivePusher.setConfig(mLivePushConfig);
                }
                mBtnBitrate.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_fix_bitrate"));
                break;
            case 3: /*540p*/
                if (mLivePusher != null) {
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_540_960);
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(1000);
                    mLivePusher.setConfig(mLivePushConfig);
                }
                mBtnBitrate.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_fix_bitrate"));
                break;
            case 2: /*360p*/
                if (mLivePusher != null) {
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
                    mLivePushConfig.setAutoAdjustBitrate(false);
                    mLivePushConfig.setVideoBitrate(700);
                    mLivePusher.setConfig(mLivePushConfig);
                }
                mBtnBitrate.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_fix_bitrate"));
                break;

            case 1: /*自动*/
                if (mLivePusher != null) {
                    mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_360_640);
                    mLivePushConfig.setAutoAdjustBitrate(true);
                    mLivePushConfig.setMaxVideoBitrate(1000);
                    mLivePushConfig.setMinVideoBitrate(500);
                    mLivePushConfig.setVideoBitrate(700);
                    mLivePusher.setConfig(mLivePushConfig);
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
        }

        if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {
            stopPublishRtmp();
            mVideoPublish = false;
        } else if (event == TXLiveConstants.PUSH_WARNING_HW_ACCELERATION_FAIL) {
            Toast.makeText(getActivity().getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
            mLivePushConfig.setHardwareAcceleration(false);
            mBtnHWEncode.setBackgroundResource(EUExUtil.getResDrawableID("plugin_uextencentlvb_quick2"));
            mLivePusher.setConfig(mLivePushConfig);
            mHWVideoEncode = false;
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_UNSURPORT) {
            stopPublishRtmp();
        } else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_START_FAILED) {
            stopPublishRtmp();
        }
    }

    @Override
    public void onNetStatus(Bundle status) {
        String str = getNetStatusString(status);
        mLogViewStatus.setText(str);
        Log.d(TAG, "Current status: " + status.toString());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == EUExUtil.getResIdID("beauty_seekbar")) {
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
        //如果您允许了横竖屏切换，请在切换时重启一下摄像头预览，以便SDK可以计算到正确的渲染角度
        //mLivePusher.stopCameraPreview(false);
        //mLivePusher.startCameraPreview(mCaptureView);
    }
}