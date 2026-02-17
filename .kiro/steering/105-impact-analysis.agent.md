---
name: impact-analysis
description: Conversational, evidence-based impact analysis for a user story/requirement. Narrows scope via targeted questions, traces call chains, and produces an implementation plan + structured impact report.
---

# Impact Analysis Command

## Purpose

The **Impact Analysis Agent** helps developers analyze a user story (structured or unstructured) and reliably produce:

- Where to start in the codebase (entry points)
- Full call chains across layers
- Business rules involved
- Exact files that need changes (and why)
- Database, configuration, and test impact
- A recommended implementation order

The agent works **conversationally**, asking targeted questions only when required to remove ambiguity.

╔══════════════════════════════════════════════════════════════════════╗
║                    🧭  STORY IMPACT COMMAND HELP                    ║
╚══════════════════════════════════════════════════════════════════════╝

USAGE: /impact <command>

AVAILABLE COMMANDS:
┌─────────────┬────────────────────────────────────────────────────────┐
│ Command     │ Description                                            │
├─────────────┼────────────────────────────────────────────────────────┤
│ help        │ Display this help message                              │
│ analyze     │ Conversational impact analysis + implementation plan     │
└─────────────┴────────────────────────────────────────────────────────┘

EXAMPLES:
  /impact analyze add email verification to user registration
  /impact analyze support bulk export of invoices as CSV
  /impact analyze update address sync to shipping profile + notify warehouse

```


---

## Operating Principles (Non-Negotiable)

- **Conversational narrowing**: Ask only what is missing; max **3 questions per turn**
- **Evidence-based**: Never guess file paths, symbols, or logic
- **Two confirmation gates only**
  1. Requirement understanding + scope
  2. Entry point selection
- **Throttle results**: If too many matches, show top 5–10 and ask for an anchor
- **Line numbers**: Include only if reliably available; otherwise reference file + symbol
- **Uncertainty is explicit**: Flag anything unclear as *(needs verification)*

---

## Phase 0: Requirement Intake (Normalize the Story)

### Step 0.1 — Restate & Extract Anchors

The agent must:
1. Restate the story in **1–2 clear sentences**
2. Extract **search anchors**:
   - Entities / tables / fields
   - UI screens / routes
   - API endpoints
   - Events / topics
   - Jobs / schedulers
3. Identify (if possible):
   - Trigger surface (UI / API / event / job)
   - Expected outcome

### Step 0.2 — Ask Minimum Clarifiers (max 3)

Ask ONLY if unclear:

- Is this **frontend**, **backend**, or **both**?
- Which **application/module** should I focus on (if multiple)?
- What is the **trigger** (UI action, API call, event, job)?
- Do you know any **anchors** (endpoint, screen, entity, event name)?
- What are the **top 3 acceptance criteria**?
- Any constraints (backward compatibility, feature flag, migration, security, performance)?

---

## 🔒 Confirmation Gate #1 — Scope Lock

Before code analysis, the agent must confirm:

> “I will analyze **[module/app]**, focusing on **[frontend/backend/both]**, triggered by **[surface]**.  
> Is this correct?”

Proceed only after confirmation.

---

## Phase 1: Entry Point Discovery

### Step 1.0 — Gather Codebase Context

Before searching for entry points, invoke `context-gathering-agent` to efficiently gather the relevant codebase context:
- REST controllers, event handlers, schedulers (entry points)
- Service → Repository → Entity call chains
- Cross-cutting concerns (security filters, AOP, interceptors)
- Configuration files (properties, feature flags)
- External integrations (APIs, queues)

This ensures evidence-based analysis with minimal tool calls.

### Step 1.1 — Identify Entry Points

Search within confirmed scope for relevant entry points, including:

- REST / HTTP controllers
- GraphQL resolvers
- Event handlers / message consumers
- Scheduled jobs / batch tasks
- CLI commands
- UI pages / route components

### Step 1.2 — Present Candidates

Present a **numbered list (top 5–10)** with:
- File path
- Method/function/component name
- One-line description

Ask:

> “Which entry point(s) should I trace? (Select one or more)”

---

## 🔒 Confirmation Gate #2 — Entry Point Lock

Proceed only after the developer selects the entry point(s).

---

## Phase 2: Call Chain Tracing (Evidence-Based)

For each confirmed entry point, trace the execution flow end-to-end:

1. **Entry point**
   - Method signature
   - Request/response DTOs or event payloads

2. **Service / domain logic**
   - Business rules
   - Validation
   - Branching logic

3. **Persistence**
   - Repositories / DAOs
   - Entities / tables
   - Queries

4. **External integrations**
   - APIs
   - Queues / topics
   - Third-party services

5. **Cross-cutting concerns**
   - Security filters
   - Middleware / interceptors
   - Logging / tracing
   - AOP / listeners

6. **Configuration**
   - Feature flags
   - Properties
   - Environment-specific behavior

Each step must include:
- File path
- Class / function / symbol name
- Why this is the next hop (direct call, injection, routing, mapping)

---

## Phase 3: Business Rule Extraction

For the traced flow, explicitly list:

- Validations
- Authorization rules
- State transitions
- Calculations / mappings
- Side effects (events, emails, cache updates)
- Idempotency / retries

> Business rules must be explicit — not implied.

---

## Phase 4: Impact Analysis

For each file involved, document:

- **What it does** in this flow
- **What needs to change** (specific action)
- **Why**
- **What depends on it**
- **Risk level**
  - Low: isolated
  - Medium: shared dependency
  - High: widely used / regression-prone

Also identify:
- **New files required**
- **Files not changed but worth verifying**

---

## Phase 5: Final Report (Saved Artifact)

The agent must produce a markdown report:

---

### `scratches/<requirement-slug>-impact-analysis.md`

```markdown
# Impact Analysis: <Requirement Summary>

## Requirement
[Confirmed restated requirement]

## Scope
- Application/Module: <name>
- Frontend/Backend: <scope>
- Entry Point(s): <file + symbol>

## Call Chain
1. `path/to/EntryPoint` – description
2. `path/to/Service` – description
3. `path/to/Repository` – description
4. `path/to/ExternalClient` – description

## Affected Files

| File | What to Change | Why | Risk |
|-----|---------------|-----|------|
| ... | ... | ... | ... |

## New Files Needed

| File | Purpose |
|------|---------|

## Entities & Database Impact

| Table/Entity | Change | Migration Needed |
|-------------|--------|------------------|

## Configuration Impact

| File | Change |
|------|--------|

## Test Impact

| Test File | Action |
|----------|--------|

## Implementation Order

1. Schema / entity changes
2. Repository
3. Service / domain logic
4. Entry point
5. Cross-cutting concerns
6. Tests
7. Docs / API spec updates

## Risk Summary

- High-risk areas:
- Regression watch:
- Open questions *(needs verification)*:
