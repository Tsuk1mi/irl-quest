#!/bin/bash
set -e

API_BASE="https://8000-iessh243ptqf83yx518ah-6532622b.e2b.dev"

echo "=== Тестирование IRL Quest API ==="

echo "1. Проверка здоровья API..."
curl -s "$API_BASE/health" | jq '.'

echo -e "\n2. Регистрация нового пользователя..."
REGISTER_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"mobile_test@example.com","username":"mobile_user","password":"testpass123"}')
echo "$REGISTER_RESPONSE" | jq '.'

echo -e "\n3. Авторизация пользователя..."
TOKEN_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/auth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'username=mobile_user&password=testpass123')
echo "$TOKEN_RESPONSE" | jq '.'

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')

echo -e "\n4. Создание задачи..."
TASK_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/tasks/" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Тестовая задача от мобильного приложения","description":"Проверка интеграции с мобильным приложением"}')
echo "$TASK_RESPONSE" | jq '.'

TASK_ID=$(echo "$TASK_RESPONSE" | jq -r '.id')

echo -e "\n5. Получение списка задач..."
curl -s -X GET "$API_BASE/api/v1/tasks/" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n6. Создание квеста..."
QUEST_RESPONSE=$(curl -s -X POST "$API_BASE/api/v1/quests/" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Мобильный квест","description":"Тестовый квест для мобильного приложения","difficulty":2}')
echo "$QUEST_RESPONSE" | jq '.'

echo -e "\n7. Получение списка квестов..."
curl -s -X GET "$API_BASE/api/v1/quests/" \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n8. Обновление задачи..."
curl -s -X PUT "$API_BASE/api/v1/tasks/$TASK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"completed":true}' | jq '.'

echo -e "\n=== API тестирование завершено успешно! ==="
