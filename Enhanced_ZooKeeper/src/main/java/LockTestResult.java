import java.util.List;

public class LockTestResult {
	private List<Long> waitTimes;       // 각 클라이언트 락 대기 시간 (ns)
	private List<Integer> successIds;   // 성공한 클라이언트 ID
	private List<Integer> allClientIds; // 모든 클라이언트 ID
	private List<Integer> lockOrder;    // 실제 락 획득 순서

	public LockTestResult(List<Long> waitTimes, List<Integer> successIds, List<Integer> allClientIds, List<Integer> lockOrder) {
		this.waitTimes = waitTimes;
		this.successIds = successIds;
		this.allClientIds = allClientIds;
		this.lockOrder = lockOrder;
	}

	public void printStatistics(String lockType) {
		int totalClients = allClientIds.size();
		int successCount = successIds.size();
		double successRate = successCount * 100.0 / totalClients;
		double timeoutRate = (totalClients - successCount) * 100.0 / totalClients;

		long totalWait = waitTimes.stream().mapToLong(Long::longValue).sum();
		double avgWait = waitTimes.isEmpty() ? 0 : totalWait / (double) waitTimes.size() / 1_000_000.0; // ms 변환
		long maxWait = waitTimes.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000;

		System.out.println("\n=== " + lockType + " ===");
		System.out.println("총 클라이언트 수: " + totalClients);
		System.out.println("락 성공 수: " + successCount);
		System.out.println("락 성공률: " + String.format("%.2f", successRate) + "%");
		System.out.println("락 타임아웃률: " + String.format("%.2f", timeoutRate) + "%");
		System.out.println("평균 락 대기 시간: " + String.format("%.2f", avgWait) + " ms");
		System.out.println("최대 락 대기 시간: " + maxWait + " ms");
	}
}
