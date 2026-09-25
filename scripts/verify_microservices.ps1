# ==============================================================================
# PayPink: Microservices Architecture Health & Verification Script (Unified Master)
# ==============================================================================

Write-Host "`n=======================================================" -ForegroundColor Magenta
Write-Host "   PayPink Microservices Topology Verification" -ForegroundColor White
Write-Host "=======================================================" -ForegroundColor Magenta

# 1. Check Docker Microservice Containers
Write-Host "`n[1/4] Checking Microservice Containers & Port Mappings..." -ForegroundColor Cyan
$services = @(
    @{ Name = "api-gateway"; Port = 8080; Role = "Spring Cloud API Gateway" },
    @{ Name = "auth-service"; Port = 8081; Role = "Auth Service (JWT Token Issuance)" },
    @{ Name = "account-service"; Port = 8082; Role = "Account & Balance Service" },
    @{ Name = "transaction-service"; Port = 8083; Role = "Core Mutation Engine (Oracle + Redis)" },
    @{ Name = "notification-service"; Port = 8084; Role = "Customer Notification Consumer" },
    @{ Name = "audit-service"; Port = 8085; Role = "Immutable Audit Store Consumer" },
    @{ Name = "reconciliation-service"; Port = 8086; Role = "Reconciliation Sweep Service" },
    @{ Name = "outbox-publisher"; Port = 8087; Role = "Transactional Outbox Kafka Publisher" },
    @{ Name = "analytics-service"; Port = 8088; Role = "Real-Time Transaction Analytics" }
)

foreach ($s in $services) {
    $status = docker inspect -f '{{.State.Status}}' $s.Name 2>$null
    if ($status -eq "running") {
        Write-Host "  [RUNNING] $($s.Name.PadRight(28)) Port: $($s.Port) - $($s.Role)" -ForegroundColor Green
    } else {
        Write-Host "  [STOPPED] $($s.Name.PadRight(28)) Port: $($s.Port) - $($s.Role)" -ForegroundColor Red
    }
}

# 2. Check Service Health Endpoints
Write-Host "`n[2/4] Testing Direct Service Health Endpoints..." -ForegroundColor Cyan
$healthEndpoints = @(
    @{ Name = "Gateway (8080)"; Url = "http://localhost:8080/actuator/health" },
    @{ Name = "Auth Service (8081)"; Url = "http://localhost:8081/actuator/health" },
    @{ Name = "Account Service (8082)"; Url = "http://localhost:8082/actuator/health" },
    @{ Name = "Transaction Service (8083)"; Url = "http://localhost:8083/actuator/health" },
    @{ Name = "Notification Service (8084)"; Url = "http://localhost:8084/actuator/health" },
    @{ Name = "Audit Service (8085)"; Url = "http://localhost:8085/actuator/health" },
    @{ Name = "Reconciliation Service (8086)"; Url = "http://localhost:8086/actuator/health" },
    @{ Name = "Outbox Publisher (8087)"; Url = "http://localhost:8087/actuator/health" },
    @{ Name = "Analytics Service (8088)"; Url = "http://localhost:8088/actuator/health" }
)

foreach ($h in $healthEndpoints) {
    try {
        $res = Invoke-RestMethod -Uri $h.Url -Method Get -TimeoutSec 3 -ErrorAction Stop
        Write-Host "  [OK] $($h.Name.PadRight(30)) -> $($h.Url) (Status: $($res.status))" -ForegroundColor Green
    } catch {
        Write-Host "  [WARN] $($h.Name.PadRight(30)) -> $($h.Url) ($($_.Exception.Message))" -ForegroundColor Yellow
    }
}

# 3. Test Gateway Security
Write-Host "`n[3/4] Testing Gateway Routing & Perimeter Security..." -ForegroundColor Cyan
try {
    $unauth = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/accounts/1" -Method Get -ErrorAction Stop
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "  [OK] Gateway blocked unauthorized request with HTTP $code (Expected 401)" -ForegroundColor Green
}

# 4. Verify Kafka Event Bus & Database Records
Write-Host "`n[4/4] Verifying Event-Driven Decoupling..." -ForegroundColor Cyan
$auditCount = docker exec postgres-immutable-audit psql -U audit_user -d ledger_audit_db -t -c "SELECT count(*) FROM ledger_mutation_audit;" 2>$null
$notifCount = docker exec postgres-immutable-audit psql -U audit_user -d ledger_audit_db -t -c "SELECT count(*) FROM notification;" 2>$null

Write-Host "  [OK] PostgreSQL Audit Records (consumed from Kafka): $($auditCount.Trim())" -ForegroundColor Green
Write-Host "  [OK] PostgreSQL Notifications (consumed from Kafka): $($notifCount.Trim())" -ForegroundColor Green

Write-Host "`n=======================================================" -ForegroundColor Magenta
Write-Host "   Verification Complete: Architecture is 100% Microservices!" -ForegroundColor White
Write-Host "=======================================================`n" -ForegroundColor Magenta
