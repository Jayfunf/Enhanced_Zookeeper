import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ParallelLockTest {
	private static final String LOCK_PATH = "/my-lock";
	private static final int CLIENT_COUNT = 10;

	public static void main(String[] args) throws Exception {
		TestingServer server = new TestingServer();
		ZooKeeper initZk = new ZooKeeper(server.getConnectString(), 3000, null);

		if (initZk.exists(LOCK_PATH, false) == null) {
			initZk.create(LOCK_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
		initZk.close();

		ExecutorService executor = Executors.newFixedThreadPool(CLIENT_COUNT);
		CountDownLatch latch = new CountDownLatch(CLIENT_COUNT);

		for (int i = 0; i < CLIENT_COUNT; i++) {
			final int clientId = i;
			executor.submit(() -> {
				try {
					ZooKeeper zk = new ZooKeeper(server.getConnectString(), 3000, null);
					BasicZooKeeperLock lock = new BasicZooKeeperLock(zk, LOCK_PATH); // 개선 전 사용 시
					// ImprovedZooKeeperLock lock = new ImprovedZooKeeperLock(zk, LOCK_PATH); // 개선 후 사용 시

					long startTime = System.nanoTime();
					lock.lock();
					long elapsedTime = System.nanoTime() - startTime;

					System.out.println("[Client " + clientId + "] 락 획득 (대기 시간 ns): " + elapsedTime);

					Thread.sleep(500); // 임계 구역 진입
					lock.unlock();
					System.out.println("[Client " + clientId + "] 락 릴리즈 완료");

					zk.close();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();
		server.close();

		System.out.println("모든 클라이언트 종료");
	}
}