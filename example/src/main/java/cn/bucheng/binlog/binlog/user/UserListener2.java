package cn.bucheng.binlog.binlog.user;

import cn.bucheng.mysql.listener.IListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @author ：yinchong
 * @create ：2019/8/1 9:55
 * @description：
 * @modified By：
 * @version:
 */
@Component
@Slf4j
public class UserListener2 implements IListener<UserDO> {
    @Override
    public Class getClassType() {
        return UserDO.class;
    }

    @Override
    public void saveEvent(UserDO data) {
        log.info("save user event2,content:{}", data);
    }

    @Override
    public void updateEvent(UserDO data) {
        log.info("update user event2,content:{}", data);
    }

    @Override
    public void deleteEvent(Serializable id) {
        log.info("delete user event2,id:{}", id);
    }
}
