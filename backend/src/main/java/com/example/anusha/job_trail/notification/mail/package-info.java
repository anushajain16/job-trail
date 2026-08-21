/**
 * The one place anything provider-specific about outbound email is allowed
 * to live. Callers (e.g. the scheduler's reminder sweep) depend only on
 * {@link com.example.anusha.job_trail.notification.mail.EmailSender} — an
 * interface with no vendor, SDK, or protocol in its signature — so swapping
 * {@link com.example.anusha.job_trail.notification.mail.SmtpEmailSender}
 * for, say, a provider-specific HTTP API later touches this package only.
 */
package com.example.anusha.job_trail.notification.mail;
