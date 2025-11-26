package com.postura.monitor.entity;

public enum SessionStatus {

    /** 모니터링이 시작되었고 현재 활성화 상태 (로그 수신 중) */
    STARTED,

    /** 사용자가 일시정지 버튼을 눌러 모니터링이 잠시 중단된 상태 */
    PAUSED,

    /** 사용자가 모니터링을 완료하고 종료한 상태 */
    COMPLETED

}
