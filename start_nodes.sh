#!/bin/bash
echo "Starting 3 payment nodes..."
 
# Make sure ZooKeeper is running first
echo "Verify ZooKeeper is running: bin/zkServer.sh status"
 
cd fault-tolerance
mvn package -DskipTests -q
 
echo "Starting Node 1 on port 8081..."
java -jar target/fault-tolerance-1.0-SNAPSHOT.jar \
  --spring.profiles.active=node1 > ../logs/node1.log 2>&1 &
 
sleep 2
 
echo "Starting Node 2 on port 8082..."
java -jar target/fault-tolerance-1.0-SNAPSHOT.jar \
  --spring.profiles.active=node2 > ../logs/node2.log 2>&1 &
 
sleep 2
 
echo "Starting Node 3 on port 8083..."
java -jar target/fault-tolerance-1.0-SNAPSHOT.jar \
  --spring.profiles.active=node3 > ../logs/node3.log 2>&1 &
 
echo "All nodes started. Checking status in 5 seconds..."
sleep 5
 
echo "Node 1 status:"
curl -s http://localhost:8081/fault/status | python3 -m json.tool
echo "Node 2 status:"
curl -s http://localhost:8082/fault/status | python3 -m json.tool
