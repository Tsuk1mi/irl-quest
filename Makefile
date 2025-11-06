# Makefile для IRL Quest

.PHONY: help build start stop restart logs clean test migrate

# Цвета для вывода
GREEN  := \033[0;32m
YELLOW := \033[0;33m
NC     := \033[0m # No Color

help: ## Показать эту справку
	@echo "IRL Quest - Команды для управления"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  ${GREEN}%-15s${NC} %s\n", $$1, $$2}'

build: ## Собрать все Docker контейнеры
	@echo "${GREEN}Сборка контейнеров...${NC}"
	docker-compose build

start: ## Запустить все сервисы
	@echo "${GREEN}Запуск сервисов...${NC}"
	docker-compose up -d
	@echo "${GREEN}Сервисы запущены!${NC}"
	@echo "🌐 API: http://localhost:8003"
	@echo "📊 Grafana: http://localhost:3000 (admin/admin)"
	@echo "📈 Prometheus: http://localhost:9090"
	@echo "💾 MinIO Console: http://localhost:9001"

stop: ## Остановить все сервисы
	@echo "${YELLOW}Остановка сервисов...${NC}"
	docker-compose down

restart: stop start ## Перезапустить все сервисы

logs: ## Показать логи всех сервисов
	docker-compose logs -f

logs-server: ## Показать логи только сервера
	docker-compose logs -f server

logs-db: ## Показать логи PostgreSQL
	docker-compose logs -f postgres

clean: ## Остановить и удалить все контейнеры и volumes
	@echo "${YELLOW}Очистка...${NC}"
	docker-compose down -v
	@echo "${GREEN}Очистка завершена${NC}"

migrate: ## Запустить миграции базы данных
	@echo "${GREEN}Запуск миграций...${NC}"
	docker-compose exec server sqlx migrate run
	@echo "${GREEN}Миграции применены${NC}"

test: ## Запустить тесты
	cd server-rust && cargo test

dev: ## Запустить в режиме разработки (только БД)
	docker-compose up -d postgres redis minio
	@echo "${GREEN}Инфраструктура запущена${NC}"
	@echo "Теперь запустите сервер локально:"
	@echo "  cd server-rust && cargo run"

health: ## Проверить статус всех сервисов
	@echo "${GREEN}Проверка статусов...${NC}"
	@docker-compose ps
	@echo ""
	@echo "${GREEN}Health checks:${NC}"
	@curl -s http://localhost:8003/health && echo "✅ Server OK" || echo "❌ Server DOWN"
	@curl -s http://localhost:9090/-/healthy && echo "✅ Prometheus OK" || echo "❌ Prometheus DOWN"

backup: ## Создать бэкап базы данных
	@echo "${GREEN}Создание бэкапа...${NC}"
	@mkdir -p ./backups
	docker-compose exec -T postgres pg_dump -U postgres irl_quest > ./backups/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "${GREEN}Бэкап создан в ./backups/${NC}"

restore: ## Восстановить из бэкапа (укажите BACKUP_FILE=файл.sql)
	@echo "${YELLOW}Восстановление из бэкапа...${NC}"
	docker-compose exec -T postgres psql -U postgres irl_quest < $(BACKUP_FILE)
	@echo "${GREEN}Восстановление завершено${NC}"

shell-db: ## Подключиться к PostgreSQL
	docker-compose exec postgres psql -U postgres irl_quest

shell-redis: ## Подключиться к Redis
	docker-compose exec redis redis-cli -a $${REDIS_PASSWORD:-redis_password}

shell-server: ## Подключиться к контейнеру сервера
	docker-compose exec server /bin/bash
