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

pub fn remote_viewer_ports() -> (u16, u16) {
    (
        fabushi_constants::sand_box::SAND_BOX_PRIMARY_NOVNC_PORT,
        fabushi_constants::sand_box::SAND_BOX_FORK_NOVNC_PORT,
    )
}

pub fn remote_viewer_url(
    proxy_base_url: &str,
    network_token: &str,
    session_token: Option<&str>,
    special_treatment: bool,
) -> String {
    fabushi_constants::sand_box::build_sand_box_no_vnc_url(
        proxy_base_url,
        network_token,
        session_token,
        special_treatment,
    )
}

#[cfg(test)]
mod constants_wiring_tests {
    use super::*;

    #[test]
    fn remote_runner_uses_canonical_box_viewer_contract() {
        assert_eq!(remote_viewer_ports(), (6080, 6081));
        let url = remote_viewer_url("https://proxy.example", "network", None, true);
        assert!(fabushi_constants::sand_box::is_sand_special_treatment_no_vnc_url(&url));
    }
}
