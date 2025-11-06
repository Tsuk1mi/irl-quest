use tower_http::cors::{Any, CorsLayer};
use tower_http::trace::TraceLayer;
use tower_http::classify::{SharedClassifier, ServerErrorsAsFailures};

pub fn create_cors_layer() -> CorsLayer {
    CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any)
}

pub fn create_trace_layer() -> TraceLayer<SharedClassifier<ServerErrorsAsFailures>> {
    TraceLayer::new_for_http()
}
