import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class VersionedZooKeeperLock {
	private final ZooKeeper zk;
	private final String lockPath;
	private String myNode;

	public VersionedZooKeeperLock(ZooKeeper zk, String lockPath) {
		this.zk = zk;
		this.lockPath = lockPath;
	}

	public String lock() throws Exception {
		myNode = zk.create(
			lockPath + "/lock-",
			new byte[0],
			ZooDefs.Ids.OPEN_ACL_UNSAFE,
			CreateMode.EPHEMERAL_SEQUENTIAL
		);
		String myNodeName = myNode.substring(lockPath.length() + 1);

		while (true) {
			List<String> children = zk.getChildren(lockPath, false);
			Collections.sort(children);

			int myIndex = children.indexOf(myNodeName);
			if (myIndex == 0) return myNodeName;

			// Watch 등록 전 강제로 딜레이를 걸어 Watch 누락 유발
			Thread.sleep(1000);

			String prevNode = children.get(myIndex - 1);
			Stat stat = new Stat();
			try {
				zk.getData(lockPath + "/" + prevNode, false, stat);
			} catch (KeeperException.NoNodeException e) {
				continue;
			}
			int initialVersion = stat.getVersion();

			// Polling
			boolean lockAcquired = false;
			for (int i = 0; i < 5; i++) {
				try {
					Stat newStat = new Stat();
					zk.getData(lockPath + "/" + prevNode, false, newStat);
					int currentVersion = newStat.getVersion();

					if (currentVersion != initialVersion) {
						// 앞 노드 버전 변화 감지
						lockAcquired = true;
						break;
					}
				} catch (KeeperException.NoNodeException e) {
					// 노드가 사라짐
					lockAcquired = true;
					break;
				}
			}

			if (!lockAcquired) {
				System.out.println("VersionPollingLock - 앞 노드 변화 감지 실패, 재시도: " + myNodeName);
			}
		}
	}

	public void unlock() throws Exception {
		if (myNode != null) {
			zk.setData(myNode, "unlock".getBytes(), -1); // 버전 증가
			zk.delete(myNode, -1);
			System.out.println("VersionPollingLock - 락 해제: " + myNode);
		}
	}
}