use std::collections::BTreeMap;

#[derive(Default)]
pub struct OAuthLoopbackRegistry {
    pending: BTreeMap<String, String>,
}

impl OAuthLoopbackRegistry {
    pub fn register(&mut self, state: impl Into<String>, provider: impl Into<String>) -> Result<(), &'static str> {
        let state=state.into();
        if state.len() < 16 { return Err("OAuth state token is too short"); }
        if self.pending.insert(state, provider.into()).is_some() { return Err("OAuth state already registered"); }
        Ok(())
    }
    pub fn consume(&mut self, state: &str) -> Option<String> { self.pending.remove(state) }
    pub fn pending_count(&self) -> usize { self.pending.len() }
}
