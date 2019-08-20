package cn.bucheng.mysql.listener;

/**
 * @author ：yinchong
 * @create ：2019/7/31 15:32
 * @description：
 * @modified By：
 * @version:
 */
public interface IBinLogFileListener {
    /**
     * 全局处理binlog的文件名称及位置
     * 一般通过这个方法将初始化的binlog中的文件名称及位置保存
     * @param fileName binlog现在所处的文件名称
     * @param position binlog当前文件的偏移量
     */
    void handleBinLogFile(String fileName, long position);
}
