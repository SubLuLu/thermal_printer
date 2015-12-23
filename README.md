# Java实现POS打印机自定义无驱打印  

**热敏打印机**使用越来越广泛，而安装驱动相当复杂，万幸的是，几乎所有的热敏打印机都支持ESC/P指令，参考网络上一些资料后，在此整理了一份自定义打印的方案  

## **• 打印模板**  

> 为了增强打印效果的通用性，因此需要提供多元化的模板对齐支持，而且不同大小的打印机所需的版式也不尽相同  

> 模板采用Json格式存储，分为header、goods、bill、footer四个部分，对模板的解析采用号称史上最快的阿里出品的fastjson  

### 模板示例  

```json
{
    "header": [
        {
            "text": "{$shopname}",
            "size": 2,
            "bold": true,
            "format": 1,
            "line": 2,
            "underline": true,
            "type": 0
        },
        {
            "text": "{$barCode}",
            "format": 1,
            "line": 2,
            "type": 1
        },
        {
            "path": "{$logo}",
            "format": 1,
            "line": 2,
            "type": 3
        },
        {
            "text": "{$qrCode}",
            "format": 1,
            "line": 2,
            "type": 2
        }
    ],
    "goods": [
        {
            "name": "商品名",
            "width": 24,
            "format": 0,
            "variable": "name"
        },
        {
            "name": "数量",
            "width": 8,
            "format": 1,
            "variable": "num"
        },
        {
            "name": "单价",
            "width": 8,
            "format": 1,
            "variable": "price"
        },
        {
            "name": "金额",
            "width": 8,
            "format": 2,
            "variable": "pay"
        }
    ],
    "bill": [
        {
            "text": "实收现金",
            "size": 3,
            "bold": true,
            "format": 1,
            "line": 2,
            "underline": false,
            "type": 0
        },
        {
            "text": "{$cash}",
            "size": 3,
            "bold": true,
            "format": 1,
            "line": 2,
            "underline": false,
            "type": 0
        }
    ],
    "footer": [
        {
            "text": "详情请访问官网",
            "size": 2,
            "bold": true,
            "format": 1,
            "line": 2,
            "underline": true,
            "type": 0
        },
        {
            "text": "http://www.sublulu.com",
            "format": 1,
            "line": 2,
            "type": 2
        }
    ]
}
```

模板的代码结构如上所示，可见每个部分均是Json数组  
> header、bill、footer三部分的结构一模一样，只是位置和内容有所差异  
> goods区域的数组里面每个元素都对应四个相同的属性  
> 类似`{$logo}`是模板中指定的占位符，能够更好的支持个性化  

### 模板参数规则
![模板打印参数](./打印内容.png)  

### goods参数详解  

```java
    /**
     * 列名
     */
    private String name;

    /**
     * 排版格式
     */
    private int format;

    /**
     * 列宽
     * 58mm 每行32个半角字符
     * 80mm 每行48个半角字符
     */
    private int width;

    /**
     * 占位符
     * e.g {$time}
     */
    private String variable;
```

## **• 打印参数**  

> 打印根据模板和打印参数合成按照顺序进行打印  
> 
> 打印参数替换模板中的占位符  
> 
> 打印参数解析商品信息进行输出  

### 参数示例  

```json
{
  "keys": {
    "shopname": "黄太吉",
    "barCode": "6921734976505",
    "qrCode": "https://www.sublulu.com",
    "time": "15:35",
    "num": 14,
    "cash": 324.5,
    "logo": "/sdcard/qr.png",
    "adv": "关注微信，有大大地活动哦"
  },
  "goods": [
    {
      "name": "鱼香肉丝",
      "num": 1,
      "price": 12.8,
      "pay": 12.8
    },
    {
      "name": "葱油粑粑",
      "num": 1,
      "price": 4.8,
      "pay": 4.8
    },
    {
      "name": "辣椒炒肉",
      "num": 1,
      "price": 14.8,
      "pay": 14.8
    }
  ]
}
```

打印参数的代码结构如上所示，主要分为keys和goods两个部分：
> keys中的值负责替换模板中的占位符，如果模板中有，keys中没有则将占位符原样输出  
> 
> goods中的参数对用模板中的goods的每个属性  

### 打印效果  

![打印效果](./打印效果.JPG)  

## **• 使用示例**  

> 打印工具采用单例模式 
> 
> 考虑可能要操作多个打印机，所以以每个ip为key，单例本身为value值 

### 获取EscPos实例  

```java
/**
 *  ip为打印机IP，需要配置
 *  端口默认为9100，请勿随意修改
 *  编码默认为“GBK”，传入打印机支持的编码
 */
EscPos.getInstance(String ip);
EscPos.getInstance(String ip, int port);
EscPos.getInstance(String ip, int port, String encoding);

EscConfig escConfig = new EscConfig(String ip);
EscConfig escConfig = new EscConfig(String ip, int ip);
EscConfig escConfig = new EscConfig(String ip, int ip, String encoding);
EscPos.getInstance(escConfig);
```

以上为几种获取EscPos实例的代码，EscConfig是对打印机的全局配置项，其详情如下：

```java
// 1 58mm 2. 80mm 默认为2
private int type;
	
// 最后退纸几行 默认为4
private int line;
	
// 打印机ip
private String host;
	
// 打印机端口 默认为9100
private int port;
	
// 打印机的编码格式 默认为"GBK"
private String encoding;
```

### 打印示例  

所有常用打印命令已经封装完毕，执行打印操作的代码十分简单，如下所示：

```jaca
// 获取EscPos实例
EscPos.getInstance("192.168.1.110");

// 根据模板内容和打印参数执行打印命令
EscPos.print(template, param);
```  

## **• 打印流程**  

EscPos工具类对外质保路了两种方法，且均为静态方法：  

* getInstance()及其重载，用于获取对象实例  
* print(String template, String param)，用于打印小票  

使用起来相当方便，但其流程略显复杂

### 打印主流程图
![主流程图](./主流程图.bmp)  

### goods打印流程图
![goods流程图](./goods流程图.bmp)

### header、bill、footer打印主流程图
![区域流程图](./区域流程图.bmp)


## • 网络参考资料

【1】[Java 实现 POS打印机 无驱打印](http://www.ibm.com/developerworks/cn/java/j-lo-pos/)  

【2】[fastjson 在 github 上项目源码](https://github.com/alibaba/fastjson)

【3】[stackoverflow 上关于 Esc/Pos 的问题](http://stackoverflow.com/search?q=Esc%2FPos)
