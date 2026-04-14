#!/bin/bash
# ─────────────────────────────────────────────────────────────
# MyTaxERP — EC2 Deployment Script
# Run this on your EC2 instance after cloning the repo.
# Usage: bash deploy.sh <EC2_PUBLIC_IP>
# ─────────────────────────────────────────────────────────────

set -e

EC2_IP=${1:-"YOUR_EC2_IP"}
REPO_DIR=$(pwd)

echo "=== Deploying MyTaxERP to EC2: $EC2_IP ==="

# ── 1. Build Backend ──────────────────────────────────────────
echo ""
echo ">>> Building Spring Boot backend..."
cd "$REPO_DIR/backend"
chmod +x mvnw
./mvnw clean package -DskipTests
echo ">>> Backend JAR built: target/itr-filing-assistant-1.0.0.jar"

# ── 2. Configure Frontend API URL ────────────────────────────
echo ""
echo ">>> Configuring frontend for EC2 IP: $EC2_IP"
cd "$REPO_DIR/frontend"
cat > .env.production << EOF
NEXT_PUBLIC_API_URL=http://$EC2_IP:8080/api
EOF
echo ">>> Frontend .env.production written"

# ── 3. Build Frontend ─────────────────────────────────────────
echo ""
echo ">>> Installing frontend dependencies..."
npm ci
echo ">>> Building Next.js frontend..."
npm run build
echo ">>> Frontend build complete"

# ── 4. Install PM2 (process manager) ─────────────────────────
echo ""
echo ">>> Installing PM2..."
sudo npm install -g pm2

# ── 5. Start Backend with PM2 ────────────────────────────────
echo ""
echo ">>> Starting backend..."
cd "$REPO_DIR/backend"
pm2 delete itr-backend 2>/dev/null || true
pm2 start "java -jar target/itr-filing-assistant-1.0.0.jar --app.cors.allowed-origins=http://$EC2_IP:3000,http://$EC2_IP" \
    --name itr-backend \
    --log "$REPO_DIR/logs/backend.log"

# ── 6. Start Frontend with PM2 ───────────────────────────────
echo ""
echo ">>> Starting frontend..."
cd "$REPO_DIR/frontend"
pm2 delete itr-frontend 2>/dev/null || true
pm2 start "npm start -- -p 3000" \
    --name itr-frontend \
    --log "$REPO_DIR/logs/frontend.log"

# ── 7. Save PM2 config (auto-restart on reboot) ──────────────
pm2 save
pm2 startup | tail -1 | sudo bash

echo ""
echo "=== Deployment complete! ==="
echo "Frontend: http://$EC2_IP:3000"
echo "Backend:  http://$EC2_IP:8080/api"
echo ""
echo "Check logs: pm2 logs"
echo "Check status: pm2 status"
