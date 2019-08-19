package cn.bucheng.binlog.binlog.book;

import cn.bucheng.mysql.handle.FieldValueHandle;
import cn.bucheng.mysql.util.BinLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

/**
 * @author buchengyin
 * @create 2019/7/27 11:34
 * @describe
 */
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
            byte[] contentData = (byte[])content;
            entity.setContent(new String(contentData));
        }
        return entity;
    }
}
