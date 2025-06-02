import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class BasicZooKeeperLock {
	private final ZooKeeper zk;
	private final String lockPath;
	private String myNode;
	public BasicZooKeeperLock(ZooKeeper zk, String lockPath) {
		this.zk = zk;
		this.lockPath = lockPath;
	}

	public String lock() throws Exception {
		myNode = zk.create(
			lockPath + "/lock-", new byte[0],
			ZooDefs.Ids.OPEN_ACL_UNSAFE,
			CreateMode.EPHEMERAL_SEQUENTIAL
		);
		String myNodeName = myNode.substring(lockPath.length() + 1);

		List<String> children = zk.getChildren(lockPath, false);
		Collections.sort(children);

		int myIndex = children.indexOf(myNodeName);
		if (myIndex == 0) {
			return myNodeName;
		}

		String prevNode = children.get(myIndex - 1);

		Thread.sleep(500); // 딜레이 후 watch 등록

		Stat stat = zk.exists(lockPath + "/" + prevNode, event -> {
			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				System.out.println("LockTest - Delete event detected for: " + prevNode);
			}
		});

		return null;
	}

	public void unlock() throws Exception {
		if (myNode != null) {
			zk.delete(myNode, -1);
			System.out.println("LockTest - 락 해제: " + myNode);
		}
	}
}