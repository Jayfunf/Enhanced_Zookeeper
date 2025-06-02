import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class SimpleZookeeperLock {
	private final ZooKeeper zk;
	private final String lockPath;
	private String myNode;
	private final String role;

	public SimpleZookeeperLock(ZooKeeper zk, String lockPath, String role) {
		this.zk = zk;
		this.lockPath = lockPath;
		this.role = role;
	}

	public String lock() throws Exception {
		myNode = zk.create(
			lockPath + "/lock-", new byte[0],
			ZooDefs.Ids.OPEN_ACL_UNSAFE,
			CreateMode.EPHEMERAL_SEQUENTIAL
		);
		String myNodeName = myNode.substring(lockPath.length() + 1);

		if (role.equals("C1")) {
			System.out.println("LockTest - C1 Sleep Start");
			Thread.sleep(200);
			System.out.println("LockTest - C1 Sleep End");
		}

		System.out.println("LockTest - " + role + " getChild Start");
		List<String> children = zk.getChildren(lockPath, false);
		Collections.sort(children);
		System.out.println("LockTest - " + role + " getChild end");

		int myIndex = children.indexOf(myNodeName);
		if (myIndex == 0) {
			// 락 획득
			System.out.println("LockTest - Get Lock: " + myNode);
			return myNodeName;
		}

		String prevNode = children.get(myIndex - 1);

		if (role.equals("C2")) {
			Thread.sleep(300); // 딜레이 후 watch 등록
		}

		Stat stat = zk.exists(lockPath + "/" + prevNode, event -> {
			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				System.out.println("LockTest - " + role + " Delete event detected for: " + prevNode);
			}
		});

		return null;
	}

	public void unlock() throws Exception {
		if (myNode != null) {
			zk.delete(myNode, -1);
			System.out.println("LockTest - 락 해제: " + role);
		}
	}
}