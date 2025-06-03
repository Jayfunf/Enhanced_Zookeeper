import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ParallelLockTest {
	private static final String LOCK_PATH = "/my-lock";
	private static final int CLIENT_COUNT = 1000;
	private static final long TEST_TIMEOUT_SEC = 60;

	public static void main(String[] args) throws Exception {
		ZooKeeper initZk = new ZooKeeper("localhost:2181", 3000, null);

		if (initZk.exists(LOCK_PATH, false) == null) {
			initZk.create(LOCK_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
		initZk.close();

		ExecutorService executor = Executors.newFixedThreadPool(CLIENT_COUNT);
		List<Long> waitTimes = Collections.synchronizedList(new ArrayList<>());
		List<Integer> allClientList = Collections.synchronizedList(new ArrayList<>());
		List<Integer> successList = Collections.synchronizedList(new ArrayList<>());
		List<Integer> lockOrder = Collections.synchronizedList(new ArrayList<>());

		for (int i = 0; i < CLIENT_COUNT; i++) {
			final int clientId = i;
			allClientList.add(clientId);
			executor.submit(() -> {
				try {
					ZooKeeper zk = new ZooKeeper("localhost:2181", 2000, null);

					// BasicZooKeeperLock lock = new BasicZooKeeperLock(zk, LOCK_PATH); // 개선 전 사용 시
					ImprovedZooKeeperLock lock = new ImprovedZooKeeperLock(zk, LOCK_PATH); // 개선 후 사용 시
					// VersionedZooKeeperLock lock = new VersionedZooKeeperLock(zk, LOCK_PATH); // 강의에서 소개된 버저닝

					long startTime = System.nanoTime(); // ==== 락 시도 시작 시간 ====
					String myNode = lock.lock();
					long endTime = System.nanoTime();   // ==== 락 획득 완료 시간 ====

					if (myNode != null) {
						System.out.println("LockTest - [" + clientId + "] 락 획득 성공: " + myNode);
						long waitTime = endTime - startTime;
						waitTimes.add(waitTime); // waitTime 기록
						successList.add(clientId);
						lockOrder.add(clientId); // 락 획득 순서 기록
						lock.unlock();
					}

					zk.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		executor.shutdown();
		if (!executor.awaitTermination(TEST_TIMEOUT_SEC, TimeUnit.SECONDS)) {
			System.out.println("Test timed out. Forcing shutdown...");

			executor.shutdownNow();
		}

		LockTestResult result = new LockTestResult(waitTimes, successList, allClientList, lockOrder);
		result.printStatistics("ImprovedZooKeeperLock");

	}
}