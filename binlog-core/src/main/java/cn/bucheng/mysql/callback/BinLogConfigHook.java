package cn.bucheng.mysql.callback;

import cn.bucheng.mysql.binlog.BinLogConfig;

/**
 * @author buchengyin
 * @create 2019/7/27 19:41
 * @describe
 */
public interface BinLogConfigHook {
    void config(BinLogConfig config);
}
