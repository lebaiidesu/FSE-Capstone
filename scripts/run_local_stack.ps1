# ==============================================================================
# PayPink Local Stack Launcher (Using Local Java & Cached Datastore Images)
# Use this when Docker Hub image pulls are blocked by TLS/Proxy firewalls
# ==============================================================================

Write-Host "`n=======================================================" -ForegroundColor Magenta
Write-Host "   PayPink Local Microservices Launcher" -ForegroundColor White
Write-Host "=======================================================" -ForegroundColor Magenta

# 1. Start Datastores & Kafka via Docker using pre-pulled local images
Write-Host "`n[1/3] Starting Local Datastores (Postgres, Redis, Kafka)..." -ForegroundColor Cyan
docker run -d --name redis-idempotency-matrix -p 6380:6379 redis:7.4-alpine 2>$null
docker run -d --name postgres-immutable-audit -p 5432:5432 -e POSTGRES_DB=ledger_audit_db -e POSTGRES_USER=audit_user -e POSTGRES_PASSWORD=AuditSecretPassword123 postgres:15-alpine 2>$null
docker run -d --name zookeeper -p 2181:2181 -e ZOOKEEPER_CLIENT_PORT=2181 confluentinc/cp-zookeeper:7.5.0 2>$null
docker run -d --name kafka-event-bus -p 9092:9092 -e KAFKA_BROKER_ID=1 -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 confluentinc/cp-kafka:7.5.0 2>$null

Write-Host "  [OK] Datastores started!" -ForegroundColor Green

# 2. Check compiled JARs
$backendDir = Resolve-Path "$PSScriptRoot\..\backend"
Write-Host "`n[2/3] Checking compiled JAR files in $backendDir..." -ForegroundColor Cyan

$services = @(
    @{ Name = "ledger-common"; Jar = "$backendDir\ledger-common\target\ledger-common-1.0.0-SNAPSHOT.jar" },
    @{ Name = "auth-service"; Jar = "$backendDir\auth-service\target\auth-service-1.0.0-SNAPSHOT.jar"; Port = 8081 },
    @{ Name = "account-service"; Jar = "$backendDir\account-service\target\account-service-1.0.0-SNAPSHOT.jar"; Port = 8082 },
    @{ Name = "transaction-service"; Jar = "$backendDir\transaction-service\target\transaction-service-1.0.0-SNAPSHOT.jar"; Port = 8083 },
    @{ Name = "notification-service"; Jar = "$backendDir\notification-service\target\notification-service-1.0.0-SNAPSHOT.jar"; Port = 8084 },
    @{ Name = "audit-service"; Jar = "$backendDir\audit-service\target\audit-service-1.0.0-SNAPSHOT.jar"; Port = 8085 },
    @{ Name = "reconciliation-service"; Jar = "$backendDir\reconciliation-service\target\reconciliation-service-1.0.0-SNAPSHOT.jar"; Port = 8086 },
    @{ Name = "outbox-publisher"; Jar = "$backendDir\outbox-publisher\target\outbox-publisher-1.0.0-SNAPSHOT.jar"; Port = 8087 },
    @{ Name = "analytics-service"; Jar = "$backendDir\analytics-service\target\analytics-service-1.0.0-SNAPSHOT.jar"; Port = 8088 },
    @{ Name = "api-gateway"; Jar = "$backendDir\api-gateway\target\api-gateway-1.0.0-SNAPSHOT.jar"; Port = 8080 }
)

foreach ($s in $services) {
    if (Test-Path $s.Jar) {
        Write-Host "  [OK] Found $($s.Name) JAR" -ForegroundColor Green
    } else {
        Write-Host "  [MISSING] $($s.Jar) - Run 'mvn clean package -DskipTests' first" -ForegroundColor Red
    }
}

Write-Host "`n[3/3] To start microservices locally, run these background jobs or execute:" -ForegroundColor Cyan
Write-Host "  Start-Job { java -jar $backendDir\auth-service\target\auth-service-1.0.0-SNAPSHOT.jar }" -ForegroundColor Yellow
Write-Host "  Start-Job { java -jar $backendDir\account-service\target\account-service-1.0.0-SNAPSHOT.jar }" -ForegroundColor Yellow
Write-Host "  Start-Job { java -jar $backendDir\transaction-service\target\transaction-service-1.0.0-SNAPSHOT.jar }" -ForegroundColor Yellow
Write-Host "  Start-Job { java -jar $backendDir\api-gateway\target\api-gateway-1.0.0-SNAPSHOT.jar }" -ForegroundColor Yellow

Write-Host "`n=======================================================" -ForegroundColor Magenta
