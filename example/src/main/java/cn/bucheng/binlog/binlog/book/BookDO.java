package cn.bucheng.binlog.binlog.book;

import cn.bucheng.mysql.annotation.ColumnName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：yinchong
 * @create ：2019/7/24 16:31
 * @description：
 * @modified By：
 * @version:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@cn.bucheng.mysql.annotation.TableName(schema = "ad_test", table = "ad_book")
public class BookDO implements Serializable {
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
