//! Android-owned contracts shared across renderer bridge, Coordinator, Host, and Runners.
//! Clean-room implementation from the Fabushi Android Spec; no reconstructed Grok source is copied.

pub mod coordinator;
pub mod execution;
pub mod rpc;

pub use coordinator::{
    CancelRequest, CoordinatorEvent, CoordinatorFailure, CoordinatorFailureCode, CoordinatorReply,
    CoordinatorRequest, ResyncRequest, ResyncSnapshot, COORDINATOR_PROTOCOL_VERSION,
};
pub use execution::{
    ExecutionCapability, ExecutionError, ExecutionRequest, ExecutionResult, ExecutionTarget,
};

pub mod auth;
pub mod gateway_wire;
pub mod media;

pub mod deep_link;
pub mod gateway_reachability;
pub mod webauthn_gateway;
pub mod os_notification;
