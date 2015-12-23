package com.sublulu.escp.params;

/**
 * 文本配置参数
 *
 * @author SubLuLu
 */
public class Text {

    // 打印内容类型
    private int type;
    // 对齐方式 居左、居中、居右
    private int format;
    // 空行行数
    private int line;
    // 打印文本内容
    private String text;
    // 文本字体大小
    private int size;
    // 文本是否加粗
    private boolean bold;
    // 文本下划线
    private boolean underline;

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }
}
