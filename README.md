# Fault-Tolerant Distributed Payment Processing System

A distributed payment processing system prototype designed for an e-commerce platform. The system supports multiple clients performing payment transactions concurrently, ensuring all payments are recorded correctly and consistently even in the presence of node failures or network delays.

## Team Members

| Role | Name | Registration Number | Email |
|------|------|---------------------|-------|
| **Fault Tolerance** (Member 1) | [Your Name] | [Reg. Number] | [email@example.com] |
| **Data Replication & Consistency** (Member 2) | [Your Name] | [Reg. Number] | [email@example.com] |
| **Time Synchronization** (Member 3) | [Your Name] | [Reg. Number] | [email@example.com] |
| **Consensus & Agreement** (Member 4) | [Your Name] | [Reg. Number] | [email@example.com] |

## System Architecture

The system consists of five specialized microservices working together:

```
                                    +------------------+
                                    |     Client       |
                                    +--------+---------+
                                             |
                                             v
                                    +------------------+
                                    | Payment Gateway  |
                                    |    (Port 8090)   |
                                    +--------+---------+
                                             |
              +----------------+-------------+-------------+----------------+
              |                |                           |                |
              v                v                           v                v
     +----------------+ +----------------+        +----------------+ +----------------+
     |Fault Tolerance | |   Consensus    |        |   Time Sync    | |  Replication   |
     | (8081-8083)    | |   (9001-9003)  |        |  (8084-8086)   | |  (6001-6003)   |
     +-------+--------+ +----------------+        +----------------+ +----------------+
             |
             v
     +----------------+
     |   ZooKeeper    |
     |   (Port 2181)  |
     +----------------+
```

### Module Descriptions

| Module | Ports | Description |
|--------|-------|-------------|
| **Payment Gateway** | 8090 | Main entry point for client requests. Orchestrates transaction flow through all modules |
| **Fault Tolerance** | 8081, 8082, 8083 | Maintains registry of healthy nodes, failure detection, automatic failover, and recovery |
| **Consensus (Raft)** | 9001, 9002, 9003 | Ensures strict global ordering of transactions using Raft algorithm |
| **Time Sync** | 8084, 8085, 8086 | Provides synchronized timestamps using NTP and Lamport Logical Clocks |
| **Replication** | 6001, 6002, 6003 | Distributed ledger with quorum-based replication for consistency |
| **ZooKeeper** | 2181 | Coordination service for leader election and distributed locking |

## Prerequisites

Before running the system, ensure you have the following installed:

- **Java JDK 17** or higher
- **Apache Maven 3.6+**
- **Docker Desktop** (for ZooKeeper)
- **Git**

### Verify Installation

```bash
java -version    # Should show Java 17+
mvn -version     # Should show Maven 3.6+
docker --version # Should show Docker version
```

## Quick Start

### Option 1: Using the Startup Script (Recommended)

**Windows (PowerShell):**
```powershell
# 1. Clone the repository
git clone <repository-url>
cd -Fault-Tolerant-Distributed-Payment-processing-System

# 2. Build the project
mvn clean compile

# 3. Start ZooKeeper
docker-compose up -d

# 4. Run the startup script
.\start_all.ps1
```

### Option 2: Manual Startup

**Step 1: Start ZooKeeper**
```bash
docker-compose up -d
```

**Step 2: Build the Project**
```bash
mvn clean compile
```

**Step 3: Start Each Module** (Open separate terminals for each)

```bash
# Terminal 1-3: Fault Tolerance Nodes
cd fault-tolerance
mvn spring-boot:run "-Dspring-boot.run.profiles=node1"
mvn spring-boot:run "-Dspring-boot.run.profiles=node2"
mvn spring-boot:run "-Dspring-boot.run.profiles=node3"

# Terminal 4-6: Consensus Nodes
cd consensus
mvn spring-boot:run "-Dspring-boot.run.profiles=node1"
mvn spring-boot:run "-Dspring-boot.run.profiles=node2"
mvn spring-boot:run "-Dspring-boot.run.profiles=node3"

# Terminal 7-9: Time Sync Nodes
cd time-sync
mvn spring-boot:run "-Dspring-boot.run.profiles=node1"
mvn spring-boot:run "-Dspring-boot.run.profiles=node2"
mvn spring-boot:run "-Dspring-boot.run.profiles=node3"

# Terminal 10-12: Replication Nodes
cd replication
mvn spring-boot:run "-Dspring-boot.run.profiles=node1"
mvn spring-boot:run "-Dspring-boot.run.profiles=node2"
mvn spring-boot:run "-Dspring-boot.run.profiles=node3"

# Terminal 13: Payment Gateway
cd payment-gateway
mvn spring-boot:run
```

### Option 3: Start Individual Modules

Use the provided PowerShell scripts:

```powershell
.\start-fault-tolerance.ps1   # Start Fault Tolerance cluster
.\start_consensus.ps1         # Start Consensus cluster
.\start-time-sync.ps1         # Start Time Sync cluster
.\start-replication.ps1       # Start Replication cluster
```

## Testing the System

### Using cURL

**Submit a Payment:**
```bash
curl -X POST http://localhost:8090/api/payment \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "amount": 99.99,
    "currency": "USD"
  }'
```

**Check System Health:**
```bash
curl http://localhost:8090/api/health
```

**View All Transactions:**
```bash
curl http://localhost:6001/transactions
```

### Using the Demo UI

Open the integrated dashboard in your browser:
```
file:///<project-path>/demo-ui/integrated-dashboard.html
```

## API Endpoints

### Payment Gateway (Port 8090)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payment` | Submit a new payment |
| GET | `/api/health` | Check gateway health |

### Fault Tolerance (Ports 8081-8083)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/fault/status` | Get network status |
| GET | `/fault/healthy-nodes` | Get list of healthy nodes |

### Consensus/Raft (Ports 9001-9003)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/raft/submit` | Submit command to Raft |
| GET | `/raft/status` | Get Raft cluster status |
| GET | `/raft/log` | Get committed log entries |

### Time Sync (Ports 8084-8086)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/time/status` | Get sync status |
| POST | `/time/event` | Tick Lamport clock |

### Replication (Ports 6001-6003)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/replication/process` | Store transaction |
| GET | `/transactions` | Get all transactions |
| GET | `/replication/status` | Get replication status |

## Project Structure

```
distributed-payment-system/
├── common/                  # Shared DTOs and utilities
├── fault-tolerance/         # Failure detection & recovery (Member 1)
├── replication/             # Data replication & consistency (Member 2)
├── time-sync/               # Time synchronization (Member 3)
├── consensus/               # Raft consensus algorithm (Member 4)
├── payment-gateway/         # API gateway & coordinator
├── demo-ui/                 # Web dashboard
├── docker-compose.yml       # ZooKeeper configuration
├── pom.xml                  # Parent Maven configuration
└── start_all.ps1            # System startup script
```

## Component Details

### 1. Fault Tolerance (Member 1)
- **Redundancy:** 3-node cluster with automatic failover
- **Failure Detection:** Heartbeat-based monitoring via ZooKeeper
- **Recovery:** Automatic node rejoin mechanism
- **Leader Election:** ZooKeeper-based coordination

### 2. Data Replication (Member 2)
- **Strategy:** Quorum-based replication (W=2, R=2)
- **Consistency:** Strong consistency with quorum writes
- **Deduplication:** Transaction ID-based duplicate detection
- **Storage:** In-memory ledger with quorum persistence

### 3. Time Synchronization (Member 3)
- **Protocol:** NTP synchronization with pool.ntp.org
- **Logical Clocks:** Lamport clock for causal ordering
- **Log Reordering:** Sequence-based event correlation
- **Accuracy:** Configurable sync intervals

### 4. Consensus Algorithm (Member 4)
- **Algorithm:** Raft consensus protocol
- **Leader Election:** Automatic leader election on failure
- **Log Replication:** Majority-based commit
- **Consistency:** Linearizable reads and writes

## Troubleshooting

### Common Issues

**Port Already in Use:**
```powershell
# Find and kill process on port (e.g., 8081)
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

**ZooKeeper Not Starting:**
```bash
# Check Docker status
docker ps
# Restart ZooKeeper
docker-compose down
docker-compose up -d
```

**Maven Build Fails:**
```bash
# Clean and rebuild
mvn clean install -DskipTests
```

## Stopping the System

```powershell
# Stop all Java processes
Get-Process java | Stop-Process -Force

# Stop ZooKeeper
docker-compose down
```

## Technologies Used

- **Java 17** - Programming language
- **Spring Boot 3.2** - Application framework
- **Apache Maven** - Build tool
- **Apache ZooKeeper** - Distributed coordination
- **Docker** - Container runtime
- **NTP** - Network Time Protocol

## License

This project is developed as part of the Distributed Systems course assignment.
