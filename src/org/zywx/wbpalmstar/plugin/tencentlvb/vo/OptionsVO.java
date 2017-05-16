package org.zywx.wbpalmstar.plugin.tencentlvb.vo;

import java.io.Serializable;


public class OptionsVO implements Serializable {
    private static final long serialVersionUID = 7399356093901915453L;

    private boolean isShowLog = true;

    public boolean isShowLog() {
        return isShowLog;
    }

    public void setShowLog(boolean showLog) {
        isShowLog = showLog;
    }
}
