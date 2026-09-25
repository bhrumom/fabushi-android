pub mod host_mcp_auth_completion;
pub mod mcp_auth_wait_registry;

pub use host_mcp_auth_completion::{
    HostMcpAuthCompletion, HostMcpAuthCompletionEvent, McpAuthCompletionRuntime,
};
pub use mcp_auth_wait_registry::{
    normalize_connector_name, McpAuthCompletionIdentity, McpAuthWaitRegistry,
    McpAuthWaitRegistration, DEFAULT_MCP_AUTH_WAIT_TTL_MS,
};
