package cn.bucheng.mysql.callback;

/**
 * @author ：yinchong
 * @create ：2019/7/29 11:12
 * @description：
 * @modified By：
 * @version:
 */
public interface BinLogCommitPosition {
    void commitBinLogPosition(long position);
}
