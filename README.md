# 🛒 ShopNServe

ShopNServe is a small **event-driven shop application** demonstrating a **Blackboard Architecture** using modern web technologies.

The application allows users to:

- browse products
- add items to a cart
- place orders
- view their order history

Additionally, all interactions are tracked in **Neo4j**, allowing the system to visualize the full **request flow between UI components and backend services**.

---

# 🧠 Architecture

The system follows a **Blackboard Architecture** pattern.

Instead of directly calling backend services, frontend components send **capability-based events** to a central **BlackboardService** which routes them to the correct handler.

# ToDo Bild der Architektur

Each request is processed through the following steps:

1. Frontend sends an event with **capabilities**
2. BlackboardService validates authentication
3. The event is routed to the correct **CapabilityHandler**
4. The handler processes the request
5. The response is returned to the frontend
6. Both request and response are logged in **Neo4j**

---

# ⚙️ Tech Stack

## Frontend

- **Vue 3**
- **Vuetify**
- **TypeScript**

Features:

- responsive product grid
- shopping cart
- quantity editing
- order submission
- order history
- JWT authentication

---

## Backend

- **Spring Boot**
- **Maven**
- **REST API**

Key components:

### BlackboardService

Central orchestration layer handling all frontend events.

### CapabilityHandlers

| Capability | Description |
|--------|-------------|
| Authentication | Login and registration |
| ProductList | Fetch available products |
| OrderPlaced | Store new orders |
| OrderHistory | Retrieve past orders |

---

# 🗄 Databases

## MySQL

Used for transactional data.

### products table

| column | description |
|------|-------------|
| id | product id |
| name | product name |
| price_cents | product price |
| description | product description |
| stock | available stock |
| created_at | creation timestamp |

### orders table

| column | description |
|------|-------------|
| id | order id |
| items | JSON list of ordered products |
| total_cents | order total |
| created_at | order timestamp |

Orders store items as **JSON** to keep the schema simple.

---

## Neo4j

Neo4j stores **system interaction graphs**.

This allows tracking:

- which UI triggered a request
- which backend handled it
- which capability was executed
- what data was provided

### Main Nodes

- Session
- Trace
- UIComponent
- BackendComponent
- Capability
- RequestedData
- ProvidedData

### Example Graph Flow

#ToDo Bild

---

# 🚀 Running the Project

## Requirements

- Docker
- Docker Compose
- Node.js
- Java 21

---

## Start the Application

To start the Application navigate to: 

shopnserve/
backend

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

## Reset Databases

To start with a clean database:

```
docker compose down -v
docker compose up –build
```

---

# 🔍 Neo4j Graph Visualization

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
todo
```

---

# 📦 Project Structure

frontend/
microclient-vue/
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
controller/
handler/
model/
repository/
service/


mysql/
init.sql

---

# ✨ Features

✔ JWT Authentication  
✔ Product catalog  
✔ Shopping cart  
✔ Order submission  
✔ Order history  
✔ Event-driven backend architecture  
✔ System trace visualization with Neo4j

---

# 🔮 Possible Improvements

Future extensions could include:

- product search
- stock updates after orders
- admin dashboard
- order status tracking
- analytics based on Neo4j graphs

---

# 👨‍💻 Author

Developed as part of a **data-driven system architecture project** demonstrating:

- blackboard architecture
- event-driven systems
- graph-based request tracing
