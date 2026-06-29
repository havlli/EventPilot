# Demo Capture Guide

Use this guide to capture a short portfolio demo of EventPilot in a Discord test server. The goal is
to show the complete gaming-session workflow without exposing secrets or private server data.

## Preparation

Create a dedicated Discord test server and invite the bot there. Use test accounts or trusted
friends for multi-user signup shots.

Recommended local settings for a fast reminder demo:

```dotenv
DISCORD_SCHEDULER_INTERVAL_SECONDS=10
DISCORD_REMINDER_LEAD_MINUTES=5
```

Start the app:

```shell
docker compose up -d
```

Or run only infrastructure and start the app from source:

```shell
docker compose -f src/main/resources/docker-compose-services.yml up -d
mvn -ntp -Dmaven.repo.local=.m2/repository spring-boot:run
```

Before recording, verify:

- the bot is online
- slash commands are registered
- the test channel is empty or easy to scan
- no private tokens, private channel names, or personal data are visible

## Suggested Demo Script

### 1. Create A Role Layout

Run:

```text
/create-embed-type
```

Use a simple raid/session layout:

```text
Tank
Healer
Melee
Ranged
Support
Late
Tentative
Absence
```

Capture:

- the guided private prompt flow
- the completed role layout confirmation

Portfolio point:

- organizers can define reusable signup structures without code changes

### 2. Create A Small Event

Run:

```text
/create-event
```

Suggested event data:

```text
Name: Friday Raid
Description: Normal mode clear and weekly vault run.
Capacity: 2
Date/time: a near-future time in the test server timezone
Destination channel: test signup channel
```

Capture:

- event creation prompts
- final event signup post
- role buttons
- event status field
- leader and event ID line

Portfolio point:

- the bot turns a Discord channel into a structured signup board

### 3. Fill The Event

Use two Discord users to click positive capacity roles such as Tank and Healer.

Capture:

- confirmed roster count reaching `2/2`
- users grouped under selected roles

Portfolio point:

- capacity is enforced from persisted state and reflected in the embed

### 4. Show Waitlist Behavior

Use a third user to click a positive role.

Capture:

- the third user appearing in the Waitlist field
- the roster count staying at `2/2`
- the preferred role shown next to the waitlisted user

Portfolio point:

- full events do not dead-end; overflow users are tracked as waitlisted

### 5. Show Promotion

Have one confirmed participant click `Absence`, `Late`, or `Tentative`.

Capture:

- the non-capacity participant moving out of the confirmed roster
- the earliest waitlisted participant promoted into the confirmed roster
- the waitlist shrinking

Portfolio point:

- waitlist promotion is automatic and keeps the roster usable for organizers

### 6. Show Lifecycle Controls

Run:

```text
/close-event message-id:<event-message-id>
```

Then try another signup click.

Capture:

- status changing to Closed
- ephemeral blocked-signup response

Run:

```text
/reopen-event message-id:<event-message-id>
```

Capture:

- status changing back to Open

Portfolio point:

- organizers can control whether signup changes are still accepted

### 7. Show Reminder

Create an event inside the configured reminder lead window or temporarily lower
`DISCORD_REMINDER_LEAD_MINUTES`.

Capture:

- reminder message
- start time and relative time
- leader
- event status
- confirmed count and waitlist count
- grouped roster and waitlist

Portfolio point:

- the bot is not only a signup form; it helps prepare the group before session start

### 8. Show Cleanup

Run one of:

```text
/cancel-event message-id:<event-message-id>
/delete-event message-id:<event-message-id>
/clear-expired
```

Capture:

- cancelled status or deleted signup post
- final command response

Portfolio point:

- the bot handles operational cleanup and stale event state

## Screenshot Checklist

Capture at least these still images if a GIF is too time-consuming:

- role layout prompt or confirmation
- initial event signup post
- full event with confirmed roles
- full event with waitlist
- promotion after a confirmed user switches to Absence, Late, or Tentative
- closed event status
- blocked signup ephemeral response
- reminder embed
- successful `mvn verify` terminal output

## Recording Tips

- Use a test server with generic names.
- Hide the member list if it contains real users.
- Do not show `.env`, bot tokens, JWT secrets, or Discord Developer Portal token pages.
- Keep the browser or Discord zoom level readable.
- Prefer one short end-to-end GIF plus two or three still screenshots.
- Crop to the Discord channel and terminal output that matter.

## Portfolio Caption

A concise caption for the demo:

```text
EventPilot coordinates Discord gaming sessions with role-based signups, capacity enforcement,
automatic waitlists, event lifecycle controls, and scheduled reminders. The implementation uses
Java, Spring Boot, Project Reactor, Discord4J, PostgreSQL, Flyway, Redis, and Testcontainers.
```
