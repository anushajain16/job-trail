package com.example.anusha.job_trail.googlecalendar;

import java.util.List;

/** {@code useDefault: false} plus an explicit override list is how Calendar
 * v3 expresses "this event's reminder differs from the calendar's own
 * defaults" — omitting {@code overrides} entirely, rather than sending an
 * empty list, is how it's told "no reminder at all" for this event. */
record GoogleEventReminders(boolean useDefault, List<GoogleEventReminderOverride> overrides) {
}
