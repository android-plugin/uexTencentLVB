package org.zywx.wbpalmstar.plugin.tencentlvb;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.plugin.tencentlvb.util.MainActivity;
import org.zywx.wbpalmstar.plugin.tencentlvb.vo.DataVO;

public class EUExTencentLVB extends EUExBase {

	public static final int ACTION_PUBLISH = 1;
	public static final int ACTION_PLAY_VOD = 2;
	public static final int ACTION_PLAY_LIVE = 3;
	public static final String TEXT_ACTION = "action";
	public static final String TEXT_URL = "url";
	public static final String TEXT_IMAGE = "image";
    public static final String BOOLEAN_IS_SHOW_LOG = "isShowLog";

    public EUExTencentLVB(Context context, EBrowserView eBrowserView) {
		super(context, eBrowserView);
	}


    public void vodPlay(String [] params) {
        if (params.length != 1) {
            return;
        }
        DataVO dataVO = DataHelper.gson.fromJson(params[0], DataVO.class);
        actionHandler(dataVO, ACTION_PLAY_VOD);
    }
    public void livePlay(String [] params) {
        if (params.length != 1) {
            return;
        }
        DataVO dataVO = DataHelper.gson.fromJson(params[0], DataVO.class);
        actionHandler(dataVO, ACTION_PLAY_LIVE);

    }
    public void publish(String [] params) {
        if (params.length != 1) {
            return;
        }
        DataVO dataVO = DataHelper.gson.fromJson(params[0], DataVO.class);
        actionHandler(dataVO,ACTION_PUBLISH);
    }
    public void actionHandler(String [] params, int action) {
        try {
            JSONObject json = new JSONObject(params[0]);
            String url = json.optString("url", null);
            String imageUrl = json.optString("bgImage", null);
            if (!TextUtils.isEmpty(url)) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.putExtra(TEXT_ACTION, action);
                intent.putExtra(TEXT_URL, url);
                String image = BUtility.makeRealPath(
                        BUtility.makeUrl(mBrwView.getCurrentUrl(), imageUrl),
                        mBrwView.getCurrentWidget().m_widgetPath,
                        mBrwView.getCurrentWidget().m_wgtType);
                intent.putExtra(TEXT_IMAGE, image);
                startActivity(intent);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public void actionHandler(DataVO data, int action) {
        String url = data.getUrl();
        String imageUrl = data.getBgImage();
        boolean isShowLog = data.getOptions().isShowLog();
        if (!TextUtils.isEmpty(url)) {
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.putExtra(TEXT_ACTION, action);
            intent.putExtra(TEXT_URL, url);
            String image = BUtility.makeRealPath(
                    BUtility.makeUrl(mBrwView.getCurrentUrl(), imageUrl),
                    mBrwView.getCurrentWidget().m_widgetPath,
                    mBrwView.getCurrentWidget().m_wgtType);
            intent.putExtra(TEXT_IMAGE, image);
            intent.putExtra(BOOLEAN_IS_SHOW_LOG, isShowLog);
            startActivity(intent);
        }

    }
    @Override
	protected boolean clean() {
		return true;
	}

}
