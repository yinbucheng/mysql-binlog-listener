package cn.bucheng.mysql.callback;

/**
 * @author ：yinchong
 * @create ：2019/7/29 11:12
 * @description：
 * @modified By：
 * @version:
 */
public interface BinLogCommitPosition {
    /**
     * 用于更新系统中处理的binlog的偏移量
     * @param position binlog的偏移量
     */
    void commitBinLogPosition(long position);
}
