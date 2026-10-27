package com.sanjeevanigrid.identity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the OnboardingState state machine.
 * Tests all valid and invalid transitions.
 */
class OnboardingStateTest {

    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("REGISTERED -> VERIFIED")
        void registeredToVerified() {
            OnboardingState result = OnboardingState.REGISTERED.transitionTo(OnboardingState.VERIFIED);
            assertEquals(OnboardingState.VERIFIED, result);
        }

        @Test
        @DisplayName("VERIFIED -> ACTIVE")
        void verifiedToActive() {
            OnboardingState result = OnboardingState.VERIFIED.transitionTo(OnboardingState.ACTIVE);
            assertEquals(OnboardingState.ACTIVE, result);
        }

        @Test
        @DisplayName("VERIFIED -> REJECTED")
        void verifiedToRejected() {
            OnboardingState result = OnboardingState.VERIFIED.transitionTo(OnboardingState.REJECTED);
            assertEquals(OnboardingState.REJECTED, result);
        }

        @Test
        @DisplayName("ACTIVE -> SUSPENDED")
        void activeToSuspended() {
            OnboardingState result = OnboardingState.ACTIVE.transitionTo(OnboardingState.SUSPENDED);
            assertEquals(OnboardingState.SUSPENDED, result);
        }

        @Test
        @DisplayName("SUSPENDED -> ACTIVE (reinstatement)")
        void suspendedToActive() {
            OnboardingState result = OnboardingState.SUSPENDED.transitionTo(OnboardingState.ACTIVE);
            assertEquals(OnboardingState.ACTIVE, result);
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("REGISTERED -> ACTIVE (must go through VERIFIED)")
        void registeredToActive() {
            assertThrows(IllegalStateException.class, () ->
                OnboardingState.REGISTERED.transitionTo(OnboardingState.ACTIVE));
        }

        @Test
        @DisplayName("REGISTERED -> REJECTED (can only reject from VERIFIED)")
        void registeredToRejected() {
            assertThrows(IllegalStateException.class, () ->
                OnboardingState.REGISTERED.transitionTo(OnboardingState.REJECTED));
        }

        @Test
        @DisplayName("REGISTERED -> SUSPENDED")
        void registeredToSuspended() {
            assertThrows(IllegalStateException.class, () ->
                OnboardingState.REGISTERED.transitionTo(OnboardingState.SUSPENDED));
        }

        @Test
        @DisplayName("VERIFIED -> SUSPENDED (must go through ACTIVE)")
        void verifiedToSuspended() {
            assertThrows(IllegalStateException.class, () ->
                OnboardingState.VERIFIED.transitionTo(OnboardingState.SUSPENDED));
        }

        @Test
        @DisplayName("REJECTED -> anything (terminal state)")
        void rejectedIsTerminal() {
            assertFalse(OnboardingState.REJECTED.canTransitionTo(OnboardingState.REGISTERED));
            assertFalse(OnboardingState.REJECTED.canTransitionTo(OnboardingState.VERIFIED));
            assertFalse(OnboardingState.REJECTED.canTransitionTo(OnboardingState.ACTIVE));
            assertFalse(OnboardingState.REJECTED.canTransitionTo(OnboardingState.SUSPENDED));
        }

        @Test
        @DisplayName("ACTIVE -> VERIFIED (no backward transitions)")
        void activeToVerified() {
            assertThrows(IllegalStateException.class, () ->
                OnboardingState.ACTIVE.transitionTo(OnboardingState.VERIFIED));
        }

        @Test
        @DisplayName("ACTIVE -> REGISTERED (no backward transitions)")
        void activeToRegistered() {
            assertThrows(IllegalStateException.class, () ->
                OnboardingState.ACTIVE.transitionTo(OnboardingState.REGISTERED));
        }

        @Test
        @DisplayName("Self-transition is invalid")
        void selfTransition() {
            for (OnboardingState state : OnboardingState.values()) {
                assertThrows(IllegalStateException.class, () ->
                    state.transitionTo(state),
                    state + " -> " + state + " should be invalid");
            }
        }
    }

    @Nested
    @DisplayName("canTransitionTo validation")
    class CanTransitionTo {

        @Test
        @DisplayName("canTransitionTo returns true for valid transitions")
        void validChecks() {
            assertTrue(OnboardingState.REGISTERED.canTransitionTo(OnboardingState.VERIFIED));
            assertTrue(OnboardingState.VERIFIED.canTransitionTo(OnboardingState.ACTIVE));
            assertTrue(OnboardingState.VERIFIED.canTransitionTo(OnboardingState.REJECTED));
            assertTrue(OnboardingState.ACTIVE.canTransitionTo(OnboardingState.SUSPENDED));
            assertTrue(OnboardingState.SUSPENDED.canTransitionTo(OnboardingState.ACTIVE));
        }

        @Test
        @DisplayName("canTransitionTo returns false for invalid transitions")
        void invalidChecks() {
            assertFalse(OnboardingState.REGISTERED.canTransitionTo(OnboardingState.ACTIVE));
            assertFalse(OnboardingState.ACTIVE.canTransitionTo(OnboardingState.VERIFIED));
        }
    }

    @Nested
    @DisplayName("Full lifecycle")
    class FullLifecycle {

        @Test
        @DisplayName("Happy path: REGISTERED -> VERIFIED -> ACTIVE -> SUSPENDED -> ACTIVE")
        void happyPathWithSuspension() {
            OnboardingState state = OnboardingState.REGISTERED;
            state = state.transitionTo(OnboardingState.VERIFIED);
            state = state.transitionTo(OnboardingState.ACTIVE);
            state = state.transitionTo(OnboardingState.SUSPENDED);
            state = state.transitionTo(OnboardingState.ACTIVE);
            assertEquals(OnboardingState.ACTIVE, state);
        }

        @Test
        @DisplayName("Rejection path: REGISTERED -> VERIFIED -> REJECTED (terminal)")
        void rejectionPath() {
            OnboardingState state = OnboardingState.REGISTERED;
            state = state.transitionTo(OnboardingState.VERIFIED);
            state = state.transitionTo(OnboardingState.REJECTED);
            assertEquals(OnboardingState.REJECTED, state);

            // Verify it's terminal
            OnboardingState finalState = state;
            assertThrows(IllegalStateException.class, () ->
                finalState.transitionTo(OnboardingState.ACTIVE));
        }
    }
}
