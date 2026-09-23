//! Remote/Box Runner boundary for work that should not execute in the Android app process.

pub mod cli;
#[allow(special_module_name)]
pub mod main;
pub mod server;

use fabushi_android_shared::{ExecutionError, ExecutionRequest, ExecutionResult};

pub trait RemoteExecutionTransport {
    fn execute(&mut self, request: &ExecutionRequest) -> Result<ExecutionResult, ExecutionError>;
    fn cancel(&mut self, operation_id: &str) -> Result<(), ExecutionError>;
}

pub struct RemoteRunner<T: RemoteExecutionTransport> { transport: T }

impl<T: RemoteExecutionTransport> RemoteRunner<T> {
    pub fn new(transport: T) -> Self { Self { transport } }
    pub fn execute(&mut self, request: ExecutionRequest) -> Result<ExecutionResult, ExecutionError> {
        request.validate()?;
        self.transport.execute(&request)
    }
    pub fn cancel(&mut self, operation_id: &str) -> Result<(), ExecutionError> {
        if operation_id.trim().is_empty() { return Err(ExecutionError::InvalidRequest("operation_id must not be empty".into())); }
        self.transport.cancel(operation_id)
    }
    pub fn into_transport(self) -> T { self.transport }
}
