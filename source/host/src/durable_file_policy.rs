use std::collections::BTreeSet;

pub const SAND_UPGRADE_RESUME_FILE_NAME: &str = "upgrade-resume.json";
pub const SAND_ACK_OBLIGATIONS_FILE_NAME: &str = "ack-obligations.json";
pub const SAND_PENDING_WAKE_FILE_NAME: &str = "pending-wake.json";
pub const SAND_XUSER_TURN_DEDUPE_FILE_NAME: &str = "xuser-turn-dedupe.json";
pub const SAND_DISK_PRESSURE_REMINDERS_FILE_NAME: &str = "disk-pressure-reminders.json";

pub fn excluded_from_box_snapshot() -> BTreeSet<&'static str> {
    [
        SAND_UPGRADE_RESUME_FILE_NAME,
        SAND_ACK_OBLIGATIONS_FILE_NAME,
        SAND_PENDING_WAKE_FILE_NAME,
        SAND_XUSER_TURN_DEDUPE_FILE_NAME,
        SAND_DISK_PRESSURE_REMINDERS_FILE_NAME,
    ].into_iter().collect()
}
