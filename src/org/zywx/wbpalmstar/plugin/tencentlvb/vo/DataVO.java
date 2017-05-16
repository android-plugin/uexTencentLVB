package org.zywx.wbpalmstar.plugin.tencentlvb.vo;

import java.io.Serializable;


public class DataVO implements Serializable {

    private static final long serialVersionUID = -2215881477976500502L;

    private String url;
    private String bgImage;
    private OptionsVO options = new OptionsVO();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBgImage() {
        return bgImage;
    }

    public void setBgImage(String bgImage) {
        this.bgImage = bgImage;
    }

    public OptionsVO getOptions() {
        return options;
    }

    public void setOptions(OptionsVO options) {
        this.options = options;
    }
}
