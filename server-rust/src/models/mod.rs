pub mod achievements;
pub mod auth;
pub mod character;
pub mod dice;
pub mod geolocation;
pub mod ml_inference;
pub mod multiplayer;
pub mod quest;
pub mod rag;
pub mod rewards;
pub mod search;
pub mod skill_tree;
pub mod task;
pub mod user;

// Re-export used across modules
pub use quest::Quest;
pub use user::{User, UserCreate, UserOut};

// Re-export RAG-related types so other modules can import them from crate::models
pub use crate::models::rag::{
    GeneratedTask, QuestGenerationRequest, QuestGenerationResponse, RagKnowledge, RagKnowledgeOut,
    TaskEnhancementRequest, TaskEnhancementResponse,
};
