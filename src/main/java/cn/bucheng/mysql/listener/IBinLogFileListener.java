package cn.bucheng.mysql.listener;

/**
 * @author ：yinchong
 * @create ：2019/7/31 15:32
 * @description：
 * @modified By：
 * @version:
 */
public interface IBinLogFileListener {
    void handleBinLogFile(String fileName,long position);
}
