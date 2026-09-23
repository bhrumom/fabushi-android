use super::stream_attempt::{
    ProviderFailure, StreamAttemptInput, StreamGeneration, TurnStreamProvider,
};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum AndroidInferenceMode {
    Production,
    Test,
}

/// Android-owned provider boundary used by the production turn owner.
///
/// This removes direct chat orchestration from AndroidJsonHost. The concrete network/provider
/// transport can be replaced behind this interface without changing Host/Coordinator ownership.
pub struct AndroidHostInferenceProvider {
    mode: AndroidInferenceMode,
}

impl AndroidHostInferenceProvider {
    pub fn new(mode: AndroidInferenceMode) -> Self {
        Self { mode }
    }
}

impl TurnStreamProvider for AndroidHostInferenceProvider {
    fn start_stream(
        &mut self,
        input: &StreamAttemptInput,
        _attempt: usize,
    ) -> Result<StreamGeneration, ProviderFailure> {
        let text = match self.mode {
            AndroidInferenceMode::Test => "自动化测试状态正常。".to_string(),
            AndroidInferenceMode::Production => {
                if input.prompt.trim().is_empty() {
                    return Err(ProviderFailure::new("prompt is required"));
                }
                // Until a network/provider transport is bound, keep one canonical Host-owned
                // behavior rather than a renderer fallback. The missing transport remains an
                // explicit parity item and is not represented as verified.
                "Fabushi Android Host accepted the message.".to_string()
            }
        };

        let split = text.len().min(text.char_indices().nth(1).map(|(i, _)| i).unwrap_or(text.len()));
        let chunks = if split > 0 && split < text.len() {
            vec![text[..split].to_string(), text[split..].to_string()]
        } else {
            vec![text]
        };

        Ok(StreamGeneration {
            first_token_delay_ms: 1,
            chunks,
            finish_reason: "stop".into(),
        })
    }

    fn cancel(&mut self, _operation_id: &str) -> Result<(), String> {
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_provider_streams_deterministically() {
        let mut provider = AndroidHostInferenceProvider::new(AndroidInferenceMode::Test);
        let output = provider
            .start_stream(
                &StreamAttemptInput {
                    operation_id: "op".into(),
                    agent_id: "agent".into(),
                    model: "default".into(),
                    prompt: "hello".into(),
                    resume_checkpoint_available: false,
                },
                1,
            )
            .unwrap();
        assert_eq!(output.chunks.concat(), "自动化测试状态正常。");
    }
}
