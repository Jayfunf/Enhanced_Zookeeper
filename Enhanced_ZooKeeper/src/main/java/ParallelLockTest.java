import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ParallelLockTest {
	private static final String LOCK_PATH = "/my-lock";
	private static final int CLIENT_COUNT = 150;
	private static final List<Long> elapsedTimes = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> acquiredNodeNames = Collections.synchronizedList(new ArrayList<>());
	private static final AtomicInteger successCount = new AtomicInteger(0); // 락 획득 성공 수
	private static final List<Integer> retryCounts = Collections.synchronizedList(new ArrayList<>()); // 각 클라이언트의 재시도 횟수 기록

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
					String myNode = lock.lock();
					long elapsedTime = System.nanoTime() - startTime;

					if (myNode != null) {
						String nodeName = myNode.substring(myNode.lastIndexOf("/") + 1);

						elapsedTimes.add(elapsedTime);
						acquiredNodeNames.add(nodeName);
						retryCounts.add(lock.getRetryCount()); // 재시도 횟수 기록
						successCount.incrementAndGet();
						System.out.println("[Client " + clientId + "] 락 획득: " + nodeName +
							" (대기 시간 ns): " + elapsedTime +
							" (재시도 횟수): " + lock.getRetryCount());
					}

					Thread.sleep(500); // 임계구역 점유
					lock.unlock();
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

		long total = elapsedTimes.stream().mapToLong(Long::longValue).sum();
		long max = elapsedTimes.stream().mapToLong(Long::longValue).max().orElse(0);
		long min = elapsedTimes.stream().mapToLong(Long::longValue).min().orElse(0);
		double avg = total / (double) elapsedTimes.size();
		double successRate = successCount.get() * 100.0 / CLIENT_COUNT;
		double avgRetry = retryCounts.stream().mapToInt(Integer::intValue).average().orElse(0);

		System.out.println("\n[성능 요약 결과]");
		System.out.println("총 클라이언트 수: " + CLIENT_COUNT);
		System.out.println("락 획득 성공 수: " + successCount.get());
		System.out.println("락 획득 성공률: " + successRate + "%");
		System.out.println("평균 락 대기 시간(ns): " + avg);
		System.out.println("최소 락 대기 시간(ns): " + min);
		System.out.println("최대 락 대기 시간(ns): " + max);
		System.out.println("평균 재시도 횟수: " + avgRetry);
		System.out.println("실제 락 노드 번호 순서: " + acquiredNodeNames);
	}
}