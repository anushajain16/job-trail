package com.example.anusha.job_trail.status;

/**
 * The pipeline a job application moves through. {@link #SAVED} is where
 * every application starts; {@link #REJECTED} and {@link #GHOSTED} are the
 * two ways out that aren't {@link #OFFER}. Nothing here enforces a linear
 * order — an application can jump straight from SAVED to OFFER, or from
 * INTERVIEW to GHOSTED — the history log just records whatever actually
 * happened.
 */
public enum Stage {
    SAVED,
    APPLIED,
    SCREEN,
    INTERVIEW,
    FINAL,
    OFFER,
    REJECTED,
    GHOSTED
}
