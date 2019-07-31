package cn.bucheng.mysql.binlog.chain;

import cn.bucheng.mysql.binlog.BinLogConfig;
import cn.bucheng.mysql.binlog.CompositeListener;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * @author ：yinchong
 * @create ：2019/7/29 13:37
 * @description：
 * @modified By：
 * @version:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class BinLogHandle {
    private String fileName;
    //开始加载的binlog的起点
    private long startPosition;
    //记录是否为正在执行的binlog
    private boolean isCurrent;
    private BinLogConfig config;
    private CompositeListener listener;
    private CountDownLatch countDownLatch;

    public BinLogHandle(String fileName, long startPosition, boolean isCurrent, BinLogConfig config, CompositeListener listener) {
        this.fileName = fileName;
        this.startPosition = startPosition;
        this.isCurrent = isCurrent;
        this.config = config;
        this.listener = listener;
        if (isCurrent) {
            countDownLatch = new CountDownLatch(1);
        }
    }

    @SuppressWarnings("all")
    public void execute(BinLogChain chain) {
        log.info("==========start execute binlog fileName:{} , fileSize:{}, startPosition:{}", fileName, startPosition);
        BinaryLogClient client = new BinaryLogClient(
                config.getHost(),
                config.getPort(),
                config.getUsername(),
                config.getPassword()
        );

        client.setBinlogFilename(fileName);
        if (startPosition > 0) {
            client.setBinlogPosition(startPosition);
        }
        client.registerEventListener(listener);
        try {
            client.connect();
            if (countDownLatch != null) {
                countDownLatch.await();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error(ex.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error(e.toString());
        }
        chain.procced();
    }

    //唤醒，当发现当前执行的binlog不是当前handle时调用该方法唤醒阻塞
    public void awaken() {
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }
}
