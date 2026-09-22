//! Android-owned contracts shared across the renderer bridge, Coordinator, Host, and Runners.
//! This is a clean implementation from the Fabushi Android Spec. It does not copy Grok source text.

pub mod coordinator;
pub mod execution;

pub use coordinator::{
    CancelRequest, CoordinatorEvent, CoordinatorFailure, CoordinatorFailureCode, CoordinatorReply,
    CoordinatorRequest, ResyncSnapshot, COORDINATOR_PROTOCOL_VERSION,
};
pub use execution::{
    ExecutionCapability, ExecutionError, ExecutionRequest, ExecutionResult, ExecutionTarget,
};
