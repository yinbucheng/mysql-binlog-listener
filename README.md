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
```