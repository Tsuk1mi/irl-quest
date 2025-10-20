pub mod quest;
pub mod task;
pub mod user;
pub mod search;
pub mod rag;

// Re-export only the types that are used across multiple modules
pub use quest::Quest;
pub use task::Task;
pub use user::{User, UserCreate, UserOut};

// Re-export RAG-related types so other modules can import them from crate::models
pub use rag::{
    GeneratedTask,
    QuestGenerationRequest,
    QuestGenerationResponse,
    TaskEnhancementRequest,
    TaskEnhancementResponse,
    RagKnowledge,
    RagKnowledgeCreate,
    RagKnowledgeOut,
};
