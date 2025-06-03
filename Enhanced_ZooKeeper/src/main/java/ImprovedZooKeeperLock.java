import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

	public String lock() throws Exception {
		myNode = zk.create(
			lockPath + "/lock-",
			new byte[0],
			ZooDefs.Ids.OPEN_ACL_UNSAFE,
			CreateMode.EPHEMERAL_SEQUENTIAL
		);
		String myNodeName = myNode.substring(lockPath.length() + 1);

		while (true) {
			// 내 순서 확인
			List<String> children = zk.getChildren(lockPath, false);
			Collections.sort(children);

			// 만약 내가 첫번째라면 락 획득
			int myIndex = children.indexOf(myNodeName);
			if (myIndex == 0) return myNodeName;

			// Watch 등록 전 강제로 딜레이를 걸어 Watch 누락 유발
			Thread.sleep(1000);

			// Watch를 기다리기 위한 Latch 생성
			CountDownLatch latch = new CountDownLatch(1);

			// 자신보다 앞 노드를 가져와 Watch 등록
			String prevNode = children.get(myIndex - 1);
			Stat stat = zk.exists(lockPath + "/" + prevNode, event -> {
				if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
					latch.countDown();
				}
			});

			if (stat == null) {
				// 앞 노드 이미 사라짐 -> 바로 루프 재시도
				continue;
			}

			boolean awaitSuccess = latch.await(1, TimeUnit.SECONDS);
			if (!awaitSuccess) {
				System.out.println("LockTest - 대기 타임아웃 발생, 다시 시도");
				continue;
			}
		}
	}

	public void unlock() throws Exception {
		if (myNode != null) {
			zk.delete(myNode, -1);
			System.out.println("LockTest - 락 해제: " + myNode);
		}
	}
}