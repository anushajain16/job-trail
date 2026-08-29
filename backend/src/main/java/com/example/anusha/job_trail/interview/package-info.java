/**
 * Interview prep tracking: one editable row per interview round logged
 * against an {@link com.example.anusha.job_trail.application.Application}
 * (Screen, Technical R1, Final, ...) — date, interviewer, the questions
 * asked, notes, and a post-interview reflection. Unlike
 * {@link com.example.anusha.job_trail.status.StatusHistory}, this is a
 * normal CRUD resource: rounds get refined after the fact as the user
 * writes up what happened, so there's no append-only constraint here.
 */
package com.example.anusha.job_trail.interview;
