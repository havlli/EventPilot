# Architecture Decisions

This document records the main product and implementation decisions behind the current EventPilot
hardening pass. It is written for reviewers who want to understand why the bot behaves the way it
does, not just what classes exist.

## Product Boundary

EventPilot is scoped as a Discord-first event coordination bot for gaming groups. The primary product
surface is the Discord signup message, not the REST API. The REST/admin API remains useful for
supporting operations, but the core portfolio story is:

- an organizer creates a role-based event post
- players sign up or change role from Discord buttons
- the bot enforces capacity and waitlist rules
- lifecycle commands keep the event state clear
- scheduled reminders help the group show up prepared

This boundary keeps the project focused on a concrete target audience: raid leaders, guild officers,
party organizers, and community moderators.

## Signup Handling

Signup button IDs keep the existing generated format:

```text
{eventMessageId},{roleIndex}
```

This preserves compatibility with already-created event messages. Rather than attaching one reactive
subscription per event message, signup clicks are handled through one global `ButtonInteractionEvent`
path. The handler ignores unrelated button IDs, parses the two-part custom ID, and returns a reactive
chain to the shared command listener.

The mutation itself lives in `EventSignupService`. Keeping the signup rules in a domain service makes
them testable without Discord mocks and avoids stale state captured when a message was created.

## Database As Source Of Truth

Signup decisions read the current event from the database. This matters because Discord messages can
be old, users can click at the same time, and the process can restart between event creation and
signup.

The repository exposes a locked event lookup for signup mutation. The signup method is transactional
so capacity checks, role updates, waitlist changes, and saves happen as one unit of work.

Participant consistency is guarded at the database level with a unique `(event_id, user_id)`
constraint. Duplicate historical rows are removed by migration before the constraint is added.

## Capacity And Role Semantics

Event capacity applies to confirmed positive-role participants only.

Positive role indexes represent capacity-consuming roster roles such as Tank, Healer, Melee, Ranged,
or Support. Negative role indexes represent non-capacity states such as Absence, Late, or Tentative.

Rules:

- new positive-role signups are confirmed while capacity is available
- new positive-role signups are waitlisted when the event is full
- existing confirmed players can change positive roles even when the event is full
- existing non-capacity players who switch into a positive role are waitlisted when the event is full
- moving a confirmed player to a non-capacity role frees a slot
- removing a confirmed player frees a slot
- the earliest waitlisted positive-role participant is promoted when a slot opens

Positions are stable. Existing participants keep their original position, and new participants receive
`max(existing.position) + 1`. Waitlist promotion changes status, not position.

## Waitlist Model

Waitlists are modeled as participant state:

```text
SIGNED_UP
WAITLISTED
```

This avoids creating synthetic role indexes or changing the button ID contract. It also keeps
waitlist data visible through the same participant list and embed renderer.

Confirmed participants are grouped under their selected role in the event embed. Waitlisted
participants render in a separate waitlist field with their position and preferred role.

## Lifecycle States

Events have explicit lifecycle status:

```text
OPEN
CLOSED
CANCELLED
EXPIRED
```

Open events accept signups. Closed, cancelled, and expired events reject signup button clicks with an
ephemeral Discord response.

Close and reopen are operational controls for organizers. Cancel preserves the event record and
message history while clearly blocking further signup changes. Expired is used by scheduled cleanup
after the event start time has passed.

## Expired And Stale Event Cleanup

The scheduler finds open or closed events whose start time is in the past. Discord messages are
deactivated so old buttons stop looking actionable, and the database event is marked expired.

If a Discord message is already missing, the bot still marks the database event expired. This keeps
the database from repeatedly trying to operate on stale Discord state.

`/delete-event` has separate behavior for three cases:

- bot-owned Discord message exists: delete the message and delete database state
- Discord message is missing: delete stale database state if present
- message exists but is not bot-owned: keep not-authorized behavior and preserve database state

## Reminders

Reminders are scheduled from persisted event state. An event is a reminder candidate when:

- its start time is in the future
- its start time is inside the configured reminder lead window
- its status is open or closed
- `reminder_sent` is false

The reminder message includes start time, relative time, leader, status, confirmed count, waitlist
count, role groups, and waitlist. The bot marks `reminder_sent` only after Discord message creation
succeeds, so failed sends can be retried by the next scheduler cycle.

The default scheduler settings are:

```text
DISCORD_SCHEDULER_INTERVAL_SECONDS=60
DISCORD_REMINDER_LEAD_MINUTES=60
```

## Reactive Boundary

Discord I/O stays reactive. Blocking database work is wrapped onto bounded elastic schedulers where
it crosses into Discord event chains. Domain services remain ordinary Spring services with
transactional methods, which keeps business rules simple and directly testable.

The goal is pragmatic separation:

- Discord handlers parse inputs and compose reactive replies
- domain services mutate state
- repositories handle locking and persistence queries
- generators render Discord embeds and components

## Testing Strategy

The test suite is organized around risk:

- domain unit tests cover signup outcomes, waitlist promotion, lifecycle blocking, and invalid input
- command tests cover Discord interaction paths and ephemeral error behavior
- generator tests cover embed fields, roster counts, and waitlist rendering
- repository integration tests cover constraints, migrations, locking queries, reminder candidates,
  and status filtering
- journey tests cover authenticated REST/admin behavior
- Testcontainers runs PostgreSQL and Redis for integration verification

The intended acceptance command for a complete local verification is:

```shell
mvn -ntp -Dmaven.repo.local=.m2/repository verify
```
