# Multi-stage build для Rust сервера
FROM rust:1.75-slim as builder

# Установить системные зависимости
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Создать пользователя для сборки
WORKDIR /app

# Скопировать манифесты
COPY Cargo.toml Cargo.lock ./

# Создать dummy main для кэширования зависимостей
RUN mkdir src && \
    echo "fn main() {}" > src/main.rs && \
    cargo build --release && \
    rm -rf src

# Скопировать исходники
COPY src ./src
COPY migrations ./migrations

# Собрать приложение
RUN cargo build --release --bin irl-quest-server

# Runtime стадия
FROM debian:bookworm-slim

# Установить runtime зависимости
RUN apt-get update && apt-get install -y \
    ca-certificates \
    libssl3 \
    && rm -rf /var/lib/apt/lists/*

# Создать пользователя без root прав
RUN useradd -m -u 1000 appuser

WORKDIR /app

# Скопировать бинарник из builder
COPY --from=builder /app/target/release/irl-quest-server ./server
COPY --from=builder /app/migrations ./migrations

# Изменить владельца
RUN chown -R appuser:appuser /app

USER appuser

# Порт
EXPOSE 8003

# Переменные окружения по умолчанию
ENV DATABASE_URL=postgresql://postgres:password@postgres:5432/irl_quest
ENV PORT=8003
ENV RUST_LOG=info

# Запуск
CMD ["./server"]

