package com.bandmong.stream;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * DB에 확실히 저장된 다음에만 화면으로 내보내기.
 * 트랜잭션 안에서 바로 보내면, 그 뒤에 롤백돼도 상대 화면에는 "없는 메시지"가 이미 떠 있게 된다.
 */
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
