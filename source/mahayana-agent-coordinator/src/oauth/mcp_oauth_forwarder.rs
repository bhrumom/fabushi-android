use super::mcp_oauth_callback_listener::OAuthCallback;
use super::mcp_oauth_loopback_registry::OAuthLoopbackRegistry;

pub struct OAuthForwarder {
    registry: OAuthLoopbackRegistry,
}

impl OAuthForwarder {
    pub fn new(registry: OAuthLoopbackRegistry) -> Self { Self { registry } }

    pub fn register(
        &mut self,
        state: impl Into<String>,
        provider: impl Into<String>,
    ) -> Result<(), &'static str> {
        self.registry.register(state, provider)
    }

    pub fn pending_count(&self) -> usize {
        self.registry.pending_count()
    }

    pub fn forward(&mut self, callback: OAuthCallback) -> Result<(String, OAuthCallback), &'static str> {
        callback.validate()?;
        let provider=self.registry.consume(&callback.state).ok_or("OAuth callback state is unknown or already consumed")?;
        Ok((provider, callback))
    }
}

impl Default for OAuthForwarder {
    fn default() -> Self {
        Self::new(OAuthLoopbackRegistry::default())
    }
}
