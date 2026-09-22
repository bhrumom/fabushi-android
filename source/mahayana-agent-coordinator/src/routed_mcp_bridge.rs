use std::collections::BTreeMap;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct RoutedMcpTool {
    pub name: String,
    pub provider: String,
    pub remote_name: String,
    pub read_only: bool,
}

#[derive(Default)]
pub struct RoutedMcpBridge {
    tools: BTreeMap<String, RoutedMcpTool>,
}

impl RoutedMcpBridge {
    pub fn replace_tools(&mut self, tools: impl IntoIterator<Item=RoutedMcpTool>) -> Result<(), &'static str> {
        let mut next=BTreeMap::new();
        for tool in tools {
            if tool.name.trim().is_empty() || tool.provider.trim().is_empty() || tool.remote_name.trim().is_empty() { return Err("MCP tool identity fields are required"); }
            if next.insert(tool.name.clone(), tool).is_some() { return Err("duplicate routed MCP tool name"); }
        }
        self.tools=next;
        Ok(())
    }
    pub fn tool(&self, name:&str) -> Option<&RoutedMcpTool> { self.tools.get(name) }
    pub fn list(&self) -> Vec<&RoutedMcpTool> { self.tools.values().collect() }
}
