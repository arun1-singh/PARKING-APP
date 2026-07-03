#!/usr/bin/env bash
# -------------------------------------------------
# run_all_tests.sh – launch backend + execute Android E2E tests
# -------------------------------------------------

# 1️⃣ Start backend containers (FastAPI + Postgres)
echo "🛳️  Starting backend Docker stack ..."
cd "$(dirname "$0")/../Backend"   # go to the Backend folder
docker compose -f docker-compose.fastapi.yml up --build -d

# 2️⃣ Give the services a moment to become ready
echo "⏳  Waiting 15 seconds for the API & DB to start ..."
sleep 15

# 3️⃣ Verify the backend is reachable (optional, but helpful)
if command -v curl >/dev/null 2>&1; then
  echo "🔍  Checking FastAPI health endpoint ..."
  curl -s http://localhost:8000/health || echo "⚠️  Backend health check failed – tests may still run but could fail."
else
  echo "⚠️  curl not installed; skipping health check."
fi

# 4️⃣ Return to the Vision‑Parking project and run the tests
cd "$(dirname "$0")/../Vision-Parking"
echo "🚀  Running full Appium test suite (pytest) ..."
# Run pytest with maxfail=0 to continue after failures and generate HTML report
pytest -vv --maxfail=0 --capture=no tests/ | tee pytest_console.log

# 5️⃣ The existing pytest‑html plugin will have generated a fresh report:
#    ./tests/report.html
echo "✅  Test run finished – open ./tests/report.html to review every test case."
