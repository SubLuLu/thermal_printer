package com.sublulu.escp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sublulu.escp.params.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 控制打印机工具类
 *
 *
 *
 * @author SubLuLu
 */
public class EscPos {

    private static String encoding = null;

    // 通过socket流进行读写
    private OutputStream socketOut = null;
    private OutputStreamWriter writer = null;

    // 以ip作为key，EscPos实例作为value的Map
    private static Map<String, EscPos> posMap = new HashMap<String, EscPos>();
    private static EscPos escPos = null;

    /**
     * 根据ip、端口、字符编码构造工具类实例
     *
     * @param ip          打印机ip
     * @param port        打印机端口，默认9100
     * @param encoding    打印机支持的编码格式(主要针对中文)
     * @throws IOException
     */
    public EscPos(String ip, int port, String encoding) throws IOException {
        Socket socket = new Socket(ip, port);
        socketOut = socket.getOutputStream();
        socket.isClosed();
        this.encoding = encoding;
        writer = new OutputStreamWriter(socketOut, encoding);
    }

    public synchronized static EscPos getInstance(String ip, Integer port, String encoding) throws IOException {
        escPos = posMap.get(ip);
        if (escPos == null) {
            escPos = new EscPos(ip, port, encoding);
        }
        return escPos;
    }

    public synchronized static EscPos getInstance(String ip, Integer port) throws IOException {
        return getInstance(ip, port, Constant.DEFAULT_ENCODING);
    }

    public static synchronized EscPos getInstance(String ip) throws IOException {
        return getInstance(ip, Constant.DEFAULT_PORT, Constant.DEFAULT_ENCODING);
    }

    /**
     * 根据某班内容和参数打印小票
     *
     * @param template 模板内容
     * @param param    参数
     * @throws IOException
     */
    public static void print(String template, String param) throws IOException {
        PosParam posParam = JSON.parseObject(param, PosParam.class);

        Map<String, Object> keyMap = posParam.getKeys();
        List<Map<String, Object>> goodsParam = posParam.getGoods();

        // replace placeholder in template
        Pattern pattern = Pattern.compile(Constant.REPLACE_PATTERN);

        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            matcher.appendReplacement(sb, keyMap.get(key).toString());
        }

        matcher.appendTail(sb);

        template = sb.toString();

        PosTpl posTpl = JSON.parseObject(template, PosTpl.class);

        // print header
        for (JSONObject jsonObject : posTpl.getHeader()) {
            print(jsonObject);
        }

        // print goods
        // print title
        for (Goods goods : posTpl.getGoods()) {
            printTitle(goods);
        }
        escPos.line(1);

        // print detail
        for (Map<String, Object> goods : goodsParam) {
            printGoods(goods, posTpl.getGoods());
        }

        // print bill
        for (JSONObject jsonObject : posTpl.getBill()) {
            print(jsonObject);
        }

        // print footer
        for (JSONObject jsonObject : posTpl.getFooter()) {
            print(jsonObject);
        }

        escPos.line(2);

        escPos.feedAndCut();
    }

    /**
     * 换行
     *
     * @param lineNum 换行数，0为不换行
     * @return
     * @throws IOException
     */
    private EscPos line(int lineNum) throws IOException {
        for (int i=0; i<lineNum; i++) {
            writer.write("\n");
            writer.flush();
        }
        return this;
    }

    /**
     * 下划线
     *
     * @param flag false为不添加下划线
     * @return
     * @throws IOException
     */
    private EscPos underline(boolean flag) throws IOException {
        if (flag) {
            writer.write(0x1B);
            writer.write(45);
            writer.write(2);
        }
        return this;
    }

    /**
     * 取消下划线
     *
     * @param flag true为取消下划线
     * @return
     * @throws IOException
     */
    private EscPos underlineOff(boolean flag) throws IOException {
        if (flag) {
            writer.write(0x1B);
            writer.write(45);
            writer.write(0);
        }
        return this;
    }

    /**
     * 加粗
     *
     * @param flag false为不加粗
     * @return
     * @throws IOException
     */
    private EscPos bold(boolean flag) throws IOException {
        if (flag) {
            writer.write(0x1B);
            writer.write(69);
            writer.write(0xF);
        }
        return this;
    }

    /**
     * 取消粗体
     *
     * @param flag true为取消粗体模式
     * @return
     * @throws IOException
     */
    private EscPos boldOff(boolean flag) throws IOException {
        if (flag) {
            writer.write(0x1B);
            writer.write(69);
            writer.write(0);
        }
        return this;
    }

    /**
     * 排版
     *
     * @param position 0：居左(默认) 1：居中 2：居右
     * @return
     * @throws IOException
     */
    private EscPos align(int position) throws IOException {
        writer.write(0x1B);
        writer.write(97);
        writer.write(position);
        return this;
    }

    /**
     * 初始化打印机
     *
     * @return
     * @throws IOException
     */
    private EscPos init() throws IOException {
        writer.write(0x1B);
        writer.write(0x40);
        return this;
    }

    /**
     * 二维码排版对齐方式
     *
     * @param position   0：居左(默认) 1：居中 2：居右
     * @param moduleSize 二维码version大小
     * @return
     * @throws IOException
     */
    private EscPos alignQr(int position, int moduleSize) throws IOException {
        writer.write(0x1B);
        writer.write(97);
        if (position == 1) {
            writer.write(1);
            centerQr(moduleSize);
        } else if (position == 2){
            writer.write(2);
            rightQr(moduleSize);
        } else {
            writer.write(0);
        }
        return this;
    }

    /**
     * 居中牌排列
     *
     * @param moduleSize  二维码version大小
     * @throws IOException
     */
    private void centerQr(int moduleSize) throws IOException {
        switch (moduleSize) {
            case 1 :{
                printSpace(16);
                break;
            }
            case 2 : {
                printSpace(18);
                break;
            }
            case 3 :{
                printSpace(20);
                break;
            }
            case 4 : {
                printSpace(22);
                break;
            }
            case 5 : {
                printSpace(24);
                break;
            }
            case 6 : {
                printSpace(26);
                break;
            }
            default:
                break;
        }
    }

    /**
     * 二维码居右排列
     *
     * @param moduleSize  二维码version大小
     * @throws IOException
     */
    private void rightQr(int moduleSize) throws IOException {
        switch (moduleSize) {
            case 1 :
                printSpace(14);
                break;
            case 2 :
                printSpace(17);
                break;
            case 3 :
                printSpace(20);
                break;
            case 4 :
                printSpace(23);
                break;
            case 5 :
                printSpace(26);
                break;
            case 6 :
                printSpace(28);
                break;
            default:
                break;
        }
    }

    /**
     * 打印空白
     *
     * @param length  需要打印空白的长度
     * @throws IOException
     */
    private void printSpace(int length) throws IOException {
        for (int i=0; i<length; i++) {
            writer.write(" ");
        }
        writer.flush();
    }

    /**
     * 字体大小
     *
     * @param size 1-8 选择字号
     * @return
     * @throws IOException
     */
    private EscPos size(int size) throws IOException {
        int fontSize;
        switch (size) {
            case 1:
                fontSize = 0;
                break;
            case 2:
                fontSize = 17;
                break;
            case 3:
                fontSize =34;
                break;
            case 4:
                fontSize = 51;
                break;
            case 5:
                fontSize = 68;
                break;
            case 6:
                fontSize = 85;
                break;
            case 7:
                fontSize = 102;
                break;
            case 8:
                fontSize = 119;
                break;
            default:
                fontSize = 0;
        }
        writer.write(0x1D);
        writer.write(33);
        writer.write(fontSize);
        return this;
    }

    /**
     * 重置字体大小
     *
     * @return
     * @throws IOException
     */
    private EscPos sizeReset() throws IOException {
        writer.write(0x1B);
        writer.write(33);
        writer.write(0);
        return this;
    }

    /**
     * 进纸并全部切割
     *
     * @return
     * @throws IOException
     */
    private EscPos feedAndCut() throws IOException {
        writer.write(0x1D);
        writer.write(86);
        writer.write(65);
        writer.write(0);
        writer.flush();
        return this;
    }

    /**
     * 打印条形码
     *
     * @param value
     * @return
     * @throws IOException
     */
    private EscPos barCode(String value) throws IOException {
        writer.write(0x1D);
        writer.write(107);
        writer.write(67);
        writer.write(value.length());
        writer.write(value);
        writer.flush();
        return this;
    }

    /**
     * 打印二维码
     *
     * @param qrData
     * @return
     * @throws IOException
     */
    private EscPos qrCode(int position, String qrData) throws IOException {
        int moduleSize = 0;
        int length = qrData.getBytes(encoding).length;
        int l = (int) (Math.ceil(1.5*length) * 8);
        if (l<200) {
            moduleSize = 1;
        } else if (l<429) {
            moduleSize = 2;
        } else if (l<641) {
            moduleSize = 3;
        } else if (l<885) {
            moduleSize = 4;
        } else if (l<1161) {
            moduleSize = 5;
        } else if (l<1469) {
            moduleSize = 6;
        }

        alignQr(position, moduleSize);

        writer.write(0x1D);// init
        writer.write("(k");// adjust height of barcode
        writer.write(length + 3); // pl
        writer.write(0); // ph
        writer.write(49); // cn
        writer.write(80); // fn
        writer.write(48); //
        writer.write(qrData);

        writer.write(0x1D);
        writer.write("(k");
        writer.write(3);
        writer.write(0);
        writer.write(49);
        writer.write(69);
        writer.write(48);

        writer.write(0x1D);
        writer.write("(k");
        writer.write(3);
        writer.write(0);
        writer.write(49);
        writer.write(67);
        writer.write(moduleSize);

        writer.write(0x1D);
        writer.write("(k");
        writer.write(3); // pl
        writer.write(0); // ph
        writer.write(49); // cn
        writer.write(81); // fn
        writer.write(48); // m

        writer.flush();

        return this;
    }

    /**
     * 打印图片
     *
     * @param path  图片地址
     * @return
     */
    private EscPos image(String path) throws IOException {
        // trans to byte array
        Bitmap bmp  = BitmapFactory.decodeFile(path);

        byte[] data = new byte[] { 0x1B, 0x33, 0x00 };
        write(data);
        data[0] = (byte)0x00;
        data[1] = (byte)0x00;
        data[2] = (byte)0x00;    //重置参数

        int pixelColor;

        // ESC * m nL nH 点阵图
        byte[] escBmp = new byte[] { 0x1B, 0x2A, 0x00, 0x00, 0x00 };

        escBmp[2] = (byte)0x21;

        //nL, nH
        escBmp[3] = (byte)(bmp.getWidth() % 256);
        escBmp[4] = (byte)(bmp.getWidth() / 256);

        // 每行进行打印
        for (int i = 0; i < bmp.getHeight()  / 24 + 1; i++){
            write(escBmp);

            for (int j = 0; j < bmp.getWidth(); j++){
                for (int k = 0; k < 24; k++){
                    if (((i * 24) + k) < bmp.getHeight()){
                        pixelColor = bmp.getPixel(j, (i * 24) + k);
                        if (pixelColor != -1){
                            data[k / 8] += (byte)(128 >> (k % 8));
                        }
                    }
                }

                write(data);
                // 重置参数
                data[0] = (byte)0x00;
                data[1] = (byte)0x00;
                data[2] = (byte)0x00;
            }
            //换行
            byte[] byte_send1 = new byte[2];
            byte_send1[0] = 0x0d;
            byte_send1[1] = 0x0a;
            write(byte_send1);
        }
        return this;
    }

    private void write(byte ...data) throws IOException {
        socketOut.write(data);
        socketOut.flush();
    }

    /**
     * 打印字符串
     *
     * @param str 所需打印字符串
     * @return
     * @throws IOException
     */
    private EscPos printStr(String str) throws IOException {
        writer.write(str);
        writer.flush();
        return this;
    }

    /**
     * 打印任何对象
     *
     * @param jsonObject  需要输出对象
     * @throws IOException
     */
    private static void print(JSONObject jsonObject) throws IOException {
        int type = jsonObject.getInteger("type");

        switch (type) {
            case 0:
                Text text = JSON.toJavaObject(jsonObject, Text.class);
                printText(text);
                break;
            case 1:
                BarCode barCode = JSON.toJavaObject(jsonObject, BarCode.class);
                printBarCode(barCode);
                break;
            case 2:
                QrCode qrCode = JSON.toJavaObject(jsonObject, QrCode.class);
                printQrCode(qrCode);
                break;
            case 3:
                Image image = JSON.toJavaObject(jsonObject, Image.class);
                printImage(image);
                break;
        }
    }

    /**
     * 打印纯文本
     *
     * @param text  文本内容
     * @throws IOException
     */
    private static void printText(Text text) throws IOException {
        escPos.align(text.getFormat())
                .bold(text.isBold())
                .underline(text.isUnderline())
                .size(text.getSize())
                .printStr(text.getText())
                .boldOff(text.isBold())
                .underlineOff(text.isUnderline())
                .sizeReset()
                .line(text.getLine());
    }

    /**
     * 打印条形码
     *
     * @param barCode   条形码内容
     * @throws IOException
     */
    private static void printBarCode(BarCode barCode) throws IOException {
        escPos.align(barCode.getFormat())
                .barCode(barCode.getText())
                .line(barCode.getLine());
    }

    /**
     * 打印二维码
     *
     * @param qrCode   二维码内容
     * @throws IOException
     */
    private static void printQrCode(QrCode qrCode) throws IOException {
        escPos.qrCode(qrCode.getFormat(), qrCode.getText())
                .line(qrCode.getLine());
    }

    /**
     * 打印图片
     *
     * @param image  图片内容
     * @throws IOException
     */
    private static void printImage(Image image) throws IOException {
        escPos.align(image.getFormat())
                .image(image.getPath())
                .line(image.getLine());
        escPos.init();
    }

    /**
     * 打印商品小票的列名
     *
     * @param goods
     * @throws IOException
     */
    private static void printTitle(Goods goods) throws IOException {
        escPos.align(goods.getFormat())
                .bold(false)
                .underline(false)
                .size(1)
                .printStr(fillLength(goods.getName(), goods))
                .boldOff(false)
                .underlineOff(false)
                .sizeReset()
                .line(0);
    }

    /**
     * 循环打印商品信息
     *
     * @param goods
     * @param goodsList
     * @throws IOException
     */
    private static void printGoods(Map<String, Object> goods, List<Goods> goodsList) throws IOException {
        for (Goods ele : goodsList) {
            escPos.align(ele.getFormat())
                    .bold(false)
                    .underline(false)
                    .size(1)
                    .printStr(fillLength(goods.get(ele.getVariable()).toString(), ele))
                    .boldOff(false)
                    .underlineOff(false)
                    .line(0);
        }
        escPos.line(1);
    }

    /**
     * 填充打印文本长度
     *
     * @param str
     * @param goods
     * @return
     */
    private static String fillLength(String str, Goods goods) {
        try {
            int width = goods.getWidth();
            int length = str.getBytes(encoding).length;
            switch (goods.getFormat()) {
                case 0: {
                    while (length < width) {
                        str += " ";
                        length++;
                    }
                    break;
                }
                case 1: {
                    if (length < width) {
                        String text = "";
                        int pre = (width - length) / 2;
                        int end = width - length - pre;
                        while (pre > 0) {
                            text += " ";
                            pre--;
                        }
                        while (end > 0) {
                            str += " ";
                            end--;
                        }
                        str = text + str;
                    }
                    break;
                }
                case 2: {
                    String text = "";
                    while (length < width) {
                        text += " ";
                        length++;
                    }
                    str = text + str;
                    break;
                }
                default:
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

}
