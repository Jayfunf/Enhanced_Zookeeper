# Enhanced_Zookeeper
Ajou Univ. 25-1 Distributed Systems

아주대학교 분산시스템 프로젝트
ZooKeeperLock의 Watch 누락 문제 개선

# 요약
ZooKeeper 분산 락의 Watch 누락 문제를 해결할 수 있는 개선 방안을 제시하고 이 방안을 통해 Java기반 ZooKeeepr락을 구현하여 직접 문제 상황을 만들어 테스트하고 결과를 도출하고자 합니다.

분산시스템 5강에서 ZooKeeper의 race condition에 대해서 학습했습니다. 
특정 조건에서 주키퍼락은 rece condition이 발생할 수 있으며, 이게 곧 Watch 누락 문제라고 할 수 있습니다.

저는 이 rece condition, 다른 말로 Watch 누락 문제를 방지하기 위한 코드를 개발하고, 이를 실험을 통해 검증하고자 합니다.

그리고 강의에서 소개된 RC을 방지하기 위한 방법인 버전 기반 관리 방법을 직접 구현해보고 이 방법 또 한 실험을 통해 검증해 보고자 합니다.

## 개발
총 3개의 락을 구현해 보았습니다.

1. Watch 누락에 대한 방어 로직이 없는 기본적인 ZooKeeperLock(By Apache ZooKeeper)
2. Watch 누락 발생 시 재시도 로직을 추가한 ZooKeeperLock
3. 버전 관리 로직을 추가한 ZooKeeperLock

# 결과
![스크린샷 2025-06-03 23 16 45](https://github.com/user-attachments/assets/2e0ef64e-437a-4988-95af-af115115cb6d)

# PDF 다운로드
[ZooKeeperLockWatch누락_프로젝트 발표(web).pdf](https://github.com/user-attachments/files/20575179/ZooKeeperLockWatch._.web.pdf)

