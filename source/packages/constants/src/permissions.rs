use std::collections::BTreeSet;

pub const TEAM_PERMISSIONS: &[&str] = &[
    "team.spend.read", "team.member_spend.read", "team.privacy_mode.read", "team.sso.read",
    "team.rules.read", "team.rules.manage", "team.hooks.read", "team.hooks.manage",
    "team.commit_metrics.read", "team.directory_groups.manage", "team.protected_git_scopes.manage",
    "team.background_composers.read", "team.background_composers.list", "team.background_composers.manage",
    "team.full_self_driving.manage", "team.public_profile_settings.manage", "team.member_spend_limits.manage",
    "team.mcp_servers.manage", "team.scim_configuration.manage", "team.billing_groups.manage",
    "team.groups.read", "team.groups.manage", "team.members.read", "team.members.manage",
    "team.membership.read", "team.invites.read", "team.invites.manage", "team.api_keys.manage",
    "team.repos.read", "team.repos.manage", "team.settings.read", "team.settings.manage",
    "team.billing.read", "team.billing.manage", "team.audit_logs.read", "team.analytics.read",
    "team.analytics.manage", "team.privacy.manage", "team.commands.read", "team.commands.manage",
    "team.bugbot.read", "team.bugbot.manage", "team.sharing_settings.read", "team.sharing_settings.manage",
    "team.background_agent_settings.manage", "team.plugins.read", "team.plugins.manage",
    "team.integrations.read", "team.integrations.manage",
];

pub const ORGANIZATION_PERMISSIONS: &[&str] = &[
    "organization.members.read", "organization.members.write", "organization.memberships.manage",
    "organization.manage", "organization.teams.manage", "organization.groups.read",
    "organization.groups.manage", "organization.api_keys.manage",
    "organization.identity_providers.manage", "organization.spend.read",
    "organization.audit_logs.read", "organization.billing.read",
];

pub const AGENT_STORE_PERMISSIONS: &[&str] = &[
    "agent_store.read", "agent_store.write", "agent_store.share",
];
pub const AGENT_STORE_SHARE_PERMISSIONS: &[&str] = &["agent_store_share.read"];
pub const KEYRING_PERMISSIONS: &[&str] = &["keyring.manage", "keyring.attach"];
pub const ENVIRONMENT_PERMISSIONS: &[&str] = &[
    "environment.security.manage", "environment.workload.manage", "environment.use",
];

pub fn is_known_permission(permission: &str) -> bool {
    [
        TEAM_PERMISSIONS,
        ORGANIZATION_PERMISSIONS,
        AGENT_STORE_PERMISSIONS,
        AGENT_STORE_SHARE_PERMISSIONS,
        KEYRING_PERMISSIONS,
        ENVIRONMENT_PERMISSIONS,
    ]
    .into_iter()
    .any(|group| group.contains(&permission))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn permission_registry_has_no_duplicates_and_fails_closed() {
        let mut all = BTreeSet::new();
        for group in [
            TEAM_PERMISSIONS,
            ORGANIZATION_PERMISSIONS,
            AGENT_STORE_PERMISSIONS,
            AGENT_STORE_SHARE_PERMISSIONS,
            KEYRING_PERMISSIONS,
            ENVIRONMENT_PERMISSIONS,
        ] {
            for permission in group {
                assert!(all.insert(*permission), "duplicate permission: {permission}");
            }
        }
        assert!(is_known_permission("agent_store.read"));
        assert!(is_known_permission("environment.use"));
        assert!(!is_known_permission("environment.root"));
    }
}
