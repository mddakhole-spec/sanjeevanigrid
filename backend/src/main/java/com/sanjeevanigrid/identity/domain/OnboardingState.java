package com.sanjeevanigrid.identity.domain;

/**
 * Onboarding state machine states.
 * Transitions:
 *   REGISTERED -> VERIFIED (after OTP/email verification)
 *   VERIFIED -> ACTIVE (after admin approval for restricted roles, or auto for CITIZEN)
 *   VERIFIED -> REJECTED (admin rejects)
 *   ACTIVE -> SUSPENDED (admin suspends)
 *   SUSPENDED -> ACTIVE (admin reinstates)
 */
public enum OnboardingState {
    REGISTERED,
    VERIFIED,
    ACTIVE,
    REJECTED,
    SUSPENDED;

    /**
     * Validates whether a transition from this state to the target state is legal.
     */
    public boolean canTransitionTo(OnboardingState target) {
        return switch (this) {
            case REGISTERED -> target == VERIFIED;
            case VERIFIED -> target == ACTIVE || target == REJECTED;
            case ACTIVE -> target == SUSPENDED;
            case SUSPENDED -> target == ACTIVE;
            case REJECTED -> false;
        };
    }

    /**
     * Performs the transition, throwing if invalid.
     */
    public OnboardingState transitionTo(OnboardingState target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                "Invalid state transition: " + this + " -> " + target
            );
        }
        return target;
    }
}
