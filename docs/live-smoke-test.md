# Live Smoke Test

Use this checklist to validate EventPilot against a real Discord test server. Keep the server,
channel names, and users generic so the same run can also produce portfolio-safe screenshots.

## Preconditions

- Docker is running.
- A Discord test server exists.
- The bot is invited with `bot` and `applications.commands` scopes.
- The bot can view channels, send messages, create embeds, read message history, and use slash
  commands in the test server.
- Organizer test users have `Manage Channels`; ordinary player test users do not.
- `.env` exists locally and is not committed.

Required local values:

```dotenv
DISCORD_BOT_TOKEN=...
JWT_SECRET=...
POSTGRES_DB=eventpilot
POSTGRES_USER=eventpilot
POSTGRES_PASSWORD=...
```

Optional fast-reminder values:

```dotenv
DISCORD_SCHEDULER_INTERVAL_SECONDS=10
DISCORD_REMINDER_LEAD_MINUTES=5
```

## Preflight

Run the local preflight before starting the bot:

```shell
scripts/live-smoke-preflight.sh
```

The script checks Docker, `.env`, required variable names, and command resources. It prints key names
only and never prints secret values.

## Startup

1. Start the app and dependencies:

```shell
docker compose up -d
```

2. Confirm the bot appears online in the test server.
3. Confirm slash commands are visible after Discord registration propagates.
4. Confirm ordinary members do not see organizer commands where Discord applies
   `default_member_permissions`.

Expected organizer commands:

```text
/create-event
/create-embed-type
/list-events
/event-info
/close-event
/reopen-event
/cancel-event
/delete-event
/clear-expired
```

## Event Creation

1. Run `/create-embed-type`.
2. Create a small gaming layout:

```json
{
  "-1": "Absence",
  "-2": "Late",
  "-3": "Tentative",
  "1": "Tank",
  "2": "Healer",
  "3": "Melee",
  "4": "Ranged",
  "5": "Support"
}
```

3. Run `/create-event`.
4. Create a near-future event with capacity `2`.
5. Post it into the test signup channel.

Expected result:

- event signup message is posted by the bot
- status is `Open`
- event ID is visible
- role buttons are visible
- roster count is `0/2`

## Signup Lifecycle

1. User A clicks a positive role such as Tank.
2. User B clicks a positive role such as Healer.
3. User C clicks a positive role after the event is full.

Expected result:

- User A and User B are confirmed
- roster count is `2/2`
- User C is waitlisted
- waitlist shows User C and their preferred role

Then:

1. User A changes to `Absence`, `Late`, or `Tentative`.

Expected result:

- User A moves to the non-capacity role
- User C is promoted from waitlist
- roster count remains accurate

## Organizer Discovery

1. Run `/list-events`.

Expected result:

- response is ephemeral
- only events from the current server are listed
- default filter includes open and closed events
- each row shows name, status, start time, channel, roster count, waitlist count, and message ID

2. Run `/list-events status:all limit:10`.

Expected result:

- cancelled and expired events appear when present
- no more than 10 events are shown

3. Run `/event-info message-id:<event-message-id>`.

Expected result:

- response is ephemeral
- details include status, start time, channel, leader, roster count, waitlist count, role groups,
  and waitlisted users

4. Run `/event-info message-id:<missing-id>`.

Expected result:

- response is `Event not found in this server.`

## Lifecycle Controls

1. Run `/close-event message-id:<event-message-id>`.
2. Click a signup button as a player.

Expected result:

- event status changes to `Closed`
- signup click receives an ephemeral blocked response

Then:

1. Run `/reopen-event message-id:<event-message-id>`.
2. Click a signup button as a player.

Expected result:

- event status changes to `Open`
- signup changes work again

Then:

1. Run `/cancel-event message-id:<event-message-id>`.

Expected result:

- event status changes to `Cancelled`
- signup clicks are blocked
- event history stays visible

## Reminder

1. Create or update an event inside the configured reminder lead window.
2. Wait for the scheduler interval.

Expected result:

- a reminder message is posted
- reminder includes start time, relative time, leader, status, confirmed count, waitlist count,
  grouped roster, and waitlist
- only one reminder is sent for the event

## Stale Delete Cleanup

1. Create an event.
2. Delete the Discord event message manually.
3. Run `/delete-event message-id:<deleted-message-id>`.

Expected result:

- response is `Event deleted!` if database state existed
- repeating the command returns `Event not found!`

## Automated Verification

Run after the live pass:

```shell
mise exec -- mvn -ntp -Dmaven.repo.local=.m2/repository test
mise exec -- mvn -ntp -Dmaven.repo.local=.m2/repository verify
```

`verify` requires Docker for Testcontainers.

## If Live Credentials Are Unavailable

Do not create or commit `.env` placeholders with real secrets. Leave this checklist as the live
acceptance script and rely on automated verification for the current pass.
