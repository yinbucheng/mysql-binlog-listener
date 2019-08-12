# cn.bucheng.mysql binlog监听器

## 前置操作

```
1.查看mysql是否开启binlog
show variables like 'log_bin';

2.查看是否使用row格式的binlog
show variables like 'binlog_format';

3.如果以上都不是请修改mysql的配置文件添加或者修改如下内容
#配置binlog存放路径
log-bin=E://cn.bucheng.mysql//binlog//cn.bucheng.mysql-bin
#bin日志的格式 Mixed/row
binlog-format=row

4.重启mysql再次执行1.2步查看是否生效
```

## 使用

```
1.执行 mvn install

2.引入下面包
<dependency>
      <groupId>cn.intellif</groupId>
      <artifactId>spring-boot-starter-cn.bucheng.mysql-binlog</artifactId>
      <version>0.0.1-SNAPSHOT</version>
</dependency>


3.添加下面配置
cn.bucheng.mysql.binlog.host=127.0.0.1
cn.bucheng.mysql.binlog.port=3306
cn.bucheng.mysql.binlog.username=root
cn.bucheng.mysql.binlog.password=123456

4.创建需要监听的实体对象
(目前默认将java字段转数库字段规则：将大写字母小写并在前面添加_ 比如 userName -转变为-> user_name)

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@cn.bucheng.cn.bucheng.mysql.annotation.TableName(schema = "ad_test", table = "ad_book")
public class BookEntity implements Serializable {
   //如果数据库中列表和表名相同可以不用添加Column注解
    private Long id;
    private String name;
    private String title;
    private String writer;
    @ColumnName(sqlColumn = "create_time")
    private Date createTime;
    @ColumnName(sqlColumn = "update_time")
    private Date updateTime;
    private String content;

}


5.创建监听并添加到springboot容器中
@Component
@Slf4j
public class BookListener implements IListener<BookEntity> {
    @Override
    public Class getClassType() {
        return BookEntity.class;
    }

  
    @Override
    public void saveEvent(BookEntity data) {
        log.info("save event, " + data);
    }

    @Override
    public void updateEvent(BookEntity data) {
        log.info("update event," + data);
    }

    @Override
    public void deleteEvent(Serializable id) {
        log.info("delete event," + id);
    }
}


6.默认会将数据回设到对象上面当时存在有些字段回设不成功此时可以利用下面来实现高级用法
@Component
@Slf4j
public class BookHandle implements FieldValueHandle<BookEntity> {
    @Override
    public Class getClassType() {
        return BookEntity.class;
    }

    @Override
    public BookEntity handle(Map<String, Serializable> values) {
        log.info("decode column value to object");
        BookEntity entity = BinLogUtils.decode(BookEntity.class, values);
        Serializable content = values.get("content");
        if(content!=null){
            byte[] datas = (byte[])content;
            entity.setContent(new String(datas));
        }
        return entity;
    }
}

```

## 高级用法

```
1.记录binlog加载文件并初始化偏移量为0
@Component
@Slf4j
public class GlobalBinLogFileHandle implements IBinLogFileListener {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void handleBinLogFile(String fileName, long position) {
        redisTemplate.opsForHash().put("es-boot-binLog", "filename", fileName);
        redisTemplate.opsForHash().put("es-boot-binLog", "position", position + "");
        log.info("save binLogFile:{} position:{}",fileName,position);
    }
}

2.记录binlog加载的偏移量
@Component
@Slf4j
public class GlobalCommitPositionHandle implements BinLogCommitPosition {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Override
    public void commitBinLogPosition(long position) {
        log.info("update position to redis position:{}", position);
        redisTemplate.opsForHash().put("es-boot-binLog", "position", position + "");
    }
}

3.服务异常重启时恢复上次加载位置
@Slf4j
@Component
public class GlobalConfigHandle implements BinLogConfigHook {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Override
    public void configReset(BinLogConfig config) {
        Object filename = redisTemplate.opsForHash().get("es-boot-binLog", "filename");
        Object position = redisTemplate.opsForHash().get("es-boot-binLog", "position");
        if (filename != null && !filename.equals("") && position != null && !"".equals(position + "")) {
            log.info("begin load filename:{}, position:{}", filename, position);
            config.setFile(filename + "");
            config.setPosition(Long.parseLong(position + ""));
        }
    }
}

4.注意点，
>>1:不要使用监听的库存放上面记录，不然会出现死循环，也就binlog不断在变化
>>2:不要在监听器中进行耗时操作，这样会阻塞其他监听器及时接收消息能力。如果需要耗时的操作请采用开启线程进行操作
```

## demo地址
```
https://github.com/yinbucheng/es-boot
```
