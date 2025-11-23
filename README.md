# 📨 Email Scheduler Server

매일 정해진 시간에 대규모 구독자에게 이메일을 자동 발송하는 Java 기반 스케줄러 서버입니다.  
스케줄링, 메시지 큐, 비동기 처리, 동시성 제어 등 백엔드 핵심 개념을 직접 구현하며  
**대량 이메일 발송 서비스**의 내부 동작 원리를 깊이 이해하는 것을 목표로 합니다.

---

## 🪄 프로젝트 선정 배경

평소 구독하던 뉴스레터 서비스가   
"정해진 시간에 엄청나게 많은 사용자에게 동시에" 메일을 발송하는 것이 신기했습니다.

- 이런 시스템은 어떤 구조로 동작할까?
- 메일은 어떤 방식으로 대량 발송될까?
- 한 번에 수십만 명에게 보내는데 서버는 멀쩡한 이유는 무엇일까?
- 메시지는 어떻게 관리되고 처리될까?

이 궁금증을 해결하기 위해 직접 설계하고 구현하며  
뉴스레터 발송 시스템의 내부 동작을 깊이 이해하는 것을 목표로 진행했습니다.

---

## 🎯 프로젝트 목표

### 1주차: 핵심 개념 이해

- 프로젝트 초기 설정 및 인프라 구성
- Spring `@Scheduled` 기반 스케줄러 동작 이해
- RabbitMQ & AMQP 기본 개념 학습
- Fanout 기반 Pub/Sub 실습

### 2주차: 실제 뉴스레터 서버 구축

- 뉴스레터 순차 발송 로직 구현
- 대량 구독자 발송 성능 측정  
  (1,000명 ➔ 10,000명 ➔ 100,000명)
- Paging 및 벌크 처리 기반 성능 개선
- 이메일 구독/구독취소 API 구현 (+ 유효성 검증)

---

## 🔮 Email Scheduler 전체 아키텍처 흐름

![final_email_scheduler_flow](/assets/final_email_scheduler_flow.png)

### 1단계: 발송 트리거 (Scheduler)

- 매일 오전 8시, `Scheduler`가 자동 실행됩니다.
- "지금 뉴스레터를 발송해야 한다"는 이벤트만 시작합니다.
- 어떤 파일을 보낼지, 메시지를 어떻게 만들지는 `Publisher`에게 전적으로 위임합니다.

### 2단계: 메시지 생성 & 발행 (Publisher ➔ RabbitMQ)

- `Publisher`는 DB에서 마지막으로 발송한 파일을 조회합니다.
- 다음 발송할 뉴스레터 파일을 결정하고, 파일 내용을 읽어 메시지(`JSON`)를 만듭니다.
- `RabbitMQ`의 `Fanout Exchange`에 메시지를 발행(`Publish`)합니다.
- 메시지는 하나의 큐(`newsletter.queue`)로 라우팅됩니다.

### 3단계: 메시지 수신 & 이메일 발송 로직 실행 (Consumer ➔ Processor)

- `Consumer`는 `newsletter.queue`에 메시지가 도착하는 즉시 수신합니다.
- 메시지를 그대로 `Processor`에게 전달합니다.
- `Processor`는 전체 발송 프로세스를 실제로 실행합니다.
    - 활성 구독자를 페이징으로 조회
    - 한 페이지(10,000명) 단위로 이메일 배치 발송
    - 발송 결과를 한 번에 DB에 저장 (`Batch Insert`)
    - 전체 발송 요약 로그 출력

---

## 🔎 전체 구조 한 줄 요약

> "Scheduler가 메일 발송 작업을 트리거하면,  
> Publisher가 뉴스레터 파일을 읽어 메시지를 만들고 RabbitMQ에 발행하며,  
> Consumer는 메시지를 받아 Processor가 구독자를 페이징해 대량 이메일을 배치 전송한다."

---

## ⚒️ 기술 스택

- Language: `Java 21`
- Framework: `Spring Boot 3.5.7`
- Message Queue: `RabbitMQ`
- Database: `MySQL`, `JPA`
- Build Tool: `Gradle`

---

## ⚙️ Docker 실행 방법

### 1) Docker 실행

```bash
cd infra
docker-compose up -d
```

### 2) 컨테이너 상태 확인

```bash
docker ps
```

### 정상 출력 예시

```text
CONTAINER ID   IMAGE                  PORTS
a1b2c3d4e5f6   rabbitmq:3-management  0.0.0.0:15672->15672/tcp
b2c3d4e5f6a7   mysql:8.0              0.0.0.0:3306->3306/tcp
```

### 3) Docker 중지

```bash
docker-compose down
```

## ✨서버 실행 방법

```bash
./gradlew bootRun
```

또는 `IntelliJ에서 EmailSchedulerServerApplication 실행`하면 된다.

---

## 💡 배운점

- `Fanout Exchange` 기반 `Pub/Sub` 메시지 흐름
- 뉴스레터 발송 시스템의 전체적인 아키텍처 구성
- 구독자 3명 ➔ 10만 명 증가에 대응하는 구조적/성능적 개선
- 이메일 구독/구독취소 API + 유효성 검증 처리
- 비동기 처리, 배치 처리, Paging 기반 대량 처리의 필요성과 구현 방법

2주 동안 RabbitMQ, 스케줄러, 비동기 시스템에 대해  
단순 개념이 아닌 실제 구현 가능한 수준의 이해를 목표로 꾸준히 개선했습니다.

--- 

## 📝 블로그 기록

> 프로젝트를 진행하면서 마주한 문제와 해결 과정, `RabbitMQ`와 스케줄러의 동작 원리,  
> 그리고 병렬 처리, 배치 처리, `Paging` 같은 설계 결정의 이유 등을 블로그에 꾸준히 기록했습니다.  
> 구현 결과만 기록하기 보다는 **왜 이런 구조를 선택했는지**, **어떤 시행착오를 겪었는지**를 중심으로 작성해  
> 앞으로 비슷한 시스템을 설계할 때 참고할 수 있는 자료가 되도록 하였습니다.

- [블로그 링크 - Open Mission 기록 모음](https://sihyun10.github.io/categories/open-mission/)

---

## 참고

본 프로젝트는 우아한테크코스 프리코스 **오픈미션 : 프리코스 챌린지**를 위해 진행되었습니다.
