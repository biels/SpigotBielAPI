# Shared event routing review

Scope: Bukkit registrations, nested dispatch, bus registration/destruction, world and
player filtering, projectile impacts, and the Minicat minion attribution consumers.
Baseline: `beb5a2e`. These changes require a shared-API review and live regression
check before integration; no server restart or deployment was performed.

## Findings and fixes

- **Duplicate registrations:** death, damage, combustion, targeting, block changes,
  item despawn and item drops had overlapping handlers. Removed the redundant
  registrations, retaining the broad handler when subclasses share its Bukkit
  handler list. Disjoint event types remain registered separately. Weak identity
  deduplication remains for explicit/reentrant delivery, not as the normal fix.
- **Unregistration races:** pending registrations could not be cancelled, and an
  unregistered bus still ran from an existing dispatch snapshot. Registration is
  now an ordered set with immediate removal; each snapshot entry is checked before
  delivery. Nested registration takes effect for subsequent/nested events without
  inserting recipients into the current outer snapshot. Removal resets the bus's
  registration flag so a valid bus can explicitly register again.
- **Destruction bypass:** overriding validity with only a loaded-world check let a
  destroyed bus receive events or a deferred respawn callback. Destruction is now
  independently checked at delivery and at the deferred callback, and world-bus
  validity includes destruction.
- **World leakage:** holderless menus and block inventories fell through to every
  world's inventory listeners. Inventory events now route by their viewer's world.
- **Participant filtering:** reflection returned after the first unsuccessful
  branch and did not recognize `ProjectileSource` getters. Game spectator checks
  and player skill buses now share one traversal. All event participant branches
  are considered; projectile shooters, inventory viewers and actual death killers
  are handled explicitly. Entity targets, vehicles and historical killers are not
  followed as if they participated in unrelated events. Traversal remains bounded
  and uses identity tracking for nested event graphs.
- **Guessed projectile impacts:** the API traced ahead instead of using the event's
  hit block, potentially breaking RainbowClay glass behind an entity impact.
  Block callbacks now use `ProjectileHitEvent.getHitBlock()` exclusively, preserving
  the existing player-shot restriction.
- **Dropped block-form hooks:** `EntityBlockFormEvent` was checked inside the
  unrelated `EntityEvent` branch. Both ordinary and entity-driven formation hooks
  now dispatch from the block-event branch.

## Verification

- Java 25: `mvn -q -Dmaven.compiler.fork=true install`, all 14 tests pass,
  including 10 new routing regressions and the existing nested-delivery tests.
- The initial eight-test baseline comparison produced five assertion failures;
  three additional old-code paths hit fixture limitations (plugin logging and
  the obsolete ray trace). Those three are source-backed findings, not claimed
  as standalone behavioral reproductions on a running server.
- Clean Minicat `4185374`: `./gradlew build --refresh-dependencies` against the
  fixed local API, including minion ownership, retreat/cadence, upgrades and guide
  checks. Unrelated canonical InkWars edits are excluded from this verification.

## Review and live gate

Before integration/deployment, exercise death rewards once under nested spawns,
projectile owner credit after shooter death, teardown during nested callbacks,
menus in two concurrent worlds, spectator/player skill filtering, exact glass
impacts in RainbowClay, and ordinary/hero snowman retreat in Obsidian Defenders.
The pending snowman change is separate from this shared-API review.

The audit does not certify every legacy event hook or every external plugin's
event-priority interaction. Minicat already commits Obsidian combat credit in a
non-cancelled damage MONITOR handler; this review does not move credit earlier or
make minion shots impersonate player projectiles. Prismarine protocol decoding,
console-command acknowledgement timing, client entity tracking distance and the
intended spawn/AFK immunity are separate concerns, not reasons to weaken game rules.
