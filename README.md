# 🛒 ShopNServe

ShopNServe is a small **event-driven shop application** developed as part of a **university project on data-driven system architectures**.

The application allows users to:

- browse products
- add items to a cart
- place orders
- view order history

The main purpose of the project is to demonstrate how modern architectural patterns can be implemented in a real system.

The project focuses on:

- **Blackboard Architecture**
- **event-driven communication**
- **traceability of system interactions**
- integration of **multiple data stores**

---

## Table of Contents

- Project Goals
- Conceptual Architecture
- System Interaction Flow
- Tech Stack
- Frontend
- Backend
- Data Storage
- Running the Project
- Neo4j Visualization

---

# 🎯 Project Goals

The goal of this project is not only to build a shop application, but to explore **architectural design patterns**.

The system demonstrates:

- Blackboard architecture
- Event-driven backend processing
- Decoupled frontend-backend communication
- Graph-based tracing of system interactions
- Use of multiple specialized databases

The system combines:

| Layer | Technology |
|------|-------------|
Frontend | Vue 3 + Vuetify |
Backend | Spring Boot |
Transactional Database | MySQL |
Interaction Graph | Neo4j |

---

# 🧠 Conceptual Architecture

The application follows a **Blackboard Architecture** pattern.

Instead of directly calling backend services, the frontend sends **events containing capabilities** to a central component called **BlackboardService**.

This component decides which backend component should process the request.

---

# 🏗 Architecture Overview

```text
                                                                       ┌───────────────────────────────┐
                                                                       │           Frontend            │
                                                                       │         Vue + Vuetify         │
                                                                       │            Shop.vue           │
                                                                       └───────────────┬───────────────┘
                                                                                       │
                                                                                       │ POST /api/blackboard/messages
                                                                                       ▼
                                                                       ┌───────────────────────────────┐
                                                                       │        BlackboardService      │
                                                                       │  Central request dispatcher   │
                                                                       └───────────────┬───────────────┘
                                                                                       │
                                                                       ┌───────────────┼────────────────┐
                                                                       ▼               ▼                ▼ 
                                                                  AuthHandler  ProductListHandler  OrderHandlers
                                                                       │               │                │
                                                                       ▼               ▼                ▼
                                                                     MySQL           MySQL            MySQL
                                                                                       │
                                                                                       ▼
                                                                                     Neo4j
                                                                             (interaction tracing)
```

---

# 🔄 System Interaction Flow

Every user interaction follows the same conceptual flow:

1. The frontend triggers an action  
2. A **capability-based event** is sent to the backend  
3. The event is received by the **BlackboardService**  
4. Authentication is validated  
5. The appropriate **CapabilityHandler** processes the request  
6. The response is returned to the frontend  
7. The interaction is recorded in **Neo4j**

---

# 🔁 Example Sequence: Loading the ProductList

```text
                                                                                     User
                                                                                       │
                                                                                       │ Login
                                                                                       ▼
                                                                               Frontend (Shop.vue)
                                                                                       │
                                                                                       │ capability: Authentication
                                                                                       ▼
                                                                               BlackboardService
                                                                                       │
                                                                                       │ validate JWT
                                                                                       ▼
                                                                                  AuthHandler
                                                                                       │
                                                                                       ▼
                                                                               User authenticated
                                                                          
                                                                          -------------------------------------
                                                                          
                                                                                     User
                                                                                       │
                                                                                       │ Click "Load Products"
                                                                                       ▼
                                                                                   Frontend
                                                                                       │
                                                                                       │ capability: ProductList
                                                                                       ▼
                                                                               BlackboardService
                                                                                       │
                                                                                       ▼
                                                                               ProductListHandler
                                                                                       │
                                                                                       ▼
                                                                              MySQL (products table)
                                                                                       │
                                                                                       ▼
                                                                          Products returned to frontend
```

---

# ⚙️ Tech Stack

## Frontend

The frontend is implemented using **Vue 3 with TypeScript** and the **Vuetify UI framework**.

It is responsible for handling all user interactions and translating them into **capability-based events** that are sent to the backend.

### Technologies

| Technology | Purpose |
|-------------|-------------|
Vue 3 | reactive frontend framework |
Vuetify | UI component library |
TypeScript | type-safe frontend development |

---

### Frontend Responsibilities

The frontend layer handles:

- user authentication
- displaying available products
- managing the shopping cart
- submitting orders
- displaying order history
- sending capability-based events to the backend

Instead of calling specific backend services directly, the frontend communicates through a **capability-based event system**.

This ensures that the frontend remains **loosely coupled** from backend services.

---

### Main Frontend Component

The application is mainly implemented in the component:

Shop.vue 

This component manages the entire application interface and orchestrates several logical UI areas.

---

### UI Structure

The interface consists of several functional sections.

| UI Section | Description |
|------------|-------------|
Login Panel | user authentication |
Product Grid | displays available products |
Order Panel | shows cart items and allows editing quantities |
Order History | displays previously placed orders |

---

### Product Grid

Displays all available products retrieved from the backend.

Features:

- responsive grid layout
- product selection
- visual cart state
- price display

Products are loaded by sending the capability:

ProductList

to the backend.

---

### Shopping Cart / Order Panel

The order panel allows users to manage their cart.

Features:

- add/remove products
- edit quantities
- display total order price
- submit orders

Submitting an order triggers the capability:

OrderPlaced

---

### Order History

Users can view their previous orders.

Features:

- expandable order panels
- display of ordered items
- chip-based visualization of products
- order totals and timestamps

Order history is loaded via the capability:

OrderHistory

---

### Authentication Flow

The login form allows users to:

- register
- login

Authentication uses **JWT tokens**.

Workflow:

1. User submits credentials
2. Frontend sends capability: Authentication
3. Backend validates credentials
4. JWT token is returned
5. Token is stored in `localStorage`
6. Token is attached to all further requests

---

### Frontend → Backend Communication

The frontend communicates with the backend via a single API endpoint:

```
POST /api/blackboard/messages
```

Each request contains:

```
{
  traceId,
  sender,
  capabilities,
  payload
}
```

Example event:

```
{
  “capabilities”: [“ProductList”],
  “payload”: {
    “action”: “listProducts”
  }
}
```

This design ensures that the frontend does not depend on specific backend endpoints.

---

## Backend

The backend is implemented using **Spring Boot** and follows a **Blackboard Architecture**.

### Technologies

| Technology | Purpose |
|-------------|-------------|
Spring Boot | backend framework |
Maven | dependency management |
REST API | communication layer |

---

### BlackboardService

The **central orchestration component**.

Responsibilities:

- receives all frontend events
- validates authentication
- routes events to capability handlers
- logs system interactions in Neo4j
- returns responses to the frontend

---

### Capability Handlers

Handlers implement the actual business logic.

| Handler | Capability | Description |
|--------|-------------|-------------|
AuthHandler | Authentication | login & registration |
ProductListHandler | ProductList | fetch products |
OrderPlacedHandler | OrderPlaced | store new orders |
OrderHistoryHandler | OrderHistory | retrieve orders |

Handlers implement a shared interface:

```
public interface CapabilityHandler {

  Capability capability();

  BlackboardResponse handle(MessageEventRequest event);

}
```

---

### OrderService

Handles order-related operations.

Responsibilities:

- calculate order totals
- convert cart items to JSON
- store orders in MySQL
- retrieve order history

---

### SessionGraphIngestService

Responsible for recording **system interaction graphs**.

Tracks:

- sessions
- UI components
- backend components
- capabilities
- request and response data

---

# 🧩 Component Dependencies

The system consists of several loosely coupled components.

```
                                                                               Frontend (Shop.vue)
                                                                                       │
                                                                                       ▼
                                                                                BlackboardService
                                                                                       │
                                                                       ┌───────────────┼────────────────┐
                                                                       │               │                │
                                                                   AuthService   ProductService   OrderService
                                                                          │            │              │
                                                                          ▼            ▼              ▼
                                                                        MySQL        MySQL           MySQL
                                                                                       │
                                                                                       ▼
                                                                                     Neo4j
                                                                           (SessionGraphIngestService)
 ```

### Component Roles

| Component | Responsibility |
|-----------|---------------|
Frontend | UI interactions and event generation |
BlackboardService | central request orchestration |
CapabilityHandlers | business logic execution |
OrderService | order processing |
AuthService | authentication and JWT validation |
SessionGraphIngestService | interaction graph storage |

---

# 🗄 Data Storage

The system uses **two databases with different responsibilities**.

---

# MySQL — Transactional Data

Stores actual business data.

## products

| column | description |
|------|-------------|
id | product id |
name | product name |
price_cents | price in cents |
description | product description |
stock | available stock |
created_at | creation timestamp |

---

## orders

| column | description |
|------|-------------|
id | order id |
user_name | username |
items | JSON list of ordered products |
total_cents | order total |
created_at | order timestamp |

Orders store cart items as **JSON** to simplify the schema.

---

# Neo4j — Interaction Graph

Neo4j stores the **interaction graph of the system**.

This graph allows analyzing:

- which UI triggered a request
- which backend handled the request
- which capability was executed
- what data was produced

---

## Graph Nodes

| Node | Description |
|-----|-------------|
Session | user interaction session |
Trace | request trace |
UIComponent | frontend component |
BackendComponent | backend service |
Capability | executed capability |
RequestedData | request payload |
ProvidedData | response payload |

---

## Example Graph Flow

```text
                                                                                 (UIComponent)
                                                                                       │
                                                                                       │ REQUESTS
                                                                                       ▼
                                                                                 (RequestedData)
                                                                                       │
                                                                                       │ HANDLED_BY
                                                                                       ▼
                                                                                 (BackendComponent)
                                                                                       │
                                                                                       │ TRIGGERS_EVENT
                                                                                       ▼
                                                                                 (Capability)
                                                                                       │
                                                                                       │ PROVIDES
                                                                                       ▼
                                                                                 (ProvidedData)
```

---

# 🚀 Running the Project

## Requirements

- Docker
- Docker Compose
- Node.js
- Java 21

---

# Start the Application

Open the Terminal and navigate to the Project and backend/

```
docker compose up –-build
```

Services started:

| Service | Port |
|------|------|
Frontend | 5176 |
Backend | 8080 |
MySQL | 3306 |
Neo4j | 7474 |

---

# Reset Databases

Open the Terminal and navigate to the Project and backend/

```
docker compose down -v
docker compose up –-build
```

---

# 🔍 Neo4j Visualization

Open Neo4j Browser:

```
http://localhost:7474
```

Example query:

```
MATCH (n)
RETURN n
```

Example session trace:

```
MATCH (s:Session)
OPTIONAL MATCH (s)-[:FIRST_STEP]->(first:Step)
OPTIONAL MATCH p=(first)-[:NEXT*0..]->(last:Step)
RETURN s, p
ORDER BY s.startedAt DESC;
```

---

# 📸 Screenshots

## Application Interface

#ToDo

---

# 📂 Project Structure

```
frontend/
  src/
    components/
      Shop.vue

backend/
  src/
    main/
      java/
        shop/
          serve/
            ShopNServe/
              handler/
              service/
              repository/
              model/

mysql/
  init.sql
```

---

# 👨‍🎓 Author

This project was developed as part of a **university project in data-driven system architectures**.

The goal was to design and implement a system demonstrating:

- **Blackboard Architecture**
- **event-driven communication**
- **decoupled system components**
- **graph-based tracing of system interactions**

The focus lies on understanding **architectural patterns and system interaction modelling** in modern software systems.
