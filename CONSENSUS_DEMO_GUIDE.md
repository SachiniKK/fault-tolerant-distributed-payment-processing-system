# Consensus Module - Demo Guide

## How to Run & Test the Raft Consensus Implementation

---

### Step 1: Start the Consensus Cluster

Open PowerShell and run:

```powershell
cd F:\Y2S2_DS\-Fault-Tolerant-Distributed-Payment-processing-System
.\start_consensus_cluster.ps1
```

This starts 3 Raft nodes on ports **8071**, **8072**, and **8073**.

> **Note:** Wait ~5 seconds for leader election to complete.

---

### Step 2: Verify Nodes are Running

Check status of each node:

```powershell
Invoke-WebRequest -Uri "http://localhost:8071/raft/status" | Select-Object -ExpandProperty Content
Invoke-WebRequest -Uri "http://localhost:8072/raft/status" | Select-Object -ExpandProperty Content
Invoke-WebRequest -Uri "http://localhost:8073/raft/status" | Select-Object -ExpandProperty Content
```

**Expected Output:** One node should be `LEADER`, others should be `FOLLOWER`.

```json
{"nodeId":"node1","state":"LEADER","currentTerm":2,"votedFor":"node1","commitIndex":0,"lastApplied":0}
{"nodeId":"node2","state":"FOLLOWER","currentTerm":2,"votedFor":"node1","commitIndex":0,"lastApplied":0}
{"nodeId":"node3","state":"FOLLOWER","currentTerm":2,"votedFor":"node1","commitIndex":0,"lastApplied":0}
```

---

### Step 3: Submit a Payment (via Terminal)

Send a payment command to the leader:

```powershell
Invoke-WebRequest -Uri "http://localhost:8071/raft/submit" -Method POST -ContentType "application/json" -Body '{"command":"PAYMENT:user1:100:USD"}'
```

**Expected Output:**

```
StatusCode        : 200
StatusDescription : OK
Content           : {"success":true,"index":1,"term":2,"leaderId":"node1"}
```

---

### Step 4: Open the Demo UI Dashboard

1. Navigate to the `demo-ui` folder
2. Open `index.html` in your browser (double-click or right-click → Open with browser)

**Dashboard Features:**
- Real-time node status (LEADER/FOLLOWER/DOWN)
- Cluster statistics (term, commit index, total entries)
- Submit payments via the form
- View Raft log entries

---

### Step 5: Test Fault Tolerance (Optional)

**Simulate Node Failure:**
1. Close one of the node terminal windows (e.g., Node 3)
2. Watch the dashboard - it will show that node as **DOWN**
3. Submit a payment - it still works with 2 nodes!

**Simulate Leader Failure:**
1. Identify which node is the LEADER
2. Close that terminal window
3. Watch the dashboard - a new leader will be elected (~3-5 seconds)

> **Important:** Minimum 2 nodes required for consensus (quorum = majority).

---

## Quick Reference

| Port | Node | Profile |
|------|------|---------|
| 8071 | node1 | application-node1.properties |
| 8072 | node2 | application-node2.properties |
| 8073 | node3 | application-node3.properties |

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/raft/status` | GET | Get node status |
| `/raft/submit` | POST | Submit payment command |
| `/raft/log` | GET | Get Raft log entries |

---

## Troubleshooting

**Q: All nodes show CANDIDATE, no LEADER?**
A: Need at least 2 nodes running. With only 1 node, it can't get majority votes.

**Q: Payment submission fails?**
A: Make sure you're sending to the LEADER node, or wait for leader election.

**Q: UI not loading node data?**
A: Check that nodes are running and CORS is enabled (CorsConfig.java).
