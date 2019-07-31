package cn.bucheng.mysql.binlog.chain;

public interface BinLogChain {
    void addHandle(BinLogHandle handle,boolean isCurrent);

    void procced();

    BinLogHandle getCurrentHandle();
}
