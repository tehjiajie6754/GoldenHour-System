
![Golden Hour Banner](goldenhour/image/banner.png)
# 🏪 Golden Hour Store Management System

> Java-based luxury watch retail management system

**[📃 Overview](#-overview) | [🚀 Functionality](#-functionality) | [🤝 Contributing](#-contributing)**

---

# 📋 Overview

## ✨ Quick Highlights
- **🔐 Authentication:** Employee login/registration
- **⏱️ Attendance:** Clock in/out with SQL persistence
- **📦 Inventory:** Stock counts and transfers
- **💳 Sales:** Transactions with receipts
- **🔍 Search:** Employees, products, and sales
- **✏️ Editing:** Update inventory, pricing, and employee info
- **🔄 Hybrid Storage:** SQLite = source of truth; CSV = backup/export

## 💻 Tech Stack
| Component | Technology |
|-----------|------------|
| Language | Java 11+ |
| Build | Maven 3.6+ |
| Database | SQLite 3 (primary) + CSV (backup) |
| UI | Console/Swing |
| Data Format | CSV + SQL |

## 📂 Project Stats
- **Modules:** main, categories, service, storage, ui, dataload, utils, gui
- **Storage:** SQLite primary; CSV backup/export
- **Data Sync:** One-time CSV → SQLite migration; runtime uses SQLite
- **Architecture:** Layered (UI → Service → Storage → DataLoad)


## 🖼 Snippets 
![Golden Hour Banner](goldenhour/image/loginpage.png)


![Golden Hour Banner](goldenhour/image/dashboard.png)
---

## 🏎️ Project Roadmap & Status

### ✅ Completed Features
- [x] **User Auth:** Login/Logout & Employee Registration
- [x] **Attendance:** Attendance Log
- [x] **Inventory:** Stock management system
- [x] **Sales:** Sale system
- [x] **Data Management:** Search, Edit, and Persistent Storage
- [x] **UX:** Loading states and Sales History filtering
- [x] **GUI:** Comprehensive Graphical User Interface
- [x] **Automation:** Auto-emailing reports to Headquarters
- [x] **Analytics:** Visual data analytics & charts
- [x] **Performance:** Employee performance metric tracking

---

# ⚙️ Functionality

## System Architecture
```
┌──────────────────────────────────┐
│      User Interface (UI)         │ ← Console/GUI
├──────────────────────────────────┤
│     Service Layer (Logic)        │ ← Business ops
│  Auth, Sales, Stock, Attendance  │
├──────────────────────────────────┤
│     Storage Layer (I/O)          │ ← Persistence
│     DatabaseHandler (SQLite),    |
|     CSVHandler (backup)          │
├──────────────────────────────────┤
│   In-Memory Cache (DataLoad)     │ ← Fast access
│     Static lists of all data     │
└──────────────────────────────────┘
```

## 🆕 Updates 
### ⏩ Run Program
There are two entry points as of now :
`Main.java` & `MainGUI.java`

Choose either one to start the program

### 🆕 Hybrid Storage Model
SQLite is the primary store; CSV is backup/export. Migration from CSV to SQLite is a one-time operation via `SyncDataCSVSQL`. **Note: Migration has been completed, only run this when you want to sync SQL with CSV**


## 🚀 Core Workflows Simplified

### 1️⃣ Startup & Data Loading
```
Main.java starts
  ↓
DatabaseHandler.connect/query → Load from SQLite
  ↓
DataLoad.fetchallxxx() → In-memory lists ready
  ↓
LoginUI displayed (system ready)
```

### 2️⃣ Authentication
```
LoginUI → AuthService.login(id, password)
  ↓
Search DataLoad.allEmployees (in-memory)
  ↓
Validate and proceed
```

### 3️⃣ Attendance Logging
```
AttendanceUI → clock in/out
  ↓
AttendanceService → create record (timestamp)
  ↓
Persist to SQLite (primary)
  ↓
Update CSV (backup)
```

### 4️⃣ Stock Management
```
StockUI → count/transfer/search
  ↓
Services update Model/Stock in DataLoad
  ↓
Persist changes to SQLite (CSV as backup)
  ↓
ReceiptHandler → text receipts (as needed)
```

### 5️⃣ Sales Processing
```
SalesUI → product + qty
  ↓
SalesService → create sale, update inventory
  ↓
Persist to SQLite
  ↓
ReceiptHandler → sales receipt
  ↓
Update CSV (backup)
```

### 6️⃣ Search Operations
```
SalesSearch/StockSearch → iterate DataLoad (in-memory)
  ↓
Return matches (no disk I/O)
```

### 7️⃣ Data Editing
```
EditXXX → select item + new values
  ↓
Service finds object in DataLoad and updates via setters
  ↓
Persist to SQLite
  ↓
Optional CSV export (backup)
```

## Data Models (POJOs)
| Entity | Fields | Storage |
|--------|--------|---------|
| Employee | id, name, role, password | SQLite (primary), CSV (backup) |
| Model | code, name, price, outlet | SQLite (primary), CSV (backup) |
| Outlet | code, name | SQLite |
| Stock | model_code, outlet_code, quantity | SQLite |
| Sales | id, model_code, qty, total, timestamp | SQLite (primary), CSV (backup) |
| Attendance | emp_id, date, clock_in, clock_out | SQLite (primary), CSV (backup) |

## Key Classes
| Package | Class | Responsibility |
|---------|-------|----------------|
| main | `Main.java` | Entry point |
| main | `MainGUI.java` | Entry point GUI |
| main | `SyncDataCSVSQL.java` | One-time CSV → SQLite migration |
| categories | `Employee.java`, `Model.java`, `Sales.java`, `Attendance.java`, `Outlet.java` | POJOs with `fromCSV()`/`toCSV()` |
| service/attendance | `AttendanceService.java` | Attendance logic |
| service/loginregister | `AuthService.java`, `RegistrationService.java` | Auth/registration |
| service/salessys | `SalesService.java`, `SalesSearch.java` | Sales ops |
| service/stocksys | `StockCountService.java`, `StockMovementService.java`, `StockSearch.java` | Inventory ops |
| storage | `DatabaseHandler.java` | SQLite CRUD/schema |
| storage | `CSVHandler.java` | CSV backup/export |
| storage | `ReceiptHandler.java` | Receipt generation |
| dataload | `DataLoad.java` | In-memory cache of runtime data |
| ui | `LoginUI.java`, `SalesUI.java`, `StockUI.java`, `SearchUI.java`, `AttendanceUI.java`, `EditUI.java` | Console UI |

## Data Flow Summary
- **Read:** Populate `DataLoad` from SQLite via `DatabaseHandler` (no CSV reads at runtime).
- **Write:** Services persist to SQLite; CSV used only for backup/export.
- **Query:** Use SQLite for complex queries; UI reads from `DataLoad`.
- **Backup:** CSV serves solely as export/backup.

---

# 🤝 Contributing

## Getting Started
```bash
# Clone
git clone https://github.com/zerngit/GoldenHour-System.git
cd GoldenHour-System
cd goldenhour

## Branches
git fetch origin
git checkout main
git pull origin main
git checkout -b feature/YOUR_FEATURE # edit YOUR_FEATURE to the feature you work on
```

## Daily Workflow
```bash
git checkout main
git pull origin main

git checkout feature/your-branch
git merge main

# Work
git add .
git commit -m "Describe changes"
git push origin feature/your-branch

```

## Submit a PR
- Base: `main` ← Compare: `feature/your-branch`
- Title: `[Feature] <name> Complete`
- Request review from `zerngit`

---

**Last Updated:** January 6, 2026 | **Version:** 2.0.1
