package cn.bucheng.binlog.binlog.global;

import cn.bucheng.mysql.binlog.BinLogConfig;
import cn.bucheng.mysql.callback.BinLogConfigHook;
import cn.bucheng.mysql.constant.BinLogConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author ：yinchong
 * @create ：2019/7/29 11:20
 * @description：
 * @modified By：
 * @version:
 */
@Slf4j
@Component
public class GlobalConfigHandle implements BinLogConfigHook {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void config(BinLogConfig config) {
        Object filename = redisTemplate.opsForHash().get(BinLogConstant.BINLOG_PREFIX + "es", BinLogConstant.BINLOG_FILE);
        Object position = redisTemplate.opsForHash().get(BinLogConstant.BINLOG_PREFIX + "es", BinLogConstant.BINLOG_POSITION);
        if (filename != null && !filename.equals("") && position != null && !"".equals(position + "")) {
            log.info("begin load filename:{}, position:{}", filename, position);
            config.setFile(filename + "");
            config.setPosition(Long.parseLong(position + ""));
        }
    }
}
