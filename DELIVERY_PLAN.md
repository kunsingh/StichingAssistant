# Six-Week MVP Delivery Plan

## Scope and operating model

Team: one React Native engineer, one Python engineer, and an Engineering Manager
who owns scope, architecture, reviews, risk removal, release coordination, and
hands-on spikes. The founder is product owner and supplies content and decisions.

The App Store/Google Play MVP includes accounts, curated learning guides,
deterministic assistant calculations, a basic project planner with cached swatch
previews, inventory, and a read/post community feed. It does not include direct
messages, advanced moderation automation, arbitrary image prompts, marketplace,
social graphs, offline sync, or multi-colour pattern generation.

## Week 1 - foundations and risk spikes

- EM: lock acceptance criteria, analytics, privacy/data retention, threat model,
  provider budgets, formula assumptions, and release checklist.
- Frontend: React Native shell, navigation, design tokens, authentication flow,
  crash/analytics wiring, and CI builds.
- Backend: Python API skeleton, PostgreSQL schema, authentication integration,
  deployment pipeline, observability, and spikes for LLM/image/Ravelry providers.
- Exit: builds install on both platforms; staging API is deployed; image latency
  and calculator accuracy risks have measured baselines.

Dependencies: founder provides brand assets, initial guide content, terms/privacy
drafts, Apple/Google organization accounts, and provider accounts immediately.

## Week 2 - learning, accounts, and deterministic assistant

- Frontend: account/profile screens, guide list/detail, assistant chat shell.
- Backend: users, guide delivery, calculator service, constrained intent
  extraction, answer provenance, offline/provider failure behaviour, evaluation.
- EM: recruit two or three knitting experts and define golden calculation cases.
- Exit: user can sign in, read seeded guides, receive traced calculator answers,
  and see a clear refusal for unsupported questions.

## Week 3 - project planner and preview pipeline

- Frontend: dimensions/stitch/colour controls, immediate placeholder, zoomable
  preview, local preset recolouring/cache.
- Backend: projects, validation, content-addressed swatch cache, generation jobs,
  moderation/quality gates, CDN assets, quotas and circuit breaker.
- EM: latency/cost review and go/no-go on generated versus prebuilt catalogue.
- Exit: planner persists a project and updates preset colours without blocking.

## Week 4 - inventory and community

- Frontend: yarn/needle inventory CRUD, community feed, post creation/reporting.
- Backend: inventory and feed APIs, pagination, uploads, report queue, basic
  profanity/spam controls, admin moderation endpoint.
- EM: moderation runbook, abuse scenarios, accessibility and privacy review.
- Exit: end-to-end inventory and a deliberately simple chronological feed work.

## Week 5 - integration, hardening, and beta

- Both engineers: integration tests, accessibility, low-end device performance,
  provider outage tests, migrations/backups, rate limits, analytics dashboards.
- EM: TestFlight/Play internal beta, expert knitting review, bug triage, support
  playbook, store metadata, data-safety forms.
- Exit: no release-blocking defects; restore/outage drills pass; p95 and cost
  budgets are understood; consent and account deletion are tested.

## Week 6 - release candidate and submission

- Frontend: polish only release blockers, screenshots, signing, store builds.
- Backend: production deployment, alerts, capacity/load test, rollback rehearsal.
- EM/founder: acceptance test, staged rollout decision, submit both apps early
  enough for review feedback, daily launch monitoring and incident ownership.
- Exit: approved production release or an explicit, evidence-based hold.

## Dependencies and critical path

Account ownership, privacy terms, curated content, and moderation policy are
founder dependencies. Authentication and data model precede feature integration.
The image latency spike in Week 1 determines whether the planner uses generated
assets, pre-generated assets, or only procedural previews at launch. Store
enrolment and review are external dependencies and must start on day one.

## Top risks and mitigations

1. **Unsafe or wrong advice:** deterministic numeric service, provenance,
   expert golden set, explicit uncertainty, refusals, monitored evaluations.
2. **Image latency/cost/quality:** pre-generation, neutral recolouring, quotas,
   multi-layer cache, visible fallback, quality gates, circuit breaker.
3. **Community abuse:** narrow posting features, report/block, rate limits,
   founder-owned moderation queue; remove posting if staffing is unavailable.
4. **Two-person throughput:** vertical slices, one deploy path, feature flags,
   daily scope decisions, no custom auth/chat/recommendation system.
5. **Store delay:** accounts and compliance in Week 1, internal builds weekly,
   submission at the start of Week 6, web fallback for stakeholders.

## Explicit cut order

If milestones slip, cut Ravelry comparison first (it is prototype evidence, not
an MVP user need), then AI generation in favour of a curated swatch catalogue,
then community post creation (retain a read-only curated feed), then inventory
photos. Never cut calculation provenance, refusal behaviour, account deletion,
moderation/reporting for writable community, accessibility basics, monitoring,
or rollback capability.
