use super::gateway_errors::GatewayError;

pub trait HttpGatewayTransport {
    fn post_command(&mut self, base_url: &str, method: &str, args_json: &str) -> Result<String, GatewayError>;
}

pub struct GatewayClient<T: HttpGatewayTransport> {
    base_url: String,
    transport: T,
}

impl<T: HttpGatewayTransport> GatewayClient<T> {
    pub fn new(base_url: impl Into<String>, transport: T) -> Result<Self, &'static str> {
        let base_url=base_url.into();
        if !(base_url.starts_with("http://") || base_url.starts_with("https://")) { return Err("gateway base URL must be HTTP(S)"); }
        Ok(Self { base_url, transport })
    }
    pub fn dispatch(&mut self, method: &str, args_json: &str) -> Result<String, GatewayError> {
        if method.trim().is_empty() { return Err(GatewayError::Command("method must not be empty".into())); }
        self.transport.post_command(&self.base_url, method, args_json)
    }
    pub fn base_url(&self) -> &str { &self.base_url }
}
