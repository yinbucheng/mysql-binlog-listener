package cn.bucheng.mysql.global;


import cn.bucheng.mysql.callback.BinLogCommitPosition;
import cn.bucheng.mysql.constant.BinLogConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author ：yinchong
 * @create ：2019/7/29 11:16
 * @description：
 * @modified By：
 * @version:
 */
@Component
@Slf4j
public class GlobalCommitPositionHandle implements BinLogCommitPosition {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Override
    public void commitBinLogPosition(long position) {
        log.info("update position to redis position:{}", position);
        redisTemplate.opsForHash().put(BinLogConstant.BINLOG_PREFIX+"es", BinLogConstant.BINLOG_POSITION, position + "");
    }
}
