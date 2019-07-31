package cn.bucheng.mysql.binlog.chain;

import java.util.LinkedList;
import java.util.List;

/**
 * @author ：yinchong
 * @create ：2019/7/29 13:47
 * @description：
 * @modified By：
 * @version:
 */
public class BinLogChainImpl implements BinLogChain {

    private List<BinLogHandle> handles = new LinkedList<>();
    private int index;
    private volatile BinLogHandle currentHandle;

    public void addHandle(BinLogHandle handle, boolean isCurrent) {
        handles.add(handle);
        if (isCurrent) {
            this.currentHandle = handle;
        }
    }

    @Override
    public void procced() {
        int length = handles.size();
        if (index < length) {
            handles.get(index++).execute(this);
        }
    }

    @Override
    public BinLogHandle getCurrentHandle() {
        return currentHandle;
    }
}
