# ROOMA

## Prerequisites

### Local

- **Node.js 24.x** and **npm** or **yarn**  
- **Java 25** and **Maven 3 (or higher)**  
- **PostgreSQL**

### Docker  
- **Docker and Docker Compose**

---

## Installation and local execution

1. Clone the repository:  
```bash
   git clone https://github.com/ISPP-ROOMA/ROOMA.git
```

### Database

Enter the postgres user credentials and the database name in the application-dev.yml

### Backend

1. Build the project:
```bash
    cd backend
    mvn clean install
```

2. Run the Spring application:
```bash
    mvn spring-boot:run
```

Runs on http://localhost:8080 by default

### Frontend

1. Install dependencies:
```bash
    cd frontend
    npm install
```

2. Start the frontend:
```bash
    npm run dev
```

Runs on http://localhost:5173 by default

---

## Installation and execution with Docker

1. Start the containers:
```bash
    docker compose up -d
```

2. Remove containers and volumes:
```bash
    docker compose down -v

Backend runs on http://localhost:8080 by default
Frontend runs on http://localhost:8080 by default