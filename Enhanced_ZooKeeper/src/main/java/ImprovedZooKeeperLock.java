import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ImprovedZooKeeperLock {
	private final ZooKeeper zk;
	private final String lockPath;
	private String myNode;

	public ImprovedZooKeeperLock(ZooKeeper zk, String lockPath) {
		this.zk = zk;
		this.lockPath = lockPath;
	}

	public void lock() throws Exception {
		myNode = zk.create(lockPath + "/lock-", new byte[0],
			ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

		while (true) {
			List<String> children = zk.getChildren(lockPath, false);
			Collections.sort(children);
			String myNodeName = myNode.substring(lockPath.length() + 1);
			int myIndex = children.indexOf(myNodeName);

			if (myIndex == 0) return;

			String prevNode = children.get(myIndex - 1);
			String prevPath = lockPath + "/" + prevNode;
			Stat stat = zk.exists(prevPath, false);

			if (stat == null) {
				continue; // 삭제됨, 재확인 루프
			}

			CountDownLatch latch = new CountDownLatch(1);
			zk.exists(prevPath, event -> {
				if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
					latch.countDown();
				}
			});
			latch.await();
		}
	}

	public void unlock() throws Exception {
		if (myNode != null) {
			zk.delete(myNode, -1);
		}
	}
}