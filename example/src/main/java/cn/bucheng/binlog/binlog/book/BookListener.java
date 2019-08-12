package cn.bucheng.binlog.binlog.book;


import cn.bucheng.mysql.listener.IListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @author buchengyin
 * @create 2019/7/27 11:20
 * @describe
 */
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
