package com.sublulu.escp.params;

/**
 * 图片配置参数
 *
 * 热敏打印机一般只能打印黑白
 * 所以图片是经过二值化处理后的
 *
 * @author SubLuLu
 */
public class Image {

    // 打印内容类型
    private int type;
    // 对齐方式 居左、居中、居右
    private int format;
    // 空行行数
    private int line;
    // 图片绝对路径
    private String path;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
