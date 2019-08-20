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
public class BookHandle implements FieldValueHandle<BookDO> {
    @Override
    public Class getClassType() {
        return BookDO.class;
    }

    @Override
    public BookDO handle(Map<String, Serializable> values) {
        log.info("decode column value to object");
        BookDO entity = BinLogUtils.decode(BookDO.class, values);
        Serializable content = values.get("content");
        if(content!=null){
            byte[] contentData = (byte[])content;
            entity.setContent(new String(contentData));
        }
        return entity;
    }
}
