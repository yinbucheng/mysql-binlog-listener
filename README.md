# mysql binlog监听器


## 使用

```
1.执行 mvn install

2.引入下面包
<dependency>
      <groupId>cn.bucheng</groupId>
      <artifactId>spring-boot-starter-mysql-binlog</artifactId>
      <version>0.0.1-SNAPSHOT</version>
</dependency>


3.添加下面配置
mysql.binlog.host=127.0.0.1
mysql.binlog.port=3306
mysql.binlog.username=root
mysql.binlog.password=123456

4.创建需要监听的实体对象

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@cn.bucheng.mysql.annotation.TableName(schema = "ad_test", table = "ad_book")
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


```
开发更加高级用法。。。。
```


