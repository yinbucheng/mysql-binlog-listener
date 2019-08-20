package cn.bucheng.mysql.callback;

import cn.bucheng.mysql.binlog.BinLogConfig;

/**
 * @author buchengyin
 * @create 2019/7/27 19:41
 * @describe
 */
public interface BinLogConfigHook {
    /**
     * 这里用于恢复系统处理的binlog的位置
     * 一般会从redis中获取binlog的的文件及偏移量调用config上面set方法进行回设
     * 防止binlog事件漏消费或者重复消费问题
     * @param config binlog配置
     */
    void config(BinLogConfig config);
}
