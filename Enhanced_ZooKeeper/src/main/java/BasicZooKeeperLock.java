import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class BasicZooKeeperLock {
	private final ZooKeeper zk;
	private final String lockPath;
	private String myNode;
	private int retryCount = 0; // 추가: 재시도 횟수
	private final Random random = new Random();

	public BasicZooKeeperLock(ZooKeeper zk, String lockPath) {
		this.zk = zk;
		this.lockPath = lockPath;
	}

	public String lock() throws Exception {
		myNode = zk.create(lockPath + "/lock-", new byte[0],
			ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		String myNodeName = myNode.substring(lockPath.length() + 1);

		while (true) {
			List<String> children = zk.getChildren(lockPath, false);
			Collections.sort(children);
			int myIndex = children.indexOf(myNodeName);

			if (myIndex == 0) return myNode;

			String prevNode = children.get(myIndex - 1);
			CountDownLatch latch = new CountDownLatch(1);

			Stat stat = zk.exists(lockPath + "/" + prevNode, event -> {
				if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
					latch.countDown();
				}
			});

			Thread.sleep(5 + random.nextInt(45));

			if (stat == null) {
				Thread.sleep(5); // 삭제되었으면 재시도
				retryCount++;
				continue;
			}

			latch.await();
		}
	}

	public void unlock() throws Exception {
		if (myNode != null) {
			zk.delete(myNode, -1);
		}
	}

	public int getRetryCount() {
		return retryCount;
	}
}