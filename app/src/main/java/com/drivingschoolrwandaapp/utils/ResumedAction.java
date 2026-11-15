package com.drivingschoolrwandaapp.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/** Runs a UI action once after its screen resumes, or discards it if destroyed. */
public final class ResumedAction {
    private ResumedAction() { }

    public static void run(LifecycleOwner owner, Runnable action) {
        Lifecycle lifecycle = owner.getLifecycle();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) return;
        if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            action.run();
            return;
        }
        lifecycle.addObserver(new DefaultLifecycleObserver() {
            private boolean completed;

            @Override public void onResume(@NonNull LifecycleOwner owner) {
                if (completed) return;
                completed = true;
                lifecycle.removeObserver(this);
                action.run();
            }

            @Override public void onDestroy(@NonNull LifecycleOwner owner) {
                completed = true;
                lifecycle.removeObserver(this);
            }
        });
    }
}
