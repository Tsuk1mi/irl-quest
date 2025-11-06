/// WebSocket сервис для realtime коммуникации
use crate::models::multiplayer::WsMessage;
use axum::extract::ws::{Message, WebSocket};
use futures::stream::{SplitSink, SplitStream};
use futures::{SinkExt, StreamExt};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::{broadcast, RwLock};

/// WebSocket менеджер комнат
#[derive(Clone)]
pub struct WebSocketManager {
    rooms: Arc<RwLock<HashMap<String, broadcast::Sender<String>>>>,
}

impl WebSocketManager {
    pub fn new() -> Self {
        Self {
            rooms: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    /// Получить или создать комнату
    pub async fn get_or_create_room(&self, room_id: &str) -> broadcast::Sender<String> {
        let mut rooms = self.rooms.write().await;
        
        rooms
            .entry(room_id.to_string())
            .or_insert_with(|| {
                let (tx, _rx) = broadcast::channel(100);
                tracing::info!("Created new WebSocket room: {}", room_id);
                tx
            })
            .clone()
    }

    /// Отправить сообщение в комнату
    pub async fn send_to_room(&self, room_id: &str, message: &str) -> Result<(), String> {
        let rooms = self.rooms.read().await;
        
        if let Some(tx) = rooms.get(room_id) {
            tx.send(message.to_string())
                .map_err(|e| format!("Failed to send message: {}", e))?;
            Ok(())
        } else {
            Err(format!("Room {} not found", room_id))
        }
    }

    /// Удалить пустые комнаты
    pub async fn cleanup_empty_rooms(&self) {
        let mut rooms = self.rooms.write().await;
        rooms.retain(|room_id, tx| {
            let has_receivers = tx.receiver_count() > 0;
            if !has_receivers {
                tracing::info!("Removing empty room: {}", room_id);
            }
            has_receivers
        });
    }

    /// Получить количество активных комнат
    pub async fn active_rooms_count(&self) -> usize {
        self.rooms.read().await.len()
    }
}

/// Обработчик WebSocket соединения
pub struct WebSocketHandler {
    user_id: i32,
    username: String,
    manager: WebSocketManager,
}

impl WebSocketHandler {
    pub fn new(user_id: i32, username: String, manager: WebSocketManager) -> Self {
        Self {
            user_id,
            username,
            manager,
        }
    }

    /// Обработать WebSocket соединение
    pub async fn handle_socket(self, socket: WebSocket) {
        let (sender, receiver) = socket.split();

        tracing::info!("WebSocket connection established for user: {}", self.user_id);

        // Сохранить user_id для логирования
        let user_id = self.user_id;
        
        // Запустить задачи приема и отправки
        let recv_task = tokio::spawn(self.clone().receive_messages(receiver));
        let send_task = tokio::spawn(self.send_messages(sender));

        // Ждать завершения любой из задач
        tokio::select! {
            _ = recv_task => {
                tracing::info!("Receive task completed for user {}", user_id);
            }
            _ = send_task => {
                tracing::info!("Send task completed for user {}", user_id);
            }
        }

        tracing::info!("WebSocket connection closed for user: {}", user_id);
    }

    /// Прием сообщений от клиента
    async fn receive_messages(
        mut self,
        mut receiver: SplitStream<WebSocket>,
    ) {
        let mut current_room: Option<String> = None;

        while let Some(msg_result) = receiver.next().await {
            match msg_result {
                Ok(msg) => {
                    if let Err(e) = self.process_message(msg, &mut current_room).await {
                        tracing::error!("Error processing message: {}", e);
                    }
                }
                Err(e) => {
                    tracing::error!("WebSocket error for user {}: {}", self.user_id, e);
                    break;
                }
            }
        }
    }

    /// Отправка сообщений клиенту
    async fn send_messages(
        self,
        mut sender: SplitSink<WebSocket, Message>,
    ) {
        // Здесь будет логика отправки сообщений из broadcast channel
        // Для упрощения - просто держим соединение открытым
        
        // В реальной реализации подписываемся на broadcast channel
        // и отправляем сообщения клиенту
        
        loop {
            tokio::time::sleep(tokio::time::Duration::from_secs(30)).await;
            
            // Ping для поддержания соединения
            if sender.send(Message::Ping(vec![])).await.is_err() {
                break;
            }
        }
    }

    /// Обработать сообщение от клиента
    async fn process_message(
        &mut self,
        msg: Message,
        current_room: &mut Option<String>,
    ) -> Result<(), String> {
        match msg {
            Message::Text(text) => {
                // Парсим JSON сообщение
                let ws_msg: WsMessage = serde_json::from_str(&text)
                    .map_err(|e| format!("Failed to parse message: {}", e))?;

                match ws_msg {
                    WsMessage::JoinRoom { room_id } => {
                        self.join_room(&room_id).await?;
                        *current_room = Some(room_id.clone());
                        
                        // Отправить уведомление в комнату
                        let join_msg = WsMessage::UserJoined {
                            room_id: room_id.clone(),
                            user_id: self.user_id,
                            username: self.username.clone(),
                        };
                        let json = serde_json::to_string(&join_msg).unwrap();
                        self.manager.send_to_room(&room_id, &json).await?;
                    }
                    
                    WsMessage::LeaveRoom { room_id } => {
                        // Отправить уведомление о выходе
                        let leave_msg = WsMessage::UserLeft {
                            room_id: room_id.clone(),
                            user_id: self.user_id,
                        };
                        let json = serde_json::to_string(&leave_msg).unwrap();
                        self.manager.send_to_room(&room_id, &json).await?;
                        
                        *current_room = None;
                    }
                    
                    WsMessage::ChatMessage { room_id, message } => {
                        // Отправить сообщение в комнату
                        let chat_msg = WsMessage::Chat {
                            room_id: room_id.clone(),
                            user_id: self.user_id,
                            username: self.username.clone(),
                            message,
                            timestamp: chrono::Utc::now().timestamp(),
                        };
                        let json = serde_json::to_string(&chat_msg).unwrap();
                        self.manager.send_to_room(&room_id, &json).await?;
                    }
                    
                    WsMessage::QuestUpdate { quest_id, progress } => {
                        if let Some(room_id) = current_room {
                            let update_msg = WsMessage::QuestProgress {
                                quest_id,
                                progress,
                                user_id: self.user_id,
                            };
                            let json = serde_json::to_string(&update_msg).unwrap();
                            self.manager.send_to_room(room_id, &json).await?;
                        }
                    }
                    
                    WsMessage::Ping => {
                        // Отправить Pong (обрабатывается в send_messages)
                    }
                    
                    _ => {
                        tracing::warn!("Unexpected message type from client");
                    }
                }

                Ok(())
            }
            Message::Close(_) => {
                tracing::info!("WebSocket closed by client: {}", self.user_id);
                Err("Connection closed".to_string())
            }
            _ => Ok(()),
        }
    }

    /// Присоединиться к комнате
    async fn join_room(&self, room_id: &str) -> Result<(), String> {
        let _tx = self.manager.get_or_create_room(room_id).await;
        tracing::info!("User {} joined room: {}", self.user_id, room_id);
        Ok(())
    }
}

impl Clone for WebSocketHandler {
    fn clone(&self) -> Self {
        Self {
            user_id: self.user_id,
            username: self.username.clone(),
            manager: self.manager.clone(),
        }
    }
}

