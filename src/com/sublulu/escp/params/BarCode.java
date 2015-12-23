package com.sublulu.escp.params;

/**
 * 条形码配置参数
 *
 * @author SubLuLu
 */
public class BarCode {

    // 打印内容类型
    private int type;
    // 条形码数字
    private String text;
    // 对齐方式 居左、居中、居右
    private int format;
    // 空行行数
    private int line;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

}
