import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class BasicZooKeeperLock {
	private final ZooKeeper zk;
	private final String lockBasePath;
	private String currentNode;

	public BasicZooKeeperLock(ZooKeeper zk, String lockBasePath) {
		this.zk = zk;
		this.lockBasePath = lockBasePath;
	}

	public void lock() throws Exception {
		String path = zk.create(lockBasePath + "/lock_", new byte[0],
			ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		this.currentNode = path;

		while (true) {
			List<String> children = zk.getChildren(lockBasePath, false);
			children.sort(String::compareTo);
			int index = children.indexOf(path.substring(lockBasePath.length() + 1));

			if (index == 0) break;

			String prevNode = children.get(index - 1);
			final CountDownLatch latch = new CountDownLatch(1);

			zk.exists(lockBasePath + "/" + prevNode, event -> {
				if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
					latch.countDown();
				}
			});

			latch.await();
		}
	}

	public void unlock() throws Exception {
		if (currentNode != null) {
			zk.delete(currentNode, -1);
		}
	}
}