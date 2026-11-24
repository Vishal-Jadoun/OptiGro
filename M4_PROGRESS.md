# M4_PROGRESS — OptiGro Development Tracker

> **Project:** OptiGro — 10-Minute Grocery Delivery Logistics Engine  
> **Deadline:** 25 November 2026  
> **Role:** M4 — Security, Delivery & Platform Integration (Aditya)  
> **Branch:** `m4/delivery-platform`

---

## Session 1 — 23 November 2026 (early morning)

**Status:** ✅ SESSION COMPLETED

### Tasks Completed
- [x] Read MASTER_PLAN.md, GIT_CHEATSHEET.md, M4_Aditya_Delivery_Platform.md
- [x] Inspected repository structure and existing team code
- [x] Checked Git status — main branch clean, up to date with origin
- [x] Created `m4/delivery-platform` branch from main
- [x] Created `PartnerStatus.java` enum (ONLINE, OFFLINE, ON_DELIVERY)
- [x] Created `UserRole.java` enum (ADMIN, CUSTOMER, PICKER, DELIVERY_PARTNER)
- [x] Created `User.java` model with equals/hashCode, lockout mechanism
- [x] Created `DeliveryPartner.java` model with equals/hashCode, rating system
- [x] Created `Delivery.java` model with equals/hashCode, SLA fields
- [x] Created `InvalidAuthException.java` custom checked exception
- [x] Created `BaseRepository.java` generic interface (dependency for all repos)
- [x] Created `UserRepository.java` with ConcurrentHashMap + CSV loading
- [x] Created `DeliveryPartnerRepository.java` with ConcurrentHashMap + CSV loading
- [x] Created `data/deliveries.csv` with header row
- [x] Compiled all M4 files — zero errors
- [x] Ran verification tests — all 9 tests passed (enums, models, exception, repos, CSV loading)

### Files Created
| # | File | Type |
|---|------|------|
| 1 | `src/com/optigro/constants/PartnerStatus.java` | Enum |
| 2 | `src/com/optigro/constants/UserRole.java` | Enum |
| 3 | `src/com/optigro/models/User.java` | Model |
| 4 | `src/com/optigro/models/DeliveryPartner.java` | Model |
| 5 | `src/com/optigro/models/Delivery.java` | Model |
| 6 | `src/com/optigro/exception/InvalidAuthException.java` | Exception |
| 7 | `src/com/optigro/repository/BaseRepository.java` | Interface |
| 8 | `src/com/optigro/repository/UserRepository.java` | Repository |
| 9 | `src/com/optigro/repository/DeliveryPartnerRepository.java` | Repository |
| 10 | `data/deliveries.csv` | Data file |

### Files Modified
- None (all new files)

### Git Commits
| # | Hash | Message |
|---|------|---------|
| 1 | `674235d` | feat(m4): add PartnerStatus and UserRole enums |
| 2 | `052af7a` | feat(m4): add User, DeliveryPartner and Delivery models with equals/hashCode |
| 3 | `c1e0ac9` | feat(m4): add InvalidAuthException custom checked exception |
| 4 | `4c365f1` | feat(m4): add BaseRepository interface, UserRepository and DeliveryPartnerRepository with CSV loading |
| 5 | `ce9201d` | chore(m4): add deliveries.csv data file with header |

### Push Status
- ✅ Pushed to `origin/m4/delivery-platform` successfully

### Compilation
- ✅ All 10 files compile cleanly with `javac` — zero errors, zero warnings

### Testing
- ✅ PartnerStatus enum — 3 values verified
- ✅ UserRole enum — 4 values verified
- ✅ User model — constructor, equals/hashCode, toString working
- ✅ User lockout — locks after 3 failed attempts, unlocks correctly
- ✅ DeliveryPartner model — constructor, avgRating, isLowRated working
- ✅ Delivery model — constructor, rating, SLA fields working
- ✅ InvalidAuthException — catch/throw verified
- ✅ UserRepository — loaded 15 users from CSV, findByUsername working
- ✅ DeliveryPartnerRepository — loaded 4 partners from CSV, findOnline working

### Errors/Blockers
- None

### Dependency Note
- `BaseRepository.java` is officially M1's file, but it did not exist yet. Created it as a generic interface since all M4 repositories depend on it. If M1 creates their own version later, we will coordinate to avoid conflicts.

---

## Session 2 — 23 November 2026 (evening)

**Status:** ✅ SESSION COMPLETED

### Tasks Completed
- [x] Read MASTER_PLAN.md, GIT_CHEATSHEET.md, M4_PROGRESS.md
- [x] Verified Session 1 commits are pushed (confirmed zero unpushed commits)
- [x] Fetched latest remote — no new changes from other team members
- [x] Inspected existing codebase — M2/M3 files not yet created by teammates
- [x] Implemented `AuthService.java` with login logic and `Set<String> lockedUsernames`
- [x] Created shared M2 contract types needed by M4 (`OrderStatus`, `PaymentMode`, `Order`, `OrderItem`) — these are frozen contracts, not M2 business logic
- [x] Implemented `DeliveryService.java` with full functionality:
  - `ArrayDeque<Integer>` FIFO partner dispatch queue
  - `ArrayDeque<Order>` pending orders queue (when no partners online)
  - `java.time.Duration` 10-minute SLA verification
  - `Set<Integer>` flagged low-rated partners tracking
  - CSV append logging to `data/deliveries.csv`
  - SLA report generation
  - Callback-based integration (avoids direct OrderService dependency)
- [x] Compiled all project files — zero errors
- [x] Ran 12 verification tests — all passed

### Files Created
| # | File | Type | Owner |
|---|------|------|-------|
| 1 | `src/com/optigro/service/AuthService.java` | Service | M4 |
| 2 | `src/com/optigro/service/DeliveryService.java` | Service | M4 |
| 3 | `src/com/optigro/constants/OrderStatus.java` | Enum | M2 (shared contract) |
| 4 | `src/com/optigro/constants/PaymentMode.java` | Enum | M2 (shared contract) |
| 5 | `src/com/optigro/models/Order.java` | Model | M2 (shared contract) |
| 6 | `src/com/optigro/models/OrderItem.java` | Model | M2 (shared contract) |

### Files Modified
- None

### Git Commits
| # | Hash | Message |
|---|------|---------|
| 1 | `f829f30` | feat(m4): implement AuthService with login and Set<String> lockout mechanism |
| 2 | `e06a320` | feat(m4): add shared M2 contract types (OrderStatus, PaymentMode, Order, OrderItem) needed by DeliveryService |
| 3 | `af09014` | feat(m4): implement DeliveryService with ArrayDeque FIFO dispatch, Duration SLA, Set flagged partners, and CSV logging |

### Push Status
- ✅ All 3 commits pushed to `origin/m4/delivery-platform`

### Compilation
- ✅ All Java files compile cleanly — zero errors, zero warnings

### Testing
- ✅ AuthService — successful login (admin, customer, partner roles verified)
- ✅ AuthService — wrong password countdown (3 attempts → lock)
- ✅ AuthService — Set<String> lockedUsernames contains locked account
- ✅ AuthService — locked user rejected even with correct password
- ✅ AuthService — empty/null username validation
- ✅ AuthService — nonexistent user handled
- ✅ DeliveryService — partner online/offline toggling
- ✅ DeliveryService — FIFO dispatch (2 orders dispatched to 2 partners)
- ✅ DeliveryService — pending queue (order queued when no partners online)
- ✅ DeliveryService — markDelivered with Duration SLA check (MET ✓)
- ✅ DeliveryService — partner re-enters queue after delivery + pending order auto-dispatched
- ✅ DeliveryService — rateDelivery and Set<Integer> flagged partners
- ✅ DeliveryService — CSV append (header + data written correctly)
- ✅ DeliveryService — SLA report generation
- ✅ DeliveryService — findActiveDeliveryByPartner

### Errors/Blockers
- None

### Design Decision: DeliveryService & OrderService Dependency
The roadmap shows `DeliveryService` taking `OrderService` as a constructor param. However, `OrderService` is M2's business logic class (not yet created). Instead of creating a fake `OrderService`, I designed `DeliveryService` to use **functional callbacks**:
- `setOrderLookup(Function<Integer, Optional<Order>>)` — wired from App.java
- `setOrderStatusUpdater(BiConsumer<Integer, OrderStatus>)` — wired from App.java

This keeps M4 independent while still supporting full integration when M2's `OrderService` is ready. In `App.java`, we'll wire these like:
```java
delivery.setOrderLookup(orders::getOrder);
delivery.setOrderStatusUpdater(orders::updateStatus);
```

### M2 Shared Contract Note
Created `OrderStatus`, `PaymentMode`, `Order`, and `OrderItem` exactly as defined in the frozen contracts. These are shared types all team members need, not M2 business logic. If Harnoor creates her versions, we'll coordinate to merge without conflicts.

---

## Session 3 — 24 November 2026 (night)

**Status:** ✅ SESSION COMPLETED

### Tasks Completed
- [x] Read MASTER_PLAN.md, GIT_CHEATSHEET.md, M4_PROGRESS.md
- [x] Verified Git status and merged upstream `origin/main` changes
- [x] Integrated shared `ConsoleScannerUtil.java` and `AsciiTableFormatter.java` from team repo
- [x] Implemented `DeliveryMenu.java` — delivery partner terminal controller
  - Online / Offline status toggle
  - Active assigned order viewer
  - Delivery completion and 10-minute SLA trigger
  - Performance dashboard with ratings and quality review warning
- [x] Implemented `MainMenuController.java` — master platform controller
  - ASCII banner with default demo credentials
  - Secure authentication loop with `AuthService`
  - Role-based routing (`ADMIN`, `DELIVERY_PARTNER`, `CUSTOMER`, `PICKER`)
  - Administrator Control Panel
  - M4 Consolidated System Reports menu
  - Plain-text export of audit reports to `reports/full_report.txt` via `BufferedWriter`
  - Pluggable callback hooks for M1/M2/M3 modules
- [x] Created initial `reports/full_report.txt` audit report export
- [x] Compiled all project files — 100% clean compilation (`javac`, 0 errors, 0 warnings)
- [x] Tested all Session 3 components via automated test suite — all tests passed

### Files Created
| # | File | Type | Owner |
|---|------|------|-------|
| 1 | `src/com/optigro/controller/DeliveryMenu.java` | Controller | M4 |
| 2 | `src/com/optigro/controller/MainMenuController.java` | Controller | M4 |
| 3 | `reports/full_report.txt` | Report File | M4 |
| 4 | `src/com/optigro/util/ConsoleScannerUtil.java` | Utility | M1/M2 (Integrated) |
| 5 | `src/com/optigro/util/AsciiTableFormatter.java` | Utility | M2 (Integrated) |

### Files Modified
- None

### Git Commits
| # | Hash | Message |
|---|------|---------|
| 1 | `a7c2841` | chore(util): integrate shared ConsoleScannerUtil and AsciiTableFormatter utilities |
| 2 | `ed5e5de` | feat(m4): implement DeliveryMenu console controller for delivery partners |
| 3 | `8fa3b0b` | feat(m4): implement MainMenuController with login loop, role routing, and report export |
| 4 | `927270b` | feat(m4): add initial consolidated audit report export |

### Push Status
- Pending final push at end of Session 3

### Compilation
- ✅ All Java files compile cleanly — zero errors, zero warnings

### Testing
- ✅ Repositories and Services loaded from CSV (15 users, 4 partners)
- ✅ `DeliveryMenu` instantiated and verified with `DeliveryPartner` profile
- ✅ `MainMenuController` banner display verified
- ✅ Role router verified across all roles (`ADMIN`, `DELIVERY_PARTNER`, `CUSTOMER`, `PICKER`)
- ✅ Pluggable handler integration verified
- ✅ Dispatch, delivery completion, 5-star rating, and SLA report verified
- ✅ `exportAllReportsToFile()` verified: `reports/full_report.txt` generated on disk with proper headers

### Errors/Blockers
- None

---

## Remaining M4 Tasks (For Final Session 4)
- [ ] `App.java` — master manual dependency injection wiring connecting all components
- [ ] Full end-to-end integration and simulation demonstration
- [ ] Final compilation, test run, and git synchronization
- [ ] Final viva preparation notes and project summary

---

## Next Session
**Session 4 (FINAL SESSION)** — 25 November 2026

### Planned Objectives
- Wire full manual dependency injection in `App.java`
- Connect callbacks (`orders.setOnOrderPlaced`, `picking.setOnOrderPacked`)
- Wire all services into `MainMenuController`
- Execute complete end-to-end order fulfillment test
- Final compilation and clean build verification
- Final commit, push, and viva defense preparation
