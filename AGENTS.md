# AGENTS.md — GameNest Project Instructions

## 1. Project Identity

**Project name:** GameNest  
**Project type:** Gaming Community Platform  
**Purpose:** Build a community website where users can select games, ask questions, discuss games, find other players, and optionally create/join teams.

GameNest is **not** intended to be a full game launcher, game marketplace, or game-streaming platform.

The core idea is:

```text
Game
 ├── Questions & Answers
 ├── Comments & Discussion
 ├── Find Players (LFG)
 └── Teams / Community
```

---

## 2. Development Role

Codex is responsible for **implementing** the system based on the project's approved business requirements and architecture.

The project owner is responsible for product decisions.

Before implementing a new feature:

1. Understand the business requirement.
2. Check existing architecture and database structure.
3. Check whether the feature conflicts with an existing rule.
4. Identify affected modules, tables, permissions, and flows.
5. Implement only what is required.
6. Do not invent major business rules without confirmation.

When a requirement is ambiguous and the ambiguity materially affects data integrity, security, or business behavior, stop and ask for clarification instead of guessing.

For minor implementation details, use reasonable engineering judgment.

---

## 3. Technology Stack

The intended backend stack is:

- Java
- Jakarta Servlet / Jakarta EE APIs
- Apache Tomcat
- Maven
- JDBC
- Microsoft SQL Server
- HTML / CSS / JavaScript
- JSP may be used for server-side rendering when appropriate

Follow the existing project structure rather than introducing another framework unnecessarily.

Do not replace the established stack with Spring Boot, Node.js, Python, PHP, or another backend framework unless explicitly requested.

---

## 4. Architecture Principles

Prefer a layered architecture:

```text
Browser
   ↓
Servlet / Controller
   ↓
Service
   ↓
DAO
   ↓
JDBC
   ↓
SQL Server
```

Responsibilities:

### Servlet / Controller
- Receive HTTP requests.
- Validate basic request parameters.
- Handle authentication/authorization checks where appropriate.
- Call the Service layer.
- Decide the response/redirect.

Do not put complex business logic directly inside Servlets.

### Service
- Business rules.
- Validation that depends on business state.
- Transaction boundaries when required.
- Coordination between multiple DAOs.
- Prevent invalid state transitions.

### DAO
- Database access.
- SQL/JDBC operations.
- Mapping ResultSet data to Java objects.
- No UI logic.
- No unrelated business rules.

### JSP / Frontend
- Presentation.
- Form handling.
- Displaying server-provided data.
- Client-side interaction.

Do not move important authorization or business rules exclusively into JavaScript.

---

## 5. Core GameNest Modules

The initial system is centered around these modules:

1. Accounts
2. Games
3. Questions
4. Answers
5. Comments
6. Votes
7. LFG (Looking For Group)
8. Teams
9. Notifications
10. Reports
11. Reputation
12. Tags / Genres

Not every module must be implemented immediately.

Prioritize the core flow before adding secondary features.

---

## 6. Core Database

The initial core database should revolve around:

```text
Accounts
Games
Questions
Answers
LFGPosts
LFGMembers
```

Important relationships:

```text
Accounts ──────< Questions >────── Games
   │                │
   │                └────< Answers
   │
   └──────────────< LFGPosts >──── Games
                         │
                         └────< LFGMembers >──── Accounts
```

Additional modules can later add:

```text
Comments
QuestionVotes
AnswerVotes
Genres
GameGenres
Tags
QuestionTags
Teams
TeamMembers
TeamApplications
Notifications
Reports
Reputations
```

Do not expand the schema unnecessarily just because a feature could theoretically exist.

---

## 7. Database Safety Policy

### 7.1 Hard Delete Policy

GameNest follows this rule:

> Core business data and user-generated community content must NOT be hard-deleted during normal application operations.

Hard Delete means executing SQL such as:

```sql
DELETE FROM Questions WHERE question_id = ?;
```

Application-level code must not use Hard Delete for core business records.

### 7.2 Prefer Soft Delete / Status

For user-generated content, prefer fields such as:

```text
is_deleted
deleted_at
deleted_by
```

or an appropriate:

```text
status
```

Examples:

```text
Questions:
ACTIVE / HIDDEN / DELETED / LOCKED

Accounts:
ACTIVE / BANNED / DELETED / SUSPENDED

Games:
ACTIVE / INACTIVE

LFGPosts:
OPEN / FULL / CLOSED / EXPIRED / DELETED

Teams:
ACTIVE / INACTIVE / DISBANDED
```

Do not invent status values inconsistently. Reuse the project's established values.

### 7.3 Junction Tables

Hard Delete can be appropriate for relationship/state tables where the row represents a current relationship rather than historical business content.

Examples:

```text
QuestionTags
GameGenres
LFGMembers
TeamMembers
QuestionVotes
AnswerVotes
```

For example, removing a user's current vote may legitimately delete the vote row.

### 7.4 Historical Data

Historical records should normally be retained.

Examples:

```text
Reports
Reputations
Audit Logs
```

Do not remove historical records simply to clean up the UI.

---

## 8. SQL Server Security Model

The application database account must follow the principle of least privilege.

Expected concept:

```text
SQL Server
│
├── GameNestAppLogin
│       ↓
│   GameNestAppUser
│       ↓
│   GameNestAppRole
│
└── GameNestDB
```

The application login must NOT be:

```text
sysadmin
db_owner
```

or another unrestricted administrative principal.

The application role should normally have only the permissions required by the application, such as:

```text
SELECT
INSERT
UPDATE
EXECUTE
```

and must not have normal application permission to:

```text
DELETE
DROP
ALTER
```

Hard Delete protection should exist at the SQL Server permission layer as an additional defense.

The application must not depend solely on Java code to prevent destructive SQL.

A separate DBA/developer account may retain full database administration privileges for schema changes, migrations, backup/restore, and maintenance.

---

## 9. Data Integrity

Protect database integrity with:

- Primary keys
- Foreign keys
- UNIQUE constraints
- NOT NULL where appropriate
- CHECK constraints where appropriate
- Appropriate indexes
- Correct data types
- Transactions for multi-step operations

Do not rely exclusively on Java validation when a rule can safely and meaningfully be enforced by SQL Server.

Examples:

```text
One account cannot vote multiple times on the same answer.
An LFG cannot exceed max_players.
A username/email that must be unique must have a UNIQUE constraint.
A relationship row must reference an existing parent row.
```

---

## 10. LFG Business Rules

LFG means **Looking For Group**.

An LFG post can contain information such as:

```text
Game
Creator
Title
Description
Game mode
Required rank
Region
Maximum players
Current players
Start time
Status
```

Important rules:

1. `current_players` must never exceed `max_players`.
2. A user should not join the same LFG more than once.
3. A closed/full/expired LFG must reject new joins.
4. Joining an LFG and increasing its member count must be handled safely when concurrent requests are possible.
5. The creator should be treated consistently as a participant according to the approved business rule.
6. Leaving an LFG must not produce negative player counts.
7. LFG state transitions must be validated in the Service layer.

When concurrent joins can occur, use an appropriate SQL transaction/locking strategy rather than relying only on application-side checks.

---

## 11. Q&A Business Rules

Questions belong to a Game and an Account.

Answers belong to a Question and an Account.

Rules:

1. A question must belong to an existing game.
2. An answer must belong to an existing question.
3. Deleted/hidden questions should not normally accept new answers.
4. A user should not be able to create duplicate vote records for the same target.
5. If accepted answers are supported, the accepted-answer rule must be enforced consistently.
6. User-generated content should use Soft Delete rather than Hard Delete.

---

## 12. Voting Rules

For each vote target:

```text
User + Target
```

must represent at most one active vote.

For example:

```text
UNIQUE(account_id, answer_id)
```

for `AnswerVotes`.

Changing a vote should update the existing vote rather than creating duplicate rows.

Removing a current vote may hard-delete the vote relationship row if that is the approved design.

Do not trust client-side restrictions to prevent duplicate votes.

---

## 13. Team Rules

Teams belong to a Game and have an owner.

Possible team states:

```text
ACTIVE
INACTIVE
DISBANDED
```

Team membership must not contain duplicate active memberships.

Applications should use explicit states such as:

```text
PENDING
ACCEPTED
REJECTED
CANCELLED
```

Do not physically delete applications merely because they were rejected.

---

## 14. Authentication & Authorization

Authentication identifies the user.

Authorization determines what the user may do.

Never rely on:

```text
hidden HTML buttons
JavaScript
URL obscurity
```

as the only authorization mechanism.

Server-side authorization must be checked for protected operations.

Examples:

- A user may edit only content they own unless they have moderation/admin permission.
- A normal user cannot perform admin operations.
- A team member cannot automatically perform owner-only operations.
- Moderators/admins may have additional moderation privileges.
- Database permissions must provide a separate safety layer.

---

## 15. External APIs

External APIs are optional integrations, not the foundation of GameNest.

Potential future integrations may include:

- Game information APIs
- Steam-related data
- Discord-related integration
- Map/location APIs
- Other game-specific data services

Rules:

1. Do not make the core community system dependent on an external API unless explicitly approved.
2. External API data should be mapped into GameNest's own domain model when persistence is required.
3. Handle API failures gracefully.
4. Do not expose external API credentials to the browser.
5. API keys/secrets must never be hardcoded into source code.
6. Use configuration/environment variables for secrets.
7. Respect rate limits and API terms.
8. If external data is synchronized into SQL Server, validate it before persistence.

Preferred architecture:

```text
External API
     ↓
Integration Service
     ↓
Validation / Mapping
     ↓
Service
     ↓
DAO
     ↓
GameNestDB
```

---

## 16. Error Handling

Do not expose:

- SQL statements
- Database credentials
- Stack traces
- Internal filesystem paths
- Server configuration
- Sensitive implementation details

to normal users.

Log useful technical information server-side while returning a safe user-facing message.

Database failures should not silently corrupt business state.

For multi-step operations, use transactions where partial completion would create inconsistent data.

---

## 17. Validation

Validate input on the server.

Typical validation includes:

- Required fields
- Length limits
- Numeric ranges
- Enum/status values
- Ownership
- Foreign key existence
- Duplicate relationships
- Business state
- Authorization

Do not trust:

```text
request parameters
hidden form fields
client-side validation
```

---

## 18. Search and Pagination

GameNest will eventually contain many Questions, Answers, LFG posts, and Games.

Use database-level search/filtering and pagination.

Do not load an entire table into Java just to filter it in memory.

For SQL Server, use the project's established pagination approach consistently.

Search should be parameterized. Never concatenate user input directly into SQL.

---

## 19. SQL Injection Prevention

Always use parameterized JDBC statements / PreparedStatement.

Never construct SQL using raw user input such as:

```java
"SELECT ... WHERE name = '" + userInput + "'"
```

Use parameters instead.

Dynamic SQL must be carefully controlled and allowlisted when unavoidable.

---

## 20. Transactions

Use transactions when multiple database operations must succeed or fail together.

Examples:

### Joining LFG

```text
Check capacity
+
Create membership
+
Update player count
```

### Team application acceptance

```text
Accept application
+
Create membership
+
Update related state
```

### Complex moderation operations

```text
Change content status
+
Create moderation record
+
Create notification
```

Do not leave partially completed business operations.

---

## 21. Naming Conventions

Use clear, consistent names.

Recommended database style:

```text
Accounts
Games
Questions
Answers
Comments
LFGPosts
LFGMembers
TeamApplications
QuestionVotes
AnswerVotes
```

Primary keys should follow:

```text
<table_singular>_id
```

Examples:

```text
account_id
game_id
question_id
answer_id
lfg_id
team_id
```

Foreign keys should use the same referenced key name.

Do not introduce multiple naming styles without a strong reason.

---

## 22. Code Quality Rules

Before adding code:

1. Search the existing project.
2. Reuse existing utilities, services, DAOs, filters, and conventions where appropriate.
3. Avoid duplicated business logic.
4. Keep methods focused.
5. Do not introduce unnecessary dependencies.
6. Do not rewrite unrelated working code.
7. Preserve existing behavior unless the requirement explicitly changes it.
8. Keep security checks server-side.
9. Keep database access in the DAO/data-access layer.
10. Keep business rules in the Service layer.

When modifying existing functionality, inspect all affected callers before changing method signatures or behavior.

---

## 23. Database Changes

Do not casually modify the database schema.

Before changing a table:

1. Check existing foreign keys.
2. Check indexes and constraints.
3. Check DAO queries.
4. Check Service logic.
5. Check JSP/Servlet usage.
6. Check seed/test data.
7. Consider migration impact.

A schema change can affect multiple layers.

Prefer incremental, reversible migrations where possible.

---

## 24. Testing Expectations

For important business rules, test both valid and invalid cases.

Examples:

### LFG

```text
5/5 + Join → reject
4/5 + Join → accept
same user joins twice → reject
closed LFG + Join → reject
```

### Voting

```text
first vote → accept
same vote again → reject/update
remove vote → allowed
```

### Authorization

```text
owner edits own content → allowed
other user edits content → reject
unauthorized admin action → reject
```

### Database protection

```text
Application DELETE → denied
Application DROP TABLE → denied
Application SELECT → allowed
Application INSERT → allowed where required
Application UPDATE → allowed where required
```

---

## 25. Do Not Overengineer

GameNest is a learning/project system.

Do not add:

- Microservices
- Message brokers
- Redis
- Kubernetes
- Complex event-driven architecture
- AI
- Distributed storage
- Unnecessary frameworks

unless the project owner explicitly decides that the feature is needed.

Prefer a well-structured monolithic Java application first.

A simple system that is correct, secure, maintainable, and understandable is better than unnecessary complexity.

---

## 26. Implementation Workflow

For a new feature:

```text
1. Understand requirement
        ↓
2. Inspect existing code/database
        ↓
3. Identify business rules
        ↓
4. Identify affected tables
        ↓
5. Identify authorization requirements
        ↓
6. Plan implementation
        ↓
7. Implement
        ↓
8. Compile/build
        ↓
9. Test affected flows
        ↓
10. Report what changed
```

Do not immediately start editing files before understanding the existing implementation.

---

## 27. Change Scope

When asked to fix one issue:

- Fix the root cause.
- Avoid unrelated refactoring.
- Avoid changing unrelated modules.
- Do not silently redesign the database.
- Do not remove existing features without approval.

If a broader change is genuinely required, explain why before proceeding when practical.

---

## 28. Security Priority

When requirements conflict, prioritize:

```text
1. Data integrity
2. Authorization / security
3. Business correctness
4. Reliability
5. Maintainability
6. Performance
7. Convenience
```

Never weaken authorization or database safety simply to make implementation easier.

---

## 29. Final Project Principle

GameNest should follow this principle:

> **The application must be allowed to do what the business requires, but it must not be trusted with unnecessary destructive power.**

Therefore:

```text
Java Backend
    ↓
Business Rules
    ↓
Authorization
    ↓
DAO / SQL
    ↓
SQL Server Permissions
    ↓
Database Constraints
```

Multiple layers should protect important data.

The goal is not merely to make GameNest work.

The goal is to make GameNest:

- Correct
- Secure
- Maintainable
- Understandable
- Expandable
- Consistent with its business rules
