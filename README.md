# DISM

**Dynamic Intelligent Schedule Manager**

*A local-first personal semantic state & adaptive scheduling engine.*

DISM is a local-first personal information engine that maintains the changing state and meaning of a user's life instead of treating schedules and tasks as static records.

> The user is responsible for living.
> DISM is responsible for maintaining the state of life.

## Why DISM?

Traditional task and calendar applications mostly store explicit records:

* tasks
* events
* schedules
* reminders
* notes

DISM focuses on what happens **between those records**.

For example:

> "Wednesday morning I have exercise, so I need to bring my power bank."

A conventional task manager may store the reminder.

DISM can model:

```text
Wednesday morning
        ↓
Exercise
        ↓
requires
        ↓
Power bank
        ↓
Power bank currently at dorm
        ↓
Need to bring it before leaving
```

The important information is not only the task itself, but the changing relationships and states around it.

---

## Core Model

DISM is built around five core concepts:

```text
Entity
   │
   ├── Relation
   │       │
   │       └── Evidence
   │
   └── State
           │
           ▼
        Event
           │
           ▼
   Dynamic Schedule
           │
           ▼
      Attention
           │
           ▼
      Adaptation
```

### Entity

An entity represents something that exists in the user's world.

Examples:

* person
* place
* item
* course
* activity
* schedule
* organization

### Relation

A relation describes how entities are connected.

Examples:

```text
PowerBank
    ──located_at──>
Dorm

Exercise
    ──requires──>
PowerBank
```

Relations are not assumed to remain permanently true.

They may be:

```text
ACTIVE
SUPERSEDED
INVALIDATED
```

### State

State describes what is currently believed to be true.

State is dynamic.

For example:

```text
PowerBank
    located_at
Dorm
```

may later become:

```text
PowerBank
    located_at
Backpack
```

The old state is not simply overwritten; its validity and evidence can be preserved.

### Event

Events represent changes observed in reality.

Conceptually:

```text
Reality
   ↓
Observation
   ↓
Event
   ↓
State transition
```

### Evidence

Evidence explains why DISM believes something.

This allows the system to distinguish:

```text
known
```

from:

```text
inferred
```

and:

```text
uncertain
```

---

# Dynamic Semantics

DISM does not treat natural-language input as the final data model.

Instead:

```text
Natural language
       ↓
Semantic observation
       ↓
Structured state
       ↓
Relations
       ↓
Dynamic meaning
```

Natural language is therefore a weakly typed user interface.

The internal model remains structured.

---

# Dynamic Scheduling

A DISM schedule is not simply a list sorted by time.

A schedule can respond to:

* current state
* temporal constraints
* priority
* confidence
* dependencies
* recent observations
* user feedback
* historical behavior

Conceptually:

```text
Current reality
      +
Future events
      +
Constraints
      +
Dynamic weights
      ↓
Schedule evaluation
      ↓
Decision
```

---

# Attention

DISM may know much more than it needs to show.

The Attention system exists to prevent the internal world model from becoming a constant stream of notifications.

```text
Rich internal state
        ↓
Attention evaluation
        ↓
User-relevant information
        ↓
Minimal presentation
```

The objective is not to tell the user everything DISM knows.

It is to tell the user what matters now.

---

# Adaptation

User feedback can change how the system evaluates future situations.

The general loop is:

```text
Reality
  ↓
Observation
  ↓
State
  ↓
Schedule
  ↓
Recommendation
  ↓
User feedback
  ↓
Adaptation
  ↓
Future evaluation
```

This makes DISM progressively more context-aware without requiring the user to manually maintain every rule.

---

# Local First

DISM is designed around local-first operation.

The core engine should not fundamentally depend on:

* cloud AI
* a specific LLM
* a specific calendar provider
* a specific external service

External services are providers.

The semantic state belongs to DISM.

```text
External Provider
       ↓
Provider Observation
       ↓
DISM Core
       ↓
Semantic State
```

---

# Project Structure

The project is organized around several architectural boundaries:

```text
core/
├── domain/
├── entity/
├── relation/
├── state/
├── event/
├── evidence/
├── semantic/
├── schedule/
├── attention/
├── persistence/
├── provider/
├── runtime/
└── api/
```

The exact internal implementation may change.

The supported Public API is the stable product boundary.

---

# Public API

Applications should depend on:

```text
me.moiclathy.dism.api
```

and supported product-level facades.

Applications should not directly depend on:

```text
repositories
SQLDelight
database drivers
internal runtime implementations
schedule engines
adaptation internals
```

Internal implementation is allowed to change without requiring applications to understand it.

---

# Example

Conceptually, an application should look like:

```kotlin
val dism = /* platform/application bootstrap */

val runtime = dism.runtime

val snapshot = runtime.snapshot()
```

The application consumes DISM through its public contract rather than constructing individual internal engines.

---

# Design Principle

DISM follows a simple separation:

```text
User
  ↓
describes reality

DISM
  ↓
maintains meaning

Application
  ↓
presents useful information
```

The goal is not to create another task list.

The goal is to maintain a continuously evolving semantic model of personal life.

---

## Documentation

### Architecture

* [Overview](docs/architecture/overview.md)
* [Semantic Model](docs/architecture/semantic-model.md)
* [Dynamic Schedule](docs/architecture/dynamic-schedule.md)
* [Attention](docs/architecture/attention.md)
* [Adaptation](docs/architecture/adaptation.md)
* [Persistence](docs/architecture/persistence.md)

### Public API

* [Getting Started](docs/api/getting-started.md)
* [Runtime](docs/api/runtime.md)
* [Schedule](docs/api/schedule.md)
* [Configuration](docs/api/configuration.md)
* [Errors](docs/api/errors.md)
* [Diagnostics](docs/api/diagnostics.md)
* [Providers](docs/api/providers.md)

### UI

* [Architecture](docs/ui/architecture.md)
* [Screens](docs/ui/screens.md)
* [Calendar Import](docs/ui/calendar-import.md)
* [Decision History](docs/ui/decision-history.md)
* [Accessibility](docs/ui/accessibility.md)
* [Responsive Layout](docs/ui/responsive-layout.md)
* [Web Persistence](docs/ui/web-persistence.md)
* [Performance](docs/ui/performance.md)

### Developer

* [Contributing](docs/developer/contributing.md)
* [Architecture Rules](docs/developer/architecture-rules.md)
* [Testing](docs/developer/testing.md)
* [Compatibility](docs/developer/compatibility.md)
* [Adding a Provider](docs/developer/adding-a-provider.md)

### Release

* [16.36 GitHub Release](docs/release/16.36-github-release.md) — release notes for the
  packaged build; what is and is not verified
* [16.36 GitHub Release Commands](docs/release/16.36-github-release-commands.md) —
  build / test / run / upload commands, each one executed in this repository
* [16.14 Platform Integration](docs/release/16.14-platform-integration.md)
* [15.14 Release Candidate Checklist](docs/release/15.14-release-candidate-checklist.md)
* [15.15 Final Release Gate](docs/release/15.15-final-release-gate.md)

### Working notes

* [Handoff](docs/HANDOFF.md) — current state, open gaps, operational
  pitfalls. Read this first when resuming work.

---

## Status

DISM is currently under active development.

The 15.x series focuses on:

* Public API boundaries
* configuration
* persistence abstraction
* diagnostics
* provider boundaries
* platform entry points
* API compatibility
* documentation
* release preparation

Internal APIs may change during this phase.

Public API compatibility is tracked separately from internal implementation.
