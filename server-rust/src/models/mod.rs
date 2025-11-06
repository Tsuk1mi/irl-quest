pub mod quest;
pub mod task;
pub mod user;
pub mod search;
pub mod rag;
pub mod auth;
pub mod ml_inference;
pub mod rewards;
pub mod dice;
pub mod character;
pub mod multiplayer;
pub mod geolocation;
pub mod achievements;
pub mod skill_tree;

// Re-export used across modules
pub use quest::Quest;
pub use task::Task;
pub use user::{User, UserCreate, UserOut};

// Re-export RAG-related types so other modules can import them from crate::models
pub use crate::models::rag::{
    GeneratedTask,
    QuestGenerationRequest,
    QuestGenerationResponse,
    TaskEnhancementRequest,
    TaskEnhancementResponse,
    RagKnowledge,
    RagKnowledgeCreate,
    RagKnowledgeOut,
};
