package com.pycoder.taczintetra.network;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadIntentServerGateTest {
    @Test
    void acceptedIntentEntersTaczAuthoritativeReloadPath() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/network/ReloadIntentMessage.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("IGunOperator.fromLivingEntity"));
        assertTrue(source.contains("ModularGunLifecycleAdapter.startReload"));
        assertFalse(source.contains("ReloadRuntimeSettlementService.markPending"));
        assertTrue(source.contains("!message.singleBatch()"));
        assertTrue(source.contains("ReloadRuntimeSettlementService.isPending(held)"));
        assertTrue(source.contains("gate.rollback(message)"));
        assertTrue(source.contains("if (!started || !pending)"));
    }

    @Test
    void rejectsReplayPerHandButAllowsTheOtherHand() {
        ReloadIntentServerGate gate = new ReloadIntentServerGate();
        ReloadIntentMessage main = new ReloadIntentMessage(false, true, 4, 2);
        ReloadIntentMessage offHand = new ReloadIntentMessage(true, true, 8, 1);

        assertTrue(gate.accept(main, 4, true, false, false, false));
        assertFalse(gate.accept(main, 4, true, false, false, false));
        assertTrue(gate.accept(offHand, 8, true, false, false, false));
    }

    @Test
    void doesNotAdvanceSequenceWhenStateOrIdentityIsInvalid() {
        ReloadIntentServerGate gate = new ReloadIntentServerGate();
        ReloadIntentMessage intent = new ReloadIntentMessage(false, false, 4, 3);

        assertFalse(gate.accept(intent, 5, true, false, false, false));
        assertFalse(gate.accept(intent, 4, false, false, false, false));
        assertTrue(gate.accept(intent, 4, true, false, false, false));
    }

    @Test
    void rejectsOutOfOrderPacketsIndependentlyForEachHand() {
        ReloadIntentServerGate gate = new ReloadIntentServerGate();
        assertTrue(gate.accept(new ReloadIntentMessage(false, true, 4, 10), 4,
                true, false, false, false));
        assertFalse(gate.accept(new ReloadIntentMessage(false, true, 4, 9), 4,
                true, false, false, false));
        assertTrue(gate.accept(new ReloadIntentMessage(true, true, 8, 1), 8,
                true, false, false, false));
        assertFalse(gate.accept(new ReloadIntentMessage(true, true, 8, 1), 8,
                true, false, false, false));
    }

    @Test
    void brokenOrOverheatedPacketsDoNotConsumeSequence() {
        ReloadIntentServerGate gate = new ReloadIntentServerGate();
        ReloadIntentMessage intent = new ReloadIntentMessage(false, true, 4, 7);
        assertFalse(gate.accept(intent, 4, true, false, true, false));
        assertFalse(gate.accept(intent, 4, true, false, false, true));
        assertTrue(gate.accept(intent, 4, true, false, false, false));
    }

    @Test
    void failedReloadStartCanRollBackTheAcceptedSequence() {
        ReloadIntentServerGate gate = new ReloadIntentServerGate();
        ReloadIntentMessage intent = new ReloadIntentMessage(false, true, 4, 7);

        assertTrue(gate.accept(intent, 4, true, false, false, false));
        gate.rollback(intent);
        assertTrue(gate.accept(intent, 4, true, false, false, false));
    }
}
