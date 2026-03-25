#!/bin/bash
# =============================================================================
# Raft Cluster Test Script
# Manual testing for Raft consensus implementation
# =============================================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Node ports
NODE1_PORT=8071
NODE2_PORT=8072
NODE3_PORT=8073

# Base URL
BASE_URL="http://localhost"

print_header() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# Check if a node is up
check_node() {
    local port=$1
    local response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL:$port/health" 2>/dev/null)
    if [ "$response" == "200" ]; then
        return 0
    else
        return 1
    fi
}

# Get node status
get_status() {
    local port=$1
    curl -s "$BASE_URL:$port/raft/status" | jq '.' 2>/dev/null
}

# Submit a payment command
submit_payment() {
    local port=$1
    local from=$2
    local to=$3
    local amount=$4
    local currency=${5:-USD}

    local command="PAYMENT:$from:$to:$amount:$currency"
    curl -s -X POST "$BASE_URL:$port/raft/submit" \
        -H "Content-Type: application/json" \
        -d "{\"command\": \"$command\"}" | jq '.'
}

# Find the leader
find_leader() {
    for port in $NODE1_PORT $NODE2_PORT $NODE3_PORT; do
        if check_node $port; then
            local state=$(curl -s "$BASE_URL:$port/raft/status" | jq -r '.state' 2>/dev/null)
            if [ "$state" == "LEADER" ]; then
                echo $port
                return 0
            fi
        fi
    done
    echo ""
}

# =============================================================================
# TEST SCENARIOS
# =============================================================================

test_cluster_status() {
    print_header "TEST: Cluster Status Check"

    for port in $NODE1_PORT $NODE2_PORT $NODE3_PORT; do
        if check_node $port; then
            print_success "Node on port $port is UP"
            get_status $port
        else
            print_error "Node on port $port is DOWN"
        fi
        echo ""
    done
}

test_leader_election() {
    print_header "TEST: Leader Election"

    print_info "Waiting for leader election (max 10 seconds)..."

    for i in {1..20}; do
        leader_port=$(find_leader)
        if [ -n "$leader_port" ]; then
            print_success "Leader found on port $leader_port"
            get_status $leader_port
            return 0
        fi
        sleep 0.5
    done

    print_error "No leader elected within timeout"
    return 1
}

test_submit_payment() {
    print_header "TEST: Submit Payment Command"

    leader_port=$(find_leader)

    if [ -z "$leader_port" ]; then
        print_error "No leader available to submit payment"
        return 1
    fi

    print_info "Submitting payment to leader on port $leader_port"
    submit_payment $leader_port "user123" "merchant456" "99.99" "USD"
}

test_follower_redirect() {
    print_header "TEST: Follower Redirect"

    leader_port=$(find_leader)

    # Find a follower
    for port in $NODE1_PORT $NODE2_PORT $NODE3_PORT; do
        if [ "$port" != "$leader_port" ] && check_node $port; then
            print_info "Submitting to follower on port $port (should redirect)"
            submit_payment $port "userA" "userB" "50.00" "EUR"
            return 0
        fi
    done

    print_error "No follower available for test"
    return 1
}

test_multiple_payments() {
    print_header "TEST: Multiple Payment Submissions"

    leader_port=$(find_leader)

    if [ -z "$leader_port" ]; then
        print_error "No leader available"
        return 1
    fi

    print_info "Submitting 5 payments..."
    for i in {1..5}; do
        echo -e "\n${YELLOW}Payment $i:${NC}"
        submit_payment $leader_port "user$i" "merchant$i" "$((i * 100)).00" "USD"
    done
}

test_log_replication() {
    print_header "TEST: Log Replication Check"

    print_info "Checking log size on all nodes..."

    for port in $NODE1_PORT $NODE2_PORT $NODE3_PORT; do
        if check_node $port; then
            local log_size=$(curl -s "$BASE_URL:$port/raft/status" | jq '.logSize' 2>/dev/null)
            local commit_index=$(curl -s "$BASE_URL:$port/raft/status" | jq '.commitIndex' 2>/dev/null)
            echo -e "Port $port: logSize=$log_size, commitIndex=$commit_index"
        fi
    done
}

# =============================================================================
# MAIN MENU
# =============================================================================

show_menu() {
    echo ""
    echo -e "${BLUE}=== Raft Consensus Test Menu ===${NC}"
    echo "1. Check cluster status"
    echo "2. Test leader election"
    echo "3. Submit a payment (to leader)"
    echo "4. Test follower redirect"
    echo "5. Submit multiple payments"
    echo "6. Check log replication"
    echo "7. Run all tests"
    echo "8. Interactive payment submission"
    echo "0. Exit"
    echo ""
    read -p "Select option: " choice
}

interactive_payment() {
    print_header "Interactive Payment Submission"

    read -p "From user: " from
    read -p "To user: " to
    read -p "Amount: " amount
    read -p "Currency (default USD): " currency
    currency=${currency:-USD}

    leader_port=$(find_leader)
    if [ -n "$leader_port" ]; then
        submit_payment $leader_port "$from" "$to" "$amount" "$currency"
    else
        print_error "No leader available"
    fi
}

run_all_tests() {
    test_cluster_status
    test_leader_election
    test_submit_payment
    test_follower_redirect
    test_multiple_payments
    test_log_replication
}

# Main loop
main() {
    print_header "Raft Consensus Cluster Tester"
    print_info "Make sure all 3 nodes are running before testing"
    print_info "Node 1: port $NODE1_PORT"
    print_info "Node 2: port $NODE2_PORT"
    print_info "Node 3: port $NODE3_PORT"

    while true; do
        show_menu
        case $choice in
            1) test_cluster_status ;;
            2) test_leader_election ;;
            3) test_submit_payment ;;
            4) test_follower_redirect ;;
            5) test_multiple_payments ;;
            6) test_log_replication ;;
            7) run_all_tests ;;
            8) interactive_payment ;;
            0)
                print_info "Goodbye!"
                exit 0
                ;;
            *)
                print_error "Invalid option"
                ;;
        esac
    done
}

# Check if running specific test from command line
if [ "$1" == "--all" ]; then
    run_all_tests
    exit 0
fi

main
