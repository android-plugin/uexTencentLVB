package org.zywx.wbpalmstar.plugin.tencentlvb.util;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.tencentlvb.EUExTencentLVB;

import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends FragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private FragmentManager fragmentManager;
    private Fragment mPlayerVodFragment, mPlayerAliveFragment, mPublisherFragment;

    private String imageUrl;

    private FrameLayout flRoot;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(EUExUtil.getResLayoutID("plugin_uextencentlvb_activity_main"));

        fragmentManager = getSupportFragmentManager();
        init();
    }

    private void init() {
        Intent intent = getIntent();
        int action = intent.getIntExtra(EUExTencentLVB.TEXT_ACTION, EUExTencentLVB.ACTION_PUBLISH);
        String url = intent.getStringExtra(EUExTencentLVB.TEXT_URL);
        imageUrl = intent.getStringExtra(EUExTencentLVB.TEXT_IMAGE);
        flRoot = (FrameLayout) findViewById(EUExUtil.getResIdID("root"));
        BitmapDrawable drawable = new BitmapDrawable(getLocalImage(imageUrl));
        flRoot.setBackground(drawable);
        initFragment(action, url, intent.getBooleanExtra(EUExTencentLVB.BOOLEAN_IS_SHOW_LOG, true));
    }
    private void initFragment(int action, String url, boolean isShowLog) {
        Bundle bundle = new Bundle();
        bundle.putInt(EUExTencentLVB.TEXT_ACTION, action);
        bundle.putString(EUExTencentLVB.TEXT_URL, url);
        bundle.putBoolean(EUExTencentLVB.BOOLEAN_IS_SHOW_LOG, isShowLog);
        switch (action) {
            case EUExTencentLVB.ACTION_PUBLISH:
                if (mPublisherFragment == null) {
                    mPublisherFragment = new LivePublisherActivity();
                    mPublisherFragment.setArguments(bundle);
                    ((RTMPBaseActivity)mPublisherFragment).setActivityType(RTMPBaseActivity.ACTIVITY_TYPE_PUBLISH);
                }
                replaceFragment(mPublisherFragment);
                break;
            case EUExTencentLVB.ACTION_PLAY_VOD:
                if (mPlayerVodFragment == null) {
                    mPlayerVodFragment = new LivePlayerActivity();
                    mPlayerVodFragment.setArguments(bundle);
                    ((RTMPBaseActivity)mPlayerVodFragment).setActivityType(RTMPBaseActivity.ACTIVITY_TYPE_VOD_PLAY);
                }
                replaceFragment(mPlayerVodFragment);
                break;
            case EUExTencentLVB.ACTION_PLAY_LIVE:
                if (mPlayerAliveFragment == null) {
                    mPlayerAliveFragment = new LivePlayerActivity();
                    mPlayerAliveFragment.setArguments(bundle);
                    ((RTMPBaseActivity)mPlayerAliveFragment).setActivityType(RTMPBaseActivity.ACTIVITY_TYPE_LIVE_PLAY);
                }
                replaceFragment(mPlayerAliveFragment);
                break;
        }
    }

    public Bitmap getLocalImage(String imgUrl) {
        if (imgUrl == null || imgUrl.length() == 0) {
            return null;
        }

        Bitmap bitmap = null;
        InputStream is = null;
        try {
            if (imgUrl.startsWith(BUtility.F_Widget_RES_path)) {
                try {
                    is = getAssets().open(imgUrl);
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (imgUrl.startsWith("/")) {
                bitmap = BitmapFactory.decodeFile(imgUrl);
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    private void setDefaultFragment() {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        mPublisherFragment = new LivePublisherActivity();
        transaction.replace(EUExUtil.getResIdID("content_layout"), mPublisherFragment);
        transaction.commit();
    }

    private void replaceFragment(Fragment newFragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (!newFragment.isAdded()) {
            transaction.replace(EUExUtil.getResIdID("content_layout"), newFragment);
            transaction.commit();
        } else {
            transaction.show(newFragment);
        }
    }


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

	}

}
