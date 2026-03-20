@echo off
REM =============================================================================
REM Start Raft Cluster - 3 Node Setup
REM Run this script to start all 3 nodes in separate windows
REM =============================================================================

echo Starting Raft Consensus Cluster (3 nodes)...
echo.

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0
set CONSENSUS_DIR=%SCRIPT_DIR%..

echo Starting Node 1 on port 8071...
start "Raft Node 1" cmd /k "cd /d %CONSENSUS_DIR% && mvn spring-boot:run -Dspring-boot.run.arguments=\"--app.node.id=node1 --server.port=8071 --app.peers=http://localhost:8072,http://localhost:8073\""

timeout /t 3 /nobreak >nul

echo Starting Node 2 on port 8072...
start "Raft Node 2" cmd /k "cd /d %CONSENSUS_DIR% && mvn spring-boot:run -Dspring-boot.run.arguments=\"--app.node.id=node2 --server.port=8072 --app.peers=http://localhost:8071,http://localhost:8073\""

timeout /t 3 /nobreak >nul

echo Starting Node 3 on port 8073...
start "Raft Node 3" cmd /k "cd /d %CONSENSUS_DIR% && mvn spring-boot:run -Dspring-boot.run.arguments=\"--app.node.id=node3 --server.port=8073 --app.peers=http://localhost:8071,http://localhost:8072\""

echo.
echo All nodes started!
echo.
echo Wait about 5-10 seconds for nodes to initialize and elect a leader.
echo Then run test-cluster.bat to test the cluster.
echo.
echo To stop all nodes, close the terminal windows or press Ctrl+C in each.
echo.
pause
