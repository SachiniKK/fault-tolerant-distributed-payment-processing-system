@echo off
REM =============================================================================
REM Raft Cluster Test Script for Windows
REM Manual testing for Raft consensus implementation
REM Requires: curl (included in Windows 10+)
REM =============================================================================

setlocal enabledelayedexpansion

set NODE1_PORT=8071
set NODE2_PORT=8072
set NODE3_PORT=8073
set BASE_URL=http://localhost

:MENU
echo.
echo ============================================
echo    Raft Consensus Cluster Tester
echo ============================================
echo.
echo 1. Check cluster status
echo 2. Submit a payment (to leader)
echo 3. Check all node statuses
echo 4. Submit multiple test payments
echo 5. Find leader
echo 0. Exit
echo.
set /p choice="Select option: "

if "%choice%"=="1" goto CHECK_STATUS
if "%choice%"=="2" goto SUBMIT_PAYMENT
if "%choice%"=="3" goto ALL_STATUS
if "%choice%"=="4" goto MULTI_PAYMENT
if "%choice%"=="5" goto FIND_LEADER
if "%choice%"=="0" goto END
echo Invalid option
goto MENU

:CHECK_STATUS
echo.
echo Checking cluster health...
echo.
echo Node 1 (port %NODE1_PORT%):
curl -s %BASE_URL%:%NODE1_PORT%/health 2>nul || echo [OFFLINE]
echo.
echo Node 2 (port %NODE2_PORT%):
curl -s %BASE_URL%:%NODE2_PORT%/health 2>nul || echo [OFFLINE]
echo.
echo Node 3 (port %NODE3_PORT%):
curl -s %BASE_URL%:%NODE3_PORT%/health 2>nul || echo [OFFLINE]
echo.
pause
goto MENU

:ALL_STATUS
echo.
echo Node 1 Status:
curl -s %BASE_URL%:%NODE1_PORT%/raft/status 2>nul || echo [OFFLINE]
echo.
echo Node 2 Status:
curl -s %BASE_URL%:%NODE2_PORT%/raft/status 2>nul || echo [OFFLINE]
echo.
echo Node 3 Status:
curl -s %BASE_URL%:%NODE3_PORT%/raft/status 2>nul || echo [OFFLINE]
echo.
pause
goto MENU

:FIND_LEADER
echo.
echo Searching for leader...
for %%p in (%NODE1_PORT% %NODE2_PORT% %NODE3_PORT%) do (
    for /f "delims=" %%r in ('curl -s %BASE_URL%:%%p/raft/status 2^>nul') do (
        echo %%r | findstr /C:"LEADER" >nul && (
            echo Leader found on port %%p
            curl -s %BASE_URL%:%%p/raft/status
        )
    )
)
echo.
pause
goto MENU

:SUBMIT_PAYMENT
echo.
set /p from="From user: "
set /p to="To user: "
set /p amount="Amount: "
set /p port="Submit to port (8071/8072/8073): "

echo.
echo Submitting payment...
curl -s -X POST %BASE_URL%:%port%/raft/submit -H "Content-Type: application/json" -d "{\"command\": \"PAYMENT:%from%:%to%:%amount%:USD\"}"
echo.
pause
goto MENU

:MULTI_PAYMENT
echo.
echo Submitting 5 test payments to port %NODE1_PORT%...
for /L %%i in (1,1,5) do (
    echo.
    echo Payment %%i:
    curl -s -X POST %BASE_URL%:%NODE1_PORT%/raft/submit -H "Content-Type: application/json" -d "{\"command\": \"PAYMENT:user%%i:merchant%%i:%%i00.00:USD\"}"
)
echo.
pause
goto MENU

:END
echo Goodbye!
exit /b 0
