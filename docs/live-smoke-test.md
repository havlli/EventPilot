# Live Smoke Test

Use this checklist to validate EventPilot against a real Discord test server. Keep the server,
channel names, and users generic so the same run can also produce portfolio-safe screenshots.

Discord4J is 3.3.3. The offline component tests check JSON shape only; live Discord behaviour is
unverified by those tests.

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

The script checks Docker, `.env`, required variable names, and command resources. During normal runs,
it prints key names, not values; shell tracing (for example `bash -x` or inherited `set -x`) could
expose captured `.env` lines, so do not run it with tracing.

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
/event-options
/close-event
/reopen-event
/cancel-event
/delete-event
/clear-expired
```

## Optional Health Check

For a local application run, set `EVENTPILOT_HEALTH_ENABLED=true` before starting the bot.
Optionally set `EVENTPILOT_HEALTH_PORT` (default `8081`), then run on the same host:

```shell
curl --fail http://127.0.0.1:8081/health
```

Expect HTTP 200 and `{"status":"UP","discord":"UP","database":"UP"}` once Discord and the
database are available. HTTP 503 marks failing checks `DOWN` without error details. Confirm the
startup log shows `127.0.0.1`; with health disabled, no health listener should start. For container
runs, pass the variables through a local Compose environment override and run the probe inside the
container using an installed probe tool; no published port is needed. This can serve as a container
healthcheck. The supplied Compose file does not add one because the image may lack a probe tool.

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

## Event Reminder Options

Use an open or closed event whose reminder has not been sent. Replies should be ephemeral.

1. Run `/event-options message-id:<event-message-id>`.
   Expect `Reminder lead: 60 minutes (instance default). Reminder: not sent.` with the standard
   instance setting, or the configured lead (for example, `5` with the fast-reminder setup).
2. Run `/event-options message-id:<event-message-id> reminder-minutes:10`.
   Expect `Event options saved.` followed by
   `Reminder lead: 10 minutes (event override). Reminder: not sent.`
3. Show the options again; expect the same override without the saved line.
4. Run `/event-options message-id:<event-message-id> reset:true`.
   Expect `Event options saved.` followed by the instance default and `Reminder: not sent.`
5. Set a lead that includes the event's start time and wait for the scheduler interval.
   Expect one reminder. Change or reset the lead afterward; expect `Reminder: already sent.` and
   `The reminder was already sent; this change will not trigger another reminder.` No second
   reminder should appear.
6. Supply both `reminder-minutes:10` and `reset:true`.
   Expect `Use either reminder-minutes or reset:true, not both.`
7. Show a missing ID or an ID belonging to another server.
   Expect `Event not found in this server.` Trying to change it, or a cancelled or expired event,
   should return `Open or closed event not found in this server.`
8. Try the command as an ordinary player. Discord may hide it; if invoked, expect
   `You do not have permission to use this command.` and no change. Values outside 1–10080 should
   be rejected by Discord; if received by the bot, expect
   `Reminder minutes must be between 1 and 10080.`

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

## Channel obfuscation (2026-11-16)

From 2026-11-16, Discord will hide names of channels the bot cannot view: Gateway payloads carry
`___hidden___` and `CHANNEL_OBFUSCATED` (`1 << 17`), while `GET /guilds/{id}/channels` omits them.
Interaction payloads are not obfuscated. Impact is expected to be low: EventPilot uses interactions
and known destination IDs. Its destination string select uses the guild channel list from
`PromptService` (gateway store with REST fallback), which now filters `Channel.isObfuscated()`.
`DiscordService` uses `getChannelById` / `getMessageById` for known destination channels without
listing; other channel references come from interactions, including the selected destination ID.

TODO: complete this check before 2026-11-16 on a real test server.

- [ ] Enable **Private Channel Obfuscation** in the developer portal and give the bot view access
  to some test channels while denying it access to others. Reconnect the bot to refresh gateway state.
- [ ] Create an event; confirm the destination channel list shows only channels the bot can view,
  with no `___hidden___` entries, and confirm posting to a visible channel works.
- [ ] Confirm reminders still post to the saved destination channel.
- [ ] Run `/clear-expired` for an expired event and confirm cleanup still works.

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

As of this pull request (2026-10-11), no live Discord checks were run: Startup (bot online,
command visibility and permissions), Event Creation, Signup Lifecycle, Organizer Discovery,
Lifecycle Controls, Reminder, and Stale Delete Cleanup. No `.env`, bot token, or test server
was available; live runs were not authorized.
