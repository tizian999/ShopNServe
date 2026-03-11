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

ToDo Bild

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

# 🔁 Example Sequence: Placing an Order

ToDo

---

# 🧩 Component Dependencies

The system consists of several loosely coupled components.

ToDo

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

# ⚙️ Backend Architecture

## BlackboardService

The **central orchestration component**.

Responsibilities:

- receives all frontend events
- validates authentication
- routes events to capability handlers
- logs system interactions in Neo4j
- returns responses to the frontend

---

## Capability Handlers

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

## OrderService

Handles order-related operations.

Responsibilities:

- calculate order totals
- convert cart items to JSON
- store orders in MySQL
- retrieve order history

---

## SessionGraphIngestService

Responsible for recording **system interaction graphs**.

Tracks:

- sessions
- UI components
- backend components
- capabilities
- request and response data

This enables **graph-based tracing of the application**.

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

ToDo

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
docker compose up –build
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
docker compose up –build
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
ToDo right query
```

---

# 📸 Screenshots

## Application Interface

ToDo

---

# 📂 Project Structure

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

---

# 👨‍🎓 Author

This project was developed as part of a **university project in data-driven system architectures**.

The goal was to design and implement a system demonstrating:

- **Blackboard Architecture**
- **event-driven communication**
- **decoupled system components**
- **graph-based tracing of system interactions**

The focus lies on understanding **architectural patterns and system interaction modelling** in modern software systems.
