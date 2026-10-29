# REST API

Base path `/api`, with no trailing slash. Bodies and responses use JSON. The complete OpenAPI document is in `backend/src/main/resources/static/openapi.json` and is served as `/openapi.json`.

## Session and CSRF

`GET /api/session` creates or restores a private browser workspace and returns `csrfToken`, `mode`, `isDemo` and `demoEnabled`. Keep its HttpOnly `FIELDKIT_SESSION` cookie and send `X-CSRF-TOKEN` with the current token on POST, PUT and DELETE. No API key or borrower login is needed. A separate cookie jar gets a separate workspace.

```js
const session = await fetch("/api/session").then((r) => r.json());
const response = await fetch("/api/equipment", {
  method: "POST",
  credentials: "same-origin",
  headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": session.csrfToken },
  body: JSON.stringify({ name: "Camera kit", category: "CAMERAS", totalQuantity: 3 }),
});
```

## Routes

| Method             | Route                  | Behavior                                                           |
| ------------------ | ---------------------- | ------------------------------------------------------------------ |
| GET                | `/health`              | Database readiness; no workspace created                           |
| GET                | `/session`             | Private workspace and current CSRF token                           |
| POST               | `/demo`                | Seed an empty workspace with fictional records; body `{}`          |
| GET                | `/dashboard`           | Owned inventory and loan summaries                                 |
| GET                | `/export`              | Owned equipment, borrowers and loans, without session secrets      |
| DELETE             | `/workspace`           | Body `{"confirmation":"DELETE"}`; removes records and session, 204 |
| GET / POST         | `/equipment`           | List or create equipment; creation returns 201                     |
| GET / PUT / DELETE | `/equipment/{uuid}`    | Read, replace editable fields, or remove unused equipment          |
| GET / POST         | `/borrowers`           | List or create borrowers; creation returns 201                     |
| PUT / DELETE       | `/borrowers/{uuid}`    | Update a person or remove one without loan history                 |
| GET / POST         | `/loans`               | List or create loans; creation returns 201                         |
| GET                | `/loans/{uuid}`        | Read an owned loan                                                 |
| POST               | `/loans/{uuid}/return` | Body `{}`; return the entire quantity once                         |

DELETE on equipment or borrowers returns 409 when any loan history references them. Whole-record PUT requires the required fields again; omitted optional text becomes empty. Unknown fields, owner fields and server-managed fields are rejected.

## Input shapes

Equipment: `name` (1–120), `model` (up to 120), `category`, `location` (up to 120), `description` (up to 2,000), `totalQuantity` (integer 1–1,000). Categories are `CAMERAS`, `AUDIO`, `LIGHTING`, `COMPUTING`, `ACCESSORIES`. Quantities cannot be reduced below units still on loan.

Borrower: `name` (1–100), optional `team` (up to 100). No contact information is required.

Loan: `equipmentId`, `borrowerId`, `quantity` (integer 1–1,000), `dueDate` (ISO calendar date), optional `notes` (up to 2,000). The date is from today through one year from today in `APP_TIME_ZONE`, default Asia/Almaty. Checkout time is server-generated. No partial returns are supported.

Quantities submitted as strings or decimals are rejected. Text fields reject numeric/boolean coercion and control characters; notes/descriptions allow ordinary line breaks and tabs.

## Queries and pagination

All list routes support `search` (max 100), `page` (1–10,000) and `pageSize` (1–50, default 20). Repeated or unknown query parameters are rejected.

- Equipment: `category`, `status=ALL|AVAILABLE|OUT`. Search matches name/model; OUT means zero available units.
- Borrowers: search matches name/team.
- Loans: `status=ALL|ACTIVE|OVERDUE|RETURNED`. Search matches equipment/borrower name. ACTIVE includes overdue loans. OVERDUE means unreturned and due before today; due today is active.

Responses contain `items`, `page`, `pageSize`, `totalItems`, `totalPages`. Lists have stable ID tie-breakers. An out-of-range page returns an empty page. Client dropdowns load bounded pages; workspace caps are 1,000 equipment types, 1,000 borrowers and 2,000 loans.

## Transactions and retries

Checkout checks owned references and current availability while locked, decrements stock and inserts the loan atomically. Return locks the records, marks the loan returned and restores its quantity atomically. A second return returns 409 `ALREADY_RETURNED` and never adds stock again.

Writes have no idempotency-key contract. Do not automatically retry a timed-out mutation: refresh records and reconcile whether it completed before retrying. A timeout can occur after the server committed a change.

## Errors

```json
{
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "There are not enough available units. Refresh availability or reduce the quantity."
  }
}
```

400 malformed JSON/types/UUIDs, 403 CSRF failure, 404 missing/not owned, 409 stock/history/capacity conflict, 413 body over 32 KB, 415 unsupported content type, 422 validation, 429 throttle, 503 unavailable. Unexpected failures return generic messages.

Default throttling targets 300 API requests/minute per remote IP with bounded process-local counters. It is approximate and resets on restart. Forwarded addresses are not trusted for the counter. Add an edge limiter for public hosting.
