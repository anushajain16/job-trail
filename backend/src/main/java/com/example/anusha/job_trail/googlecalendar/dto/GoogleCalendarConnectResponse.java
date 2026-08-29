package com.example.anusha.job_trail.googlecalendar.dto;

/** The frontend navigates the whole browser here (not a fetch) — Google's
 * own consent screen needs to render, which an XHR/fetch response can't do. */
public record GoogleCalendarConnectResponse(String authorizationUrl) {
}
